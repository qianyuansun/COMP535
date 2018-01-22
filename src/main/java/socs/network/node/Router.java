package socs.network.node;

import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];
  

  public Router(Configuration config) {
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    lsd = new LinkStateDatabase(rd);
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {

  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

	  for(Link link: ports){
		  if(link != null && link.router2.getProcessPortNumber() == portNumber){				  
			  link = null;
		  }
	  }
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort,
                             String simulatedIP, short weight) {
	  
	  String msg = "Connection cannot be established." ;
	  
	  for(Link link: ports){
		  if(link == null){
			  RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP);
			  link = new Link(rd, r2);
			  msg = "Connection established." ;
			  break;
		  }
	  }
	  
	  System.out.println(msg);
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {
	  //client
	  try{
		  Socket client = new Socket(hostName, port);
		  System.out.println("Connected to "+ client.getRemoteSocketAddress());
		  OutputStream outToServer = client.getOutputStream();
		  DataOutputStream message_out = new DataOutputStream(outToServer);
		  message_out.writeUTF("Hello From " + client.getLocalSocketAddress());
		  InputStream inToClient = client.getInputStream();
		  DataInputStream message_in = new DataInputStream(inToClient);
		  System.out.println(message_in.readUTF());
		  client.close();
	  } catch(IOException e){
		  e.printStackTrace();
	  }

	  //server
	  try {
		  ServerSocket serverSocket = new ServerSocket(rd.getProcessPortNumber());
		  Socket socket = serverSocket.accept();
		  //this.ports[0].router2.getSimulatedIPAddress();
		  //this.ports[0].router2.getProcessPortNumber();
		  BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		  inFromClient.readLine();
		  		
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) {

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {

  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

  }

  public void terminal() {
    try {
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
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else {
          //invalid command
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
