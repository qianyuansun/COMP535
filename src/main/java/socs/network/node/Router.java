package socs.network.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import socs.network.util.Configuration;

public class Router {

	RouterDescription rd = new RouterDescription();
	protected LinkStateDatabase lsd;

	public Router(Configuration config) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		lsd = new LinkStateDatabase(rd);
	}

	public void terminal() throws IOException {

		ServerSocket serverSocket = new ServerSocket(9090);
		System.out.println("Router:[" + rd.getSimulatedIPAddress() + "] ready...");
		while (true) {

			Socket socket = serverSocket.accept();
			new RouterThread(socket, rd).start();

		}
	}

}
