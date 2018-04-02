package socs.network.node;

import java.util.HashMap;

import socs.network.message.Dijkstra;
import socs.network.message.LSA;
import socs.network.message.LinkDescription;

public class LinkStateDatabase {

	// linkID => LSAInstance
	public HashMap<String, LSA> _store = new HashMap<String, LSA>();

	private RouterDescription rd = null;

	public LinkStateDatabase(RouterDescription routerDescription) {
		rd = routerDescription;
		LSA l = initLinkStateDatabase();
		_store.put(l.linkStateID, l);
	}

	/**
	 * output the shortest path from this router to the destination with the
	 * given IP address
	 */
	String getShortestPath(String destinationIP) {
		// TODO: fill the implementation here
		Dijkstra dijkstra = new Dijkstra();
		String path = dijkstra.getPath(_store, rd.getSimulatedIPAddress(), destinationIP);
		return path;
	}

	// initialize the linkstate database by adding an entry about the router
	// itself
	private LSA initLinkStateDatabase() {
		LSA lsa = new LSA();
		lsa.linkStateID = rd.simulatedIPAddress;
		lsa.lsaSeqNumber = Integer.MIN_VALUE;
		LinkDescription ld = new LinkDescription();
		ld.linkID = rd.simulatedIPAddress;
		ld.portNum = rd.getProcessPortNumber();
		ld.tosMetrics = 0;
		lsa.links.add(ld);
		return lsa;
	}

	public void addLink(String linkStateID, LinkDescription linkDes) {
		_store.get(linkStateID).addLink(linkDes);
	}
	
	public void deleteLink(String linkStateID, String simulatedIP, Short portNum){
		if(_store.get(linkStateID) != null)
			_store.get(linkStateID).deleteLink(portNum);
		if(_store.containsKey(simulatedIP))
			_store.remove(simulatedIP);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (LSA lsa : _store.values()) {
			sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
			for (LinkDescription ld : lsa.links) {
				sb.append(ld.linkID).append(",").append(ld.portNum).append(",").append(ld.tosMetrics).append("\t");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

}
