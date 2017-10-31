package project2.one;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
public class NodeInfo implements Comparable<NodeInfo>{
	
	int clientNum; //"port" routing number from 1 to 255, set by client
	int portNum; //actual port num
	int clientState = -1; //-1 for not init'd, 0 for init'd and sending, 1 for done sending
	Socket clientSocket; //comm socket
    BufferedReader in; // for reading input
    PrintWriter out; //for reading output
    
    /**
     * NodeInfo Constructor, sets all attributes except for clientNum, which is
     * set by flooding
     * @param portNum
     */
	NodeInfo (int portNum) {
		this.portNum = portNum;
		this.clientState = 0;
		
		//blocks until client successfully connects to switch
		try {
			ServerSocket serverSocket = new ServerSocket(portNum);
			this.clientSocket = serverSocket.accept();
			this.out =
			        new PrintWriter(this.clientSocket.getOutputStream(), true);
			this.in =
					 new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
			serverSocket.close();
			
		} catch (Exception e) {
	        e.printStackTrace();
	        System.out.println("Connection Error while initiating permanent socket on port " 
	        + portNum);
	    }
	}
	
	public static boolean allNodesDone(ArrayList<NodeInfo> list) {
		boolean defaultVal = true;
		for (NodeInfo node : list) {
			if (node.clientState != 1) {
				defaultVal = false;
			}
		}
		return defaultVal;
	}
	@Override
	public int compareTo(NodeInfo o) {
		return this.clientNum - o.clientNum;
	}
}