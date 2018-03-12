package socs.network.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

public class Router {

	static RouterDescription rd = new RouterDescription();
	protected LinkStateDatabase lsd;
	static Link[] ports = new Link[4];
	List<Socket> socketList = new ArrayList<Socket>();
	Map<String, ObjectOutputStream> oosMap = new HashMap<String, ObjectOutputStream>();

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
		System.out.println(this.lsd.getShortestPath(destinationIP));
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
	 * @throws IOException 
	 */
	private void processAttach(String processIP, short processPort, String simulatedIP, short weight) throws IOException {

		String msg = "Connection cannot be established.";
		
		for (int i = 0; i < 4; i++) {
			if(ports[i] == null){
				RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP);
				ports[i] = new Link(rd, r2,weight);
				Socket client2 = new Socket(r2.getProcessIPAddress(), r2.getProcessPortNumber());
				socketList.add(client2);
				oosMap.put(r2.getSimulatedIPAddress(), new ObjectOutputStream(client2.getOutputStream()));
				msg = "Connection established.";
				break;
			}
		}

		System.out.println(msg);
	}

	/**
	 * broadcast Hello to neighbors
	 */
	private boolean processStart() {

		boolean hasNeighbour = false;
		boolean isUpdated = false;
		try {	
			for (int i = 0; i < 4; i++) {
				if(ports[i] != null){
					hasNeighbour = true;
					Socket client = new Socket(ports[i].router2.getProcessIPAddress(), ports[i].router2.getProcessPortNumber());										
					ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
					ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
					
					SOSPFPacket pack = new SOSPFPacket(); 
					pack.srcProcessIP = ports[i].router2.getProcessIPAddress();
					pack.srcProcessPort = rd.getProcessPortNumber();
					pack.srcIP = rd.getSimulatedIPAddress();
					pack.weight = ports[i].weight;
					pack.sospfType = 0;
					oos.writeObject(pack);
										
					SOSPFPacket backPack = (SOSPFPacket) ois.readObject();
					
					if(backPack.sospfType == 2){
						System.out.println("Already Started.");
						//client.close();
						continue;
					}
					
					System.out.println("Hello From " + ports[i].router2.getSimulatedIPAddress() + " \nSet " + ports[i].router2.getSimulatedIPAddress() + " state to TWO_WAY");
					ports[i].router2.setStatus(RouterStatus.TWO_WAY);

					oos.writeObject(pack);							
					
					//1.creates linkDescription for this new link
					//2. adds this new link to the LSA
					LinkDescription newLinkDes = new LinkDescription(ports[i].router2.getSimulatedIPAddress(), ports[i].router2.getProcessPortNumber(), ports[i].weight);
					this.lsd.addLink(rd.getSimulatedIPAddress(), newLinkDes);
					isUpdated = true;
					
					//client.close();	
				}
				
			}						
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(!hasNeighbour){ 
			System.out.println("No Neighbour Connected.");
		}
		return isUpdated;
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
		for (int i = 0; i < 4; i++) {
			if (ports[i] != null) {
				System.out.println("IP Address of the neighbor[" + i + "] : " + ports[i].router2.getSimulatedIPAddress());
			}
		}
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
				new RouterThread(socket, rd, ports, lsd, socketList, oosMap).start();
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
        					boolean isUpdated = processStart();
        					if(isUpdated){
        						sendNewPack();
        					}
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
	
	public void sendNewPack() throws IOException {

		SOSPFPacket newPack = new SOSPFPacket();
		newPack.srcIpList = new ArrayList<String>();
		newPack.srcIpList.add(rd.getSimulatedIPAddress());
		newPack.srcIP = rd.getSimulatedIPAddress();
		newPack.sospfType = 1;
		newPack.lsaArray = new Vector<LSA>();
		for (Entry<String, LSA> lsa : lsd._store.entrySet()) {
			LSA newLsa = lsa.getValue().copy();
			newPack.lsaArray.addElement(newLsa);
		}

		for (Entry<String, ObjectOutputStream> e : oosMap.entrySet()) {
			e.getValue().writeObject(newPack);
			System.out.println(newPack.lsaArray.toString());
			System.out.println("Send pack out");
		}
	}
}
