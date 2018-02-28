package socs.network.node;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;
import java.util.Map.Entry;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;

public class RouterThread extends Thread {

	Socket socket;
	RouterDescription rd;

	// assuming that all routers are with 4 ports
	Link[] ports;
	
	LinkStateDatabase lsd;

	RouterThread() {
	}

	RouterThread(Socket socket, RouterDescription rd, Link[] ports, LinkStateDatabase lsd) throws IOException {
		this.socket = socket;
		this.rd = rd;
		this.ports = ports;
		this.lsd = lsd;
	}

	public void run() {
		try {
			//DataInputStream inFromClient = new DataInputStream(socket.getInputStream());
			//DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());	
			
			ObjectInputStream ois  = new ObjectInputStream( socket.getInputStream());
			ObjectOutputStream oos  = new ObjectOutputStream( socket.getOutputStream());
			
			// as server, accept request from clients
			SOSPFPacket pack = null;
			while ((pack = (SOSPFPacket) ois.readObject()) != null) {
				//hello
				if (pack.sospfType == 0) {					
					communicate(oos, pack);					
				//add r2 routerDescription
				}else{
					updateLSA(pack);	
					//TODO: continue forward?
				}
			}			
			socket.close();
			
		} catch (IOException | ClassNotFoundException e) {
			//System.out.println("Unable to read from standard in");
			//System.exit(1);
			Thread.currentThread().interrupt();
		    return;
		}
	}
	
	public void communicate(ObjectOutputStream outToClient, SOSPFPacket pack) throws IOException{
		
		//System.out.println(message);
		//String[] arguments = message.split(" ");
		String simulatedIP = pack.srcIP;

		boolean found = false;
		for (int i = 0; i < 4; i++) {
			if(ports[i] != null && ports[i].router2.getSimulatedIPAddress().equals(simulatedIP)){
				found = true;
				
				//if (ports[i].router2.getStatus() == null) {
					
					//ports[i].router2.setStatus(RouterStatus.INIT);
					//outToClient.writeUTF("Hello From " + rd.getSimulatedIPAddress() + " \nSet " + rd.getSimulatedIPAddress() + " state to TWO_WAY");
					
				//} else 
					//if(ports[i].router2.getStatus() == RouterStatus.INIT) {
				System.out.println("Hello From " + pack.srcIP + " \nSet " + pack.srcIP + " state to TWO_WAY");
				ports[i].router2.setStatus(RouterStatus.TWO_WAY);
				//outToClient.writeUTF("Done");
					
				//}
				//else {
					//outToClient.writeUTF("Already Started.");
					
				//}
				//1.creates linkDescription for this new link
				//2. adds this new link to the LSA				
				LinkDescription newLinkDes = new LinkDescription(ports[i].router2.getSimulatedIPAddress(), ports[i].router2.getProcessPortNumber(), ports[i].weight);				
				lsd.addLink(rd.getSimulatedIPAddress(), newLinkDes);						
				break;
			}
			
		}
		if(!found){
			this.connect(pack);
			SOSPFPacket newPack = new SOSPFPacket();
			newPack.sospfType = 0;
			newPack.srcIP = rd.getSimulatedIPAddress();
			outToClient.writeObject(newPack);
			return;
		}
		//TODO: 3. shares the LSP with all neighbors
		SOSPFPacket newPack = new SOSPFPacket(); 
		newPack.sospfType = 1;
		newPack.lsaArray = new Vector<LSA>();
		for(Entry<String, LSA> lsa : lsd._store.entrySet()){
			newPack.lsaArray.addElement(lsa.getValue());				
		}
		
		for (int i = 0; i < 4; i++) {
			if (ports[i] != null) {
				Socket client2 = new Socket(ports[i].router2.getProcessIPAddress(),
						ports[i].router2.getProcessPortNumber());
				ObjectOutputStream oos = new ObjectOutputStream(client2.getOutputStream());
				oos.writeObject(newPack);
			}
		}
	}
	
	public void connect(SOSPFPacket pack){
		// String[] arguments = message.split(" ");
		System.out.println("Hello From " + pack.srcIP + " \nSet " + pack.srcIP + " state to INIT");

		String processIP = pack.srcProcessIP;
		Short processPort = pack.srcProcessPort;
		String simulatedIP = pack.srcIP;
		Short weight = pack.weight;

		// add port in server
		//String hasSpot = "false";
		for (int i = 0; i < 4; i++) {
			if (ports[i] == null) {
				//hasSpot = "true";
				RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP);
				r2.setStatus(RouterStatus.INIT);
				ports[i] = new Link(rd, r2, weight);
				break;
			}
		}

		//outToClient.writeUTF(hasSpot);

	}
	
	public void updateLSA(SOSPFPacket pack){
		Vector<LSA> lsaArray = pack.lsaArray;
		for (LSA l: lsaArray){
			if(lsd._store.containsKey(l.linkStateID)){
				if(lsd._store.get(l.linkStateID).lsaSeqNumber < l.lsaSeqNumber){
					lsd._store.put(l.linkStateID, l);
				}
			}
			else{
				lsd._store.put(l.linkStateID, l);
			}
	}
}}
