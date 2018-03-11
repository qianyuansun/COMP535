package socs.network.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;

class Vertex implements Comparable<Vertex> {
	// public Edge[] adjacencies;
	public List<Edge> adjacencies;
	public double minDistance = Double.POSITIVE_INFINITY;
	public Vertex previous;
	public String name;

	public Vertex() {
	}

	public Vertex(String argName) {
		name = argName;
	}

	public String toString() {
		return name;
	}

	public int compareTo(Vertex other) {
		return Double.compare(minDistance, other.minDistance);
	}

	// getter and setter
	public List<Edge> getAdjacencies() {
		return adjacencies;
	}

	public void setAdjacencies(List<Edge> adjacencies) {
		this.adjacencies = adjacencies;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}

class Edge {
	public final Vertex target;
	public final double weight;

	public Edge(Vertex argTarget, double argWeight) {
		target = argTarget;
		weight = argWeight;
	}
}

public class Dijkstra {
	HashMap<String, Vertex> nodeList = new HashMap<String, Vertex>();

	public void computePaths(Vertex source) {
		source.minDistance = 0.;
		PriorityQueue<Vertex> vertexQueue = new PriorityQueue<Vertex>();
		vertexQueue.add(source);

		while (!vertexQueue.isEmpty()) {
			Vertex u = vertexQueue.poll();

			// Visit each edge exiting u
			for (Edge e : u.adjacencies) {
				Vertex v = e.target;
				double weight = e.weight;
				double distanceThroughU = u.minDistance + weight;
				if (distanceThroughU < v.minDistance) {
					vertexQueue.remove(v);

					v.minDistance = distanceThroughU;
					v.previous = u;
					vertexQueue.add(v);
				}
			}
		}
	}

	public List<Vertex> getShortestPathTo(Vertex target) {
		List<Vertex> path = new ArrayList<Vertex>();
		for (Vertex vertex = target; vertex != null; vertex = vertex.previous) {
			path.add(vertex);
		}

		Collections.reverse(path);
		return path;
	}

	public String getPath(HashMap<String, LSA> store, String source, String destination) {
		this.createNodes(store);
		Vertex target = nodeList.get(destination);
		computePaths(nodeList.get(source)); // run Dijkstra
		System.out.println("Distance to " + target + ": " + target.minDistance);
		List<Vertex> path = getShortestPathTo(target);
		String shortestPath = "Path: ";
		String[] path2 = new String[path.size()];
		path2[0] = path.get(0) + "->";
		shortestPath = shortestPath + path2[0];
		for(int i = 1; i < path.size(); i++){
			path2[i] = "(" + Double.toString(path.get(i).minDistance) + ")" + path.get(i) + "->";
			shortestPath = shortestPath + path2[i];
		}	
		return shortestPath;
	}

	public void createNodes(HashMap<String, LSA> store) {

		for (Entry<String, LSA> entry : store.entrySet()) {
			Vertex node = new Vertex(entry.getKey());
			nodeList.put(node.getName(), node);
		}

		for (Entry<String, Vertex> i : nodeList.entrySet()) {
			LinkedList<LinkDescription> adjNodes = store.get(i.getValue().getName()).links;
			List<Edge> adjacencies = new ArrayList<Edge>();
			for (LinkDescription j : adjNodes) {
				if (!j.linkID.equals(i.getValue().getName())) {
					adjacencies.add(new Edge(nodeList.get(j.linkID), j.tosMetrics));
				}
			}
			i.getValue().setAdjacencies(adjacencies);
		}
	}
}
