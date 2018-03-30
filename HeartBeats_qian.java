package socs.network.message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.TimerTask;

import socs.network.node.Link;
import socs.network.node.LinkStateDatabase;
import socs.network.node.RouterDescription;

public class HeartBeats extends TimerTask {
	
	RouterDescription rd = new RouterDescription();	
	LinkStateDatabase lsd;
	Link[] ports = new Link[4];
	
	public HeartBeats(Link[] ports){
		this.ports = ports;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		for(int i = 0; i < 4; i++){
			if(ports[i] != null){
				try {
					//
					Socket client = new Socket(ports[i].getRouter2().getProcessIPAddress(), ports[i].getRouter2().getProcessPortNumber());
					PrintWriter out = new PrintWriter(client.getOutputStream(),true);
					//BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
					
					out.write("Periodical Hello");
									
					System.out.println("heartbeats send");
					

					
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}					
			}
		}
		
	}

}
