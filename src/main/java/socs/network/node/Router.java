package socs.network.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import socs.network.util.Configuration;

public class Router {

	RouterDescription rd = new RouterDescription();
	protected LinkStateDatabase lsd;
	Link[] ports = new Link[4];

	public Router(Configuration config) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
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

			if (ports == null || ports.length == 0) {
				System.out.println("No Neighbour Connected.");
				return;
			}

			// right now, only one server connected
			Socket client = new Socket(ports[0].router2.getProcessIPAddress(), 9090);
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(client.getInputStream()));
			PrintWriter outToServer = new PrintWriter(client.getOutputStream(), true);
			
			outToServer.println(rd.getProcessIPAddress() + " " + rd.getProcessPortNumber() + " " + rd.getSimulatedIPAddress());

			outToServer.println("Hello, From " + rd.getSimulatedIPAddress() + "\nset " + rd.getSimulatedIPAddress()
					+ "state to INIT");

			System.out.println(inFromServer.readLine());
			ports[0].router2.setStatus(RouterStatus.TWO_WAY);

			outToServer.println("Hello, From " + rd.getSimulatedIPAddress() + "\nset " + rd.getSimulatedIPAddress()
					+ "state to TWO_WAY");

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
		System.out.println("Quitting client.");
		System.exit(1);
	}

	public void terminal(){
		try {
			
			ServerSocket serverSocket = new ServerSocket(9090);
			System.out.println("Router:[" + rd.getSimulatedIPAddress() + "] ready...");
			
			InputStreamReader isReader = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isReader);
			System.out.print(">> ");
			String command = br.readLine();
			
			while (true) {

				Socket socket = serverSocket.accept();
				new RouterThread(socket, rd, ports).start();

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

		} catch (IOException e) {
			System.out.println("Unable to read from standard in");
			System.exit(1);
		}

	}

}
