package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Vector;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;

public class RouterThread extends Thread {

	Socket socket;
	RouterDescription rd;
	Socket[] sockets;
	ObjectOutputStream[] oosList;

	// assuming that all routers are with 4 ports
	Link[] ports;
	
	LinkStateDatabase lsd;

	RouterThread() {
	}

	RouterThread(Socket socket, RouterDescription rd, Link[] ports, LinkStateDatabase lsd, Socket[] sockets, ObjectOutputStream[] oosList) throws IOException {
		this.socket = socket;
		this.rd = rd;
		this.ports = ports;
		this.lsd = lsd;
		this.sockets = sockets;
		this.oosList = oosList;
		
		//time out in routerthread
		ServerSocket server = new ServerSocket();
		server.setSoTimeout(30000);
	}

	public void run() {
		try {
			ObjectOutputStream oos  = new ObjectOutputStream( socket.getOutputStream());
			oos.flush();
			ObjectInputStream ois  = new ObjectInputStream( socket.getInputStream());
			
			SOSPFPacket pack = null;
			while ((pack = (SOSPFPacket) ois.readObject()) != null) {
				
				if (pack.sospfType == 0) {					
					boolean isUpdated = communicate(oos, pack);	
					if(isUpdated){
						this.sendNewPack(null);
					}
				}else {
					//System.out.println("Receive Updated LSD from: " + pack.srcIP + " : " + pack.lsaArray.toString());
					updateLSA(pack);
					this.sendNewPack(pack);
				}
			}		
			socket.close();
			
		} catch (IOException | ClassNotFoundException e) {
			//System.out.println("Unable to read from standard in");
			//System.exit(1);
			Thread.currentThread().interrupt();
		    return;
		} catch (SocketTimeoutException s) {
	        System.out.println("Socket timed out!");
	        //update lsa
	        break;
	      }
		
	}
	
	public boolean communicate(ObjectOutputStream outToClient, SOSPFPacket pack) throws IOException{
		
		boolean isUpdated = false;
		String simulatedIP = pack.srcIP;

		boolean found = false;
		for (int i = 0; i < 4; i++) {
			if(ports[i] != null && ports[i].router2.getSimulatedIPAddress().equals(simulatedIP)){
				found = true;
				
				if(ports[i].router2.getStatus().equals(RouterStatus.TWO_WAY)){					
					SOSPFPacket newPack = new SOSPFPacket(); 
					newPack.sospfType = 2;
					outToClient.writeObject(newPack);
					return isUpdated;					
				}
				
				System.out.println("Hello From " + pack.srcIP + " \nSet " + pack.srcIP + " state to TWO_WAY");
				ports[i].router2.setStatus(RouterStatus.TWO_WAY);

				//1.creates linkDescription for this new link
				//2. adds this new link to the LSA				
				LinkDescription newLinkDes = new LinkDescription(ports[i].router2.getSimulatedIPAddress(), ports[i].router2.getProcessPortNumber(), ports[i].weight);				
				lsd.addLink(rd.getSimulatedIPAddress(), newLinkDes);
				isUpdated = true;
				break;
			}
			
		}
		
		if(!found){
			this.connect(pack);
			SOSPFPacket newPack = new SOSPFPacket();
			newPack.sospfType = 0;
			newPack.srcIP = rd.getSimulatedIPAddress();
			outToClient.writeObject(newPack);
		}		
		return isUpdated;		
	}
	
	public void sendNewPack(SOSPFPacket pack) throws IOException{
		
		SOSPFPacket newPack = new SOSPFPacket();
		if (pack == null) {
			newPack.srcIpList = new ArrayList<String>();
		} else {
			newPack.srcIpList = pack.srcIpList;
		}
		newPack.srcIpList.add(rd.getSimulatedIPAddress());
		newPack.srcIP = rd.getSimulatedIPAddress();
		newPack.sospfType = 1;
		newPack.lsaArray = new Vector<LSA>();
		for(Entry<String, LSA> lsa : lsd._store.entrySet()){
			newPack.lsaArray.addElement(lsa.getValue().copy());	
		}
		
		for (int i = 0; i < 4; i++) {
			if(ports[i] != null){
				if(newPack.srcIpList.contains(ports[i].router2.getSimulatedIPAddress())){
					continue;
				}				
				synchronized (sockets[i]) {
					//System.out.println("as server Send pack to" + ports[i].router2.getSimulatedIPAddress());	
					oosList[i].writeObject(newPack);
			    }
			}
		}
	}
	
	public void connect(SOSPFPacket pack) throws IOException {

		System.out.println("Hello From " + pack.srcIP + " \nSet " + pack.srcIP + " state to INIT");

		String processIP = pack.srcProcessIP;
		Short processPort = pack.srcProcessPort;
		String simulatedIP = pack.srcIP;
		Short weight = pack.weight;

		for (int i = 0; i < 4; i++) {
			if (ports[i] == null) {
				RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP);
				r2.setStatus(RouterStatus.INIT);
				ports[i] = new Link(rd, r2, weight);
				Socket client2 = new Socket(r2.getProcessIPAddress(), r2.getProcessPortNumber());
				sockets[i] = client2;
				oosList[i] = new ObjectOutputStream(client2.getOutputStream());
				break;
			}
		}
	}
	
	public void updateLSA(SOSPFPacket pack) throws IOException {
		Vector<LSA> lsaArray = pack.lsaArray;
		for (LSA l : lsaArray) {
			if (lsd._store.containsKey(l.linkStateID)) {
				if (lsd._store.get(l.linkStateID).lsaSeqNumber < l.lsaSeqNumber) {
					//System.out.println("Updated");
					lsd._store.put(l.linkStateID, l);
				}
			} else {
				//System.out.println("Add new LSA");
				lsd._store.put(l.linkStateID, l);
			}
		}
		//System.out.println(lsd._store.toString());
	}
}
