package socs.network.message;

import java.io.Serializable;
import java.util.LinkedList;

public class LSA implements Serializable {

  //IP address of the router originate this LSA
  public String linkStateID;
  public int lsaSeqNumber = Integer.MIN_VALUE;

  public LinkedList<LinkDescription> links = new LinkedList<LinkDescription>();

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(linkStateID + ":").append(lsaSeqNumber + "\n");
    for (LinkDescription ld : links) {
      sb.append(ld);
    }
    sb.append("\n");
    return sb.toString();
  }
  
  public void addLink(LinkDescription linkDes){
	  this.links.add(linkDes);
	  lsaSeqNumber++;
  }
  
  public void deleteLink(short portNum){
	  for(LinkDescription ld : links){
		 if(ld.portNum == portNum){
			 links.remove(ld);
			 break;
		 }
	  }
	  lsaSeqNumber++;
  }
  
  public LSA copy(){
	LSA copyLsa = new LSA();
	copyLsa.linkStateID = this.linkStateID;
	copyLsa.lsaSeqNumber = this.lsaSeqNumber;
	LinkedList<LinkDescription> copyLinks = new LinkedList<LinkDescription>();
	for(LinkDescription ld: links){
		copyLinks.add(ld);
	}
	copyLsa.links = copyLinks;
	return copyLsa;
	  
  }
}
