package socs.network.node;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import socs.network.util.Configuration;

public class Router {

	static RouterDescription rd = new RouterDescription();
	protected LinkStateDatabase lsd;
	static Link[] ports = new Link[4];

	public Router(Configuration config) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processPortNumber = config.getShort("socs.network.router.port");
		lsd = new LinkStateDatabase(rd);
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

		for (int i = 0; i < 4; i++) {
			if (ports[i] != null && ports[i].router2.getProcessPortNumber() == portNumber) {
				ports[i] = null;
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
		
		for (int i = 0; i < 4; i++) {
			if(ports[i] == null){
				RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP);
				ports[i] = new Link(rd, r2);
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
			boolean hasNeighbour = false;
			
			for (int i = 0; i < 4; i++) {
				if(ports[i] != null){
					hasNeighbour = true;
					
					Socket client = new Socket(ports[i].router2.getProcessIPAddress(), ports[i].router2.getProcessPortNumber());
					
					DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
					DataInputStream inFromServer = new DataInputStream(client.getInputStream());
					
					outToServer.writeUTF(
							ports[i].router2.getProcessIPAddress() + " " + rd.getProcessPortNumber() + " " + rd.getSimulatedIPAddress());					
					
					if(inFromServer.readUTF().equals("false")){
						System.out.println("No Spot Avavilable in Router: " + ports[i].router2.getSimulatedIPAddress());
						continue;
					}
					
					outToServer.writeUTF("Hello From " + rd.getSimulatedIPAddress() + " \nSet " + rd.getSimulatedIPAddress()
							+ " state to INIT");					

					System.out.println(inFromServer.readUTF());
					ports[i].router2.setStatus(RouterStatus.TWO_WAY);

					outToServer.writeUTF("Hello From " + rd.getSimulatedIPAddress() + " \nSet " + rd.getSimulatedIPAddress()
						+ " state to TWO_WAY");					

					if(inFromServer.readUTF().equals("Done"))			
						client.close();							
				}
			}
			
			if(!hasNeighbour){
				System.out.println("No Neighbour Connected.");
			}
			
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
		System.out.println("Quitting client.");
		System.exit(1);
	}

	public void terminal() {
		try {
						
			ServerSocket serverSocket = new ServerSocket(rd.getProcessPortNumber());	
			System.out.println("Router:[" + rd.getSimulatedIPAddress() + "] ready...");
							
			startClient();
			
			while (true) {
				Socket socket = serverSocket.accept();
				new RouterThread(socket, rd, ports).start();
			}
			
		} catch (IOException e) {
			System.out.println("Unable to read from standard in");
			System.exit(1);
		}

	}

	public void startClient() {
        (new Thread() {
            @Override
            public void run() {
                try {
                	
                	BufferedReader br = new java.io.BufferedReader(new InputStreamReader(System.in));
        			
                	while (true) {
                		System.out.print(">> ");
        				String command = br.readLine();
        				
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
					}
					br.close();
					//isReader.close();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

}
