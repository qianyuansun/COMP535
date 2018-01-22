package socs.network.node;

public class RouterDescription {
	// used to socket communication
	String processIPAddress;
	short processPortNumber;
	// used to identify the router in the simulated network space
	String simulatedIPAddress;
	// status of the router
	RouterStatus status;

	public RouterDescription(){
		
	}
	
	public RouterDescription(String processIPAddress, short processPortNumber, String simulatedIPAddress) {
		this.processIPAddress = processIPAddress;
		this.processPortNumber = processPortNumber;
		this.simulatedIPAddress = simulatedIPAddress;
	}

	public String getProcessIPAddress() {
		return processIPAddress;
	}

	public void setProcessIPAddress(String processIPAddress) {
		this.processIPAddress = processIPAddress;
	}

	public short getProcessPortNumber() {
		return processPortNumber;
	}

	public void setProcessPortNumber(short processPortNumber) {
		this.processPortNumber = processPortNumber;
	}

	public String getSimulatedIPAddress() {
		return simulatedIPAddress;
	}

	public void setSimulatedIPAddress(String simulatedIPAddress) {
		this.simulatedIPAddress = simulatedIPAddress;
	}

	public RouterStatus getStatus() {
		return status;
	}

	public void setStatus(RouterStatus status) {
		this.status = status;
	}

}
