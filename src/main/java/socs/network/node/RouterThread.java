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
			// as server, accept request from clients
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);
			String message = null;

			while ((message = inFromClient.readLine()) != null) {
				//hello
				if (message.startsWith("Hello ")) {
					
					System.out.println(message);
					
					/*
					String[] arguments = message.split(" ");
					String simulatedIP = arguments[2];
					
					for(for (int i = 0; i < 4; i++) {){
						if(link.router2.getSimulatedIPAddress().equals(simulatedIP)){
						}
					} */

					if (ports[0].router2.getStatus() == null) {
						ports[0].router2.setStatus(RouterStatus.INIT);
						outToClient.write("Hello From " + rd.getSimulatedIPAddress() + "\nset "
								+ rd.getSimulatedIPAddress() + " state to TWO_WAY");
					} else {
						ports[0].router2.setStatus(RouterStatus.TWO_WAY);
					}
				//r2 router description
				}else{
					String[] arguments = message.split(" ");

					String processIP = arguments[0];
					Short processPort = Short.parseShort(arguments[1]);
					String simulatedIP = arguments[2];

					// add port in server
					for (int i = 0; i < 4; i++) {
						if (ports[i] == null) {
							RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP);
							ports[i] = new Link(rd, r2);
							outToClient.write("true");
							break;
						}
					}
				}
			}
			
			socket.close();

		} catch (IOException e) {
			System.out.println("Unable to read from standard in");
			System.exit(1);
		}
	}
}
