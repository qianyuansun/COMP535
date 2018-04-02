package socs.network.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.Vector;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

public class Router {

	static RouterDescription rd = new RouterDescription();
	protected LinkStateDatabase lsd;
	static Link[] ports = new Link[4];
	Socket[] sockets = new Socket[4];
	ObjectOutputStream[] oosList = new ObjectOutputStream[4];
	HeartBeats heartbeats = new HeartBeats(ports, sockets, oosList);

	public Router(Configuration config) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processPortNumber = config.getShort("socs.network.router.port");
		lsd = new LinkStateDatabase(rd);		
		heartbeats.setSrcIP(rd.simulatedIPAddress);
		
		Timer timer = new Timer();
		timer.schedule(heartbeats, 1000, 10000);
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
	 * @throws IOException 
	 */
	private boolean processDisconnect(short portNumber){
		
		boolean isUpdated = false;

		for (int i = 0; i < 4; i++) {
			if (ports[i] != null && ports[i].router2.getProcessPortNumber() == portNumber) {
				String simulatedIP = ports[i].router2.getSimulatedIPAddress();
				ports[i] = null;
				sockets[i] = null;
				oosList[i] = null;
				heartbeats.setAll(ports, sockets, oosList);
				this.lsd.deleteLink(rd.getSimulatedIPAddress(), simulatedIP, portNumber);
				
				System.out.println(simulatedIP + " has been disconnected.");
				isUpdated = true;
				break;
			}
		}
		
		if(!isUpdated){
			System.out.println("No Such Port.");
		}
		return isUpdated;
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
				//Socket client2 = new Socket(r2.getProcessIPAddress(), r2.getProcessPortNumber());
				//sockets[i] = client2;
				//oosList[i] = new ObjectOutputStream(client2.getOutputStream());
				msg = "Connection established.";
				break;
			}
		}

		System.out.println(msg);
	}

	/**
	 * broadcast Hello to neighbors
	 */
	private boolean processStart(String simulatedIP) {

		boolean hasNeighbour = false;
		boolean isUpdated = false;
		
		if (simulatedIP != null) {
			for (int i = 0; i < 4; i++) {
				if (ports[i] != null && ports[i].router2.getSimulatedIPAddress().equals(simulatedIP)) {
					hasNeighbour = true;
					isUpdated = this.sendHello(i, isUpdated);
					break;
				}
			}
			this.heartbeats.setAll(ports, sockets, oosList);
			return isUpdated;
		}

		for (int i = 0; i < 4; i++) {
			if (ports[i] != null) {
				hasNeighbour = true;
				isUpdated = this.sendHello(i, isUpdated);
			}
		}

		if(!hasNeighbour){ 
			System.out.println("No Neighbour Connected.");
		}
		
		this.heartbeats.setAll(ports, sockets, oosList);
		return isUpdated;
	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to
	 * indentify the process IP and process Port; additionally, weight is the
	 * cost to transmitting data through the link
	 * <p/>
	 * This command does trigger the link database synchronization
	 * @throws IOException 
	 */
	private void processConnect(String processIP, short processPort, String simulatedIP, short weight) throws IOException {
		this.processAttach(processIP, processPort, simulatedIP, weight);
		boolean isUpdated = processStart(simulatedIP);
		if(isUpdated){
			sendNewPack();
		}
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
		for (int i = 0; i < 4; i++) {
			if (ports[i] != null) {
				this.processDisconnect(ports[i].router2.getProcessPortNumber());
			}
		}
		
		this.lsd._store.remove(rd.getSimulatedIPAddress());
		
		
		
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
				new RouterThread(socket, rd, ports, lsd, sockets, oosList, heartbeats).start();
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
        					boolean isUpdated = processDisconnect(Short.parseShort(cmdLine[1]));
        					if(isUpdated){
        						sendNewPack();
        					}
        				} else if (command.equals("quit")) {
        					processQuit();
        				} else if (command.startsWith("attach ")) {
        					String[] cmdLine = command.split(" ");
        					processAttach(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
        				} else if (command.equals("start")) {
        					boolean isUpdated = processStart(null);
        					if(isUpdated){
        						sendNewPack();
        					}
        				} else if (command.startsWith("connect ")) {
        					String[] cmdLine = command.split(" ");
        					processConnect(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
        				} else if (command.equals("neighbors")) {
        					// output neighbors
        					processNeighbors();
        				} else {
        					// invalid command
        					System.out.println("The interface does not support this command.");
        					//break;
        				}
					}
					//br.close();
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
			newPack.lsaArray.addElement(lsa.getValue().copy());
		}
		
		for (int i = 0; i < 4; i++) {
			if(ports[i] != null){
				synchronized (sockets[i]) {
					//System.out.println("as client Send pack to" + ports[i].router2.getSimulatedIPAddress());	
					oosList[i].writeObject(newPack);
			    }
			}
		}
	}
	
	public boolean sendHello(int i, boolean isUpdated){
		try {
			Socket client = new Socket(ports[i].router2.getProcessIPAddress(), ports[i].router2.getProcessPortNumber());
			sockets[i] = client;
			oosList[i] = new ObjectOutputStream(client.getOutputStream());
			//ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
			ObjectInputStream ois = new ObjectInputStream(client.getInputStream());

			SOSPFPacket pack = new SOSPFPacket();
			pack.srcProcessIP = ports[i].router2.getProcessIPAddress();
			pack.srcProcessPort = rd.getProcessPortNumber();
			pack.srcIP = rd.getSimulatedIPAddress();
			pack.weight = ports[i].weight;
			pack.sospfType = 0;
			oosList[i].writeObject(pack);

			SOSPFPacket backPack = (SOSPFPacket) ois.readObject();

			if (backPack.sospfType == 2) {
				System.out.println("Already Started.");
				// client.close();
				return isUpdated;
			}

			System.out.println("Hello From " + ports[i].router2.getSimulatedIPAddress() + " \nSet "
					+ ports[i].router2.getSimulatedIPAddress() + " state to TWO_WAY");
			ports[i].router2.setStatus(RouterStatus.TWO_WAY);

			oosList[i].writeObject(pack);

			// 1.creates linkDescription for this new link
			// 2. adds this new link to the LSA
			LinkDescription newLinkDes = new LinkDescription(ports[i].router2.getSimulatedIPAddress(),
					ports[i].router2.getProcessPortNumber(), ports[i].weight);
			this.lsd.addLink(rd.getSimulatedIPAddress(), newLinkDes);
			isUpdated = true;

			// client.close();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return isUpdated;
	}
}
