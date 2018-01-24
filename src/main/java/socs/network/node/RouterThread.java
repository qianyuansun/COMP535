package socs.network.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import socs.network.util.Configuration;

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

			Socket socket = new Socket(hostName, serverPort);
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter outToServer = new PrintWriter(socket.getOutputStream(), true); // open
																						// an
																						// output
																						// stream
																						// to
																						// the
																						// server...

			outToServer.println("Hello From " + clientPort);

			System.out.println(inFromServer.readLine());
			// two
			outToServer.println("Hello From " + clientPort);

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
		System.out.println(ports[0].router2.getSimulatedIPAddress());
	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {

	}

	public void run() {
		try {
			
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);
			
			InputStreamReader isReader = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isReader);
			System.out.print(">> ");
			String command = br.readLine();
			while (true) {
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
					break;
				}
				System.out.print(">> ");
				command = br.readLine();
			}
			isReader.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
