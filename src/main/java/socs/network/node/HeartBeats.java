package socs.network.node;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.TimerTask;

import socs.network.message.SOSPFPacket;

public class HeartBeats extends TimerTask {
	
	//RouterDescription rd = new RouterDescription();	
	String srcIP;
	//LinkStateDatabase lsd;
	Link[] ports;
	Socket[] sockets;
	ObjectOutputStream[] oosList;
	
	public HeartBeats(Link[] ports, Socket[] sockets, ObjectOutputStream[] oosList){
		this.ports = ports;
		this.sockets = sockets;
		this.oosList = oosList;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		SOSPFPacket pack = new SOSPFPacket();
		pack.srcIP = srcIP;
		pack.sospfType = 3;
		
		for (int i = 0; i < 4; i++) {
			if(ports[i] != null){			
				synchronized (sockets[i]) {
					try {
						oosList[i].writeObject(pack);
						System.out.println("Heartbeats sent");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						System.out.println("Cannot reach " + ports[i].router2.getSimulatedIPAddress());
					}
			    }
			}	
		}		
	}

	public String getSrcIP() {
		return srcIP;
	}

	public void setSrcIP(String srcIP) {
		this.srcIP = srcIP;
	}
	
	public void setAll(Link[] ports, Socket[] sockets, ObjectOutputStream[] oosList){
		this.ports = ports;
		this.sockets = sockets;
		this.oosList = oosList;
	}
	
	

}
