package socs.network.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;

public class RouterThread extends Thread {

	Socket socket;
	RouterDescription rd;

	// assuming that all routers are with 4 ports
	Link[] ports = new Link[4];

	RouterThread() {
	}

	RouterThread(Socket socket, RouterDescription rd) throws IOException {
		this.socket = socket;
		this.rd = rd;
	}

	/**
	 * output the shortest path to the given destination ip
	 * <p/>
	 * format: source ip address -> ip address -> ... -> destination ip
	 *
	 * @param destinationIP
	 *            the ip adderss of the destination simulated router
	 */
	private void processDetect(String destinationIP) {

	}

	/**
	 * disconnect with the router identified by the given destination ip address
	 * Notice: this command should trigger the synchronization of database
	 *
	 * @param portNumber
	 *            the port number which the link attaches at
	 */
	private void processDisconnect(short portNumber) {

		for (Link link : ports) {
			if (link != null && link.router2.getProcessPortNumber() == portNumber) {
				link = null;
			}
		}
	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to
	 * indentify the process IP and process Port; additionally, weight is the
	 * cost to transmitting data through the link
	 * <p/>
	 * NOTE: this command should not trigger link database synchronization
	 */
	private void processAttach(String processIP, short processPort, String simulatedIP, short weight) {
		
		String msg = "Connection cannot be established.";
		
		for (Link link : ports) {
			if (link == null) {
				
				try {
					Socket client = new Socket(ports[0].router2.getProcessIPAddress(), 9090);
					BufferedReader inFromServer = new BufferedReader(new InputStreamReader(client.getInputStream()));
					PrintWriter outToServer = new PrintWriter(client.getOutputStream(), true); 

					outToServer.println(rd.getProcessIPAddress() + "," + rd.getSimulatedIPAddress() + ","+rd.getProcessPortNumber());
					
					if( !inFromServer.readLine().equals("true")){
						System.out.println(msg);
						return;		
					}					
					
				} catch (IOException e) {
					e.printStackTrace();
				}				
				
				RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP);
				link = new Link(rd, r2);
				msg = "Connection established.";
				
				
				break;
			}
		}

		System.out.println(msg);
	}

	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {

		try {	
			
			if(ports == null || ports.length == 0){
				System.out.println("No Neighbour connected.");
				return;			
			}
							
			//right now, only one server connected
			Socket client = new Socket(ports[0].router2.getProcessIPAddress(), 9090);
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(client.getInputStream()));
			PrintWriter outToServer = new PrintWriter(client.getOutputStream(), true); 

			outToServer.println("Hello From " + rd.getSimulatedIPAddress() + "\nset " + rd.getSimulatedIPAddress() + "state to INIT");

			System.out.println(inFromServer.readLine());
			ports[0].router2.setStatus(RouterStatus.TWO_WAY);
			
			outToServer.println("Hello From " + rd.getSimulatedIPAddress() + "\nset " + rd.getSimulatedIPAddress() + "state to TWO_WAY");
			
			client.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to
	 * indentify the process IP and process Port; additionally, weight is the
	 * cost to transmitting data through the link
	 * <p/>
	 * This command does trigger the link database synchronization
	 */
	private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {

	}

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {
		System.out.println("IP Address of the neighbor1: " + ports[0].router2.getSimulatedIPAddress());
	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {
		//TODO:
		System.out.println("Quitting client.");
		System.exit(1);
	}

	public void run() {
		try {
			//as server, accept request from clients
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);
			
			InputStreamReader isReader = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isReader);
			System.out.print(">> ");
			String command = br.readLine();
			
			while (true) {//works forever
				
				if(inFromClient.readLine() != null){
					if(inFromClient.readLine().startsWith("Hello"))
					
					System.out.print(inFromClient.readLine());
					
					//right now only one client
					if (ports[0].router2.getStatus() == null) {
						ports[0].router2.setStatus(RouterStatus.INIT);
						outToClient.write("Hello From " + rd.getSimulatedIPAddress() + "\nset " + rd.getSimulatedIPAddress() + "state to TWO_WAY");
					} else {
						ports[0].router2.setStatus(RouterStatus.TWO_WAY);
					}
				}
				
				//as client, no command typed in
				if(command == null) 
					continue;
				
				//as client, with command
				if (command.startsWith("detect ")) {
					String[] cmdLine = command.split(" ");
					processDetect(cmdLine[1]);
				} else if (command.startsWith("disconnect ")) {
					String[] cmdLine = command.split(" ");
					processDisconnect(Short.parseShort(cmdLine[1]));
				} else if (command.startsWith("quit")) {
					processQuit();
				} else if (command.startsWith("attach ")) {
					String[] cmdLine = command.split(" ");
					processAttach(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("start")) {
					processStart();
				} else if (command.equals("connect ")) {
					String[] cmdLine = command.split(" ");
					processConnect(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("neighbors")) {
					// output neighbors
					processNeighbors();
				} else {
					// invalid command
					System.out.println("The interface does not support this command.");
					break;
				}
				System.out.print(">> ");
				command = br.readLine();
			}
			isReader.close();
			br.close();
			socket.close();
		} catch (IOException e) {
			System.out.println("Unable to read from standard in");
            System.exit(1);
		}
	}
}
