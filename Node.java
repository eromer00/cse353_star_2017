package project2.one;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Node implements Runnable{

	private int node_number = 0;
	private static int node_num_static = 0;
	
	private int port_number = 5000;
	
	private List<String> send = null;
	private List<String> bytes = null;
	private List<String> received = null;
	
	private Socket node = null;
	private boolean kill = true;
	
	private PrintWriter w = null;
	
	public Node(int num, String filename) {
		
		setNodeNumber(num);
		setStaticNodeNumber(num);
		
		
		/* Read in given file */
		try {
			String l;
			BufferedReader buf = new BufferedReader(new FileReader(filename));
			send = new ArrayList<String>();
			received = new ArrayList<String>();
			
			while((l = buf.readLine()) != null) {
				send.add(l);
			}
			buf.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		
		/* Convert to binary bytes to be sent over socket */
		Iterator<String> it = send.iterator();
		bytes = new ArrayList<String>();
		
		while(it.hasNext()) {
			String[] split = it.next().split(":");
			Frame_Project f = null;
			try {
				f = new Frame_Project(getNodeNumber(), Integer.parseInt(split[0]), split[1]);
				bytes.add(f.getFrame());
			} catch (NumberFormatException | UnsupportedEncodingException e) {
				e.printStackTrace();
			} 
		}
	
		/* setup text file for amending */
		try {
			String fn = "node" + getNodeNumber() + "output.txt";
			w = new PrintWriter(fn, "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		
	}
	
	
	/* Getters and Setters */
	public void setNodeNumber(int num) {
		this.node_number = num;
	}
	public int getNodeNumber() {
		return node_number;
	}
	public static int getStaticNodeNumber() {
		return node_num_static;
	}
	public static void setStaticNodeNumber(int num) {
		node_num_static = num;
	}
	
	public void setPortNumber(int port) {
		this.port_number = port;
	}
	public int getPortNumber() {
		return port_number;
	}
	
	public void setSocket(Socket node) {
		this.node = node;
	}
	public Socket getSocket() {
		return node;
	}
	
	public List<String> getBytes() {
		return bytes;
	}
	/* kills the socket connection */
	public void kill() {
		this.kill = false;
	}
	
	public void tofile() {
		List<String> tmp = received;
		
		String filename = "node" + 1 + "output.txt";
		
		try {
			PrintWriter w = new PrintWriter(filename, "UTF-8");
			while(!tmp.isEmpty()) {
				String current = tmp.get(0);
				tmp.remove(0);
				current = current.substring(24, current.length());
				
				String convert = "";
				
				for(int i = 0; i <= current.length() - 8;) {
					int j = Integer.parseInt(current.substring(i, i + 8), 2);
					convert += (char)j;
					i += 8;
				} 
				convert = 1 + ":" + convert;
				w.println(convert);			
			}
			w.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			System.out.println("Error creating output file");
		}
	}
	public static String output(String bin) {
		String current = bin.substring(24, bin.length());
		
		String convert = "";
		
		for(int i = 0; i <= current.length() - 8;) {
			int j = Integer.parseInt(current.substring(i, i + 8), 2);
			convert += (char)j;
			i += 8;
		} 
		
		convert = getStaticNodeNumber() + ":" + convert;
		
		return convert;
	}
	
	@Override
	public void run() {
		
		boolean setup = true;
		/* looping setup of socket screws up stuff, should work with switch */
		
		/* setup initial socket, get new port number */
		boolean initsocket = true;
		while(initsocket) {
			try {
				setSocket(new Socket("localhost", 5000));
				Scanner tmp = new Scanner(getSocket().getInputStream());
				while (!tmp.hasNextLine()) {
						Thread.sleep(1);
				}
				setPortNumber(Integer.parseInt(tmp.nextLine()));
				tmp.close();
				getSocket().close();
				setSocket(null);
				initsocket = false;
			} catch (IOException e) {	
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
		while(setup) {
			try {
				setSocket(new Socket("localhost", getPortNumber()));
				setup = false;
			} catch (IOException e) {
				try {
					Thread.sleep(300);
				} catch (InterruptedException e1) {}
			}
		}
		
		try {
			Scanner sc = new Scanner(getSocket().getInputStream());
			PrintStream ps = new PrintStream(getSocket().getOutputStream());
			
			boolean is_flooded = false;
			/* change to check if kill received by switch */
			while(kill) {
				
				boolean ack = true;	
				String pkt = "";
				Frame_Project packetRecieved = null;
				Frame_Project packetSent = null;
				
				/* sending data through socket */
				if(!bytes.isEmpty() && is_flooded) {
					String current = bytes.get(0);
					bytes.remove(0);
					System.out.println("current frame sent from node: " + current);
					ps.println(current);
				}
				/*
				 * Once the input file is empty this loop will run. It will be waiting for the termination frame 
				 * and until then will be open for frames input and will send ACKs for any received frames
				 */
				else if(bytes.isEmpty()) {
					System.out.println("Node: " + this.getNodeNumber() + " in final loop");
					ps.println( new Frame_Project(this.getNodeNumber(), 0, "").getFrame());
					ack = false;
					while(true) {
						if(sc.hasNextLine()) {
							packetRecieved = new Frame_Project(sc.nextLine());
							if(Frame_Project.getSrc(packetRecieved.getFrame()) == 0) {
								kill = false;
								break;
							}
							else if(!Frame_Project.isACK(packetRecieved.getFrame())) {
								w.println(output(packetRecieved.getFrame()));
								packetSent = new Frame_Project(Frame_Project.getDest(packetRecieved.getFrame()), Frame_Project.getSrc(packetRecieved.getFrame()), "");
								ps.println(packetSent.getFrame());
							}
						}
					}
				}
				
				/* receiving data through socket */
				
				while(ack) {
					try {
						pkt = sc.nextLine();
						packetRecieved = new Frame_Project(pkt);
					} catch(NoSuchElementException e) {}
					
					/* setup 4 frame types */
					
					
					/* flood */
					if(Frame_Project.isFloodFrame(packetRecieved.getFrame())) {
						System.out.printf("Node %d is now in flood\n", this.getNodeNumber());
						Frame_Project floodsend = new Frame_Project(getNodeNumber(),getNodeNumber(), "");
						System.out.println("floodSend = " + floodsend.getFrame());
						if(Frame_Project.isFloodResponseFrame(floodsend.getFrame())) {
							ps.println(floodsend.getFrame());
							ack = false;
						}
						is_flooded = true;
					}
					
					/* done sending */
					else if(Frame_Project.isDoneSendingFrame(packetRecieved.getFrame())) {
						System.out.printf("Node %d is now in done sending\n", this.getNodeNumber());
					} 
					
					
					/* terminate */
					else if(Frame_Project.isTerminateClientFrame(packetRecieved.getFrame())) {
						System.out.printf("Node %d is now in terminat\n", this.getNodeNumber());
						System.out.printf("Kill called in %d\n", this.getNodeNumber());
						kill();
					}

					/* ACK */
					else if(Frame_Project.isACK(packetRecieved.getFrame())) {
						ack = false;
						System.out.printf("Node %d is now in ack\n", this.getNodeNumber());
					} 	
					else {
							System.out.printf("Node %d is now in write\n", this.getNodeNumber());
							received.add(packetRecieved.getFrame());
							/* add to file */
							if(!received.isEmpty()) {
								String current = received.get(0);
								received.remove(0);
								System.out.println("writing: " + current);
								w.println(output(current));
								w.flush();
								System.out.printf("Node %d finished writing, sending ack\n", this.getNodeNumber());
								packetSent = new Frame_Project(Frame_Project.getDest(current), Frame_Project.getSrc(current), "");
								ps.println(packetSent.getFrame());
							}
					}
					/*
					try {
							Thread.sleep(10000);
					} catch (InterruptedException e) {}
					*/
					
				}
			}
			System.out.printf("Node %d killed", this.getNodeNumber());
			
			/* shut down sockets, scanners, output file */
			sc.close();
			node.close();
			
			/* safety measure, check if received is empty (should be) and send anything left behind into file */
			while(!received.isEmpty()) {
				String current = received.get(0);
				received.remove(0);
				w.println(output(current));
			}
			w.close();			
			
		} catch (IOException e) {
		}
	}
}
