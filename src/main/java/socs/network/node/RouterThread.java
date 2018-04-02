package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;

public class RouterThread extends Thread {

	Socket socket;
	RouterDescription rd;
	Socket[] sockets;
	ObjectOutputStream[] oosList;
	String clientIP;
	short clientPort;
	HeartBeats heartbeats;

	// assuming that all routers are with 4 ports
	Link[] ports;
	
	LinkStateDatabase lsd;

	RouterThread() {
	}

	RouterThread(Socket socket, RouterDescription rd, Link[] ports, LinkStateDatabase lsd, Socket[] sockets, ObjectOutputStream[] oosList, HeartBeats heartbeats) throws IOException {
		this.socket = socket;
		this.rd = rd;
		this.ports = ports;
		this.lsd = lsd;
		this.sockets = sockets;
		this.oosList = oosList;
		this.heartbeats = heartbeats;
		
		//socket.setSoTimeout(30000);
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
				} else if (pack.sospfType == 3) {
					clientIP = pack.srcIP;
					clientPort = pack.srcProcessPort;
					System.out.println("Router: " + pack.srcIP + " alive.");					
				} else {
					// System.out.println("Receive Updated LSD from: " + pack.srcIP + " : " + pack.lsaArray.toString());
					updateLSA(pack);
					this.sendNewPack(pack);
				}
			}				
		} catch (SocketTimeoutException s) {
			// update lsa	
			if (clientIP != null) {
				System.out.println(clientIP + " socket timed out.");
				
				this.deleteNeighbor(clientPort);
				try {
					this.sendNewPack(null);
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			if (clientIP != null) {
				//Thread.sleep(1000);
				System.out.println(clientIP + " socket interrupted.");
				this.deleteNeighbor(clientPort);
				try {
					this.sendNewPack(null);
					//socket.close();
				} catch (IOException ioe) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
			}
			//Thread.currentThread().interrupt();
			//return;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		clientIP = simulatedIP;
		clientPort = processPort;

		for (int i = 0; i < 4; i++) {
			if (ports[i] == null) {
				RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP);
				r2.setStatus(RouterStatus.INIT);
				ports[i] = new Link(rd, r2, weight);
				Socket client2 = new Socket(r2.getProcessIPAddress(), r2.getProcessPortNumber());
				sockets[i] = client2;
				oosList[i] = new ObjectOutputStream(client2.getOutputStream());
				heartbeats.setAll(ports, sockets, oosList);
				break;
			}
		}
	}
	
	public void updateLSA(SOSPFPacket pack) {
		Vector<LSA> lsaArray = pack.lsaArray;
		Vector<String> lsaArrayKeys = this.generateKeySet(lsaArray);
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
		//removed deleted lsa for other routers
		/*
		Iterator<Entry<String, LSA>> iter = lsd._store.entrySet().iterator();
		while (iter.hasNext()) {
		    Entry<String, LSA> entry = iter.next();
		    if(!lsaArrayKeys.contains(entry.getKey())){
		        iter.remove();
		    }
		} */
		
	}
	
	public void deleteNeighbor(short portNumber){
		for (int i = 0; i < 4; i++) {
			if (ports[i] != null && ports[i].router2.getProcessPortNumber() == portNumber) {
				String simulatedIP = ports[i].router2.getSimulatedIPAddress();
				ports[i] = null;
				sockets[i] = null;
				oosList[i] = null;
				heartbeats.setAll(ports, sockets, oosList);
				this.lsd.deleteLink(rd.getSimulatedIPAddress(), simulatedIP, portNumber);
				
				System.out.println("Neighbor: " + simulatedIP + " has been disconnected.");
				break;
			}
		}
	}
	
	private Vector<String> generateKeySet(Vector<LSA> lsaArray){
		Vector<String> keySet = new Vector<String>();
		for(LSA lsa : lsaArray){
			keySet.add(lsa.linkStateID);
		}
		return keySet;
		
	}
}
