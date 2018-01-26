package socs.network.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RouterThread extends Thread {

	Socket socket;
	RouterDescription rd;

	// assuming that all routers are with 4 ports
	Link[] ports;

	RouterThread() {
	}

	RouterThread(Socket socket, RouterDescription rd, Link[] ports) throws IOException {
		this.socket = socket;
		this.rd = rd;
		this.ports = ports;
	}

	
	public void run() {
		try {
			//as server, accept request from clients
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);			
			String message = null;
			
			while ((message = inFromClient.readLine()) != null) {
				
				String[] arguments = message.split(" ");
				String header = arguments[0];
				
				String processIP = arguments[0];
				Short processPort =Short.parseShort(arguments[1]);
				String simulatedIP = arguments[2];

				//add port in server
				for (Link link : ports) {
					if (link == null) {
						RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP);
						link = new Link(rd, r2);	
						break;
					}
				}

				System.out.println(message);
				if (ports[0].router2.getStatus() == null) {
					ports[0].router2.setStatus(RouterStatus.INIT);
					outToClient.write("Hello, From " + rd.getSimulatedIPAddress() + "\nset " + rd.getSimulatedIPAddress() + "state to TWO_WAY");
				} else {
					ports[0].router2.setStatus(RouterStatus.TWO_WAY);
				}
				
			}			
			socket.close();
			
		} catch (IOException e) {
			System.out.println("Unable to read from standard in");
            System.exit(1);
		}
	}
}
