package Chord;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class ChordNameServiceImpl extends Thread implements ChordNameService  {

	private boolean joining, active;
	private int port;
	protected InetSocketAddress myName;
	protected int myKey;
	private InetSocketAddress suc;
	private InetSocketAddress pre;
	private InetSocketAddress connectedAt;

	public int keyOfName(InetSocketAddress name)  {
		int tmp = name.hashCode()*1073741651 % 2147483647;
		if (tmp < 0) { tmp = -tmp; }
		return tmp;
	}

	public InetSocketAddress getChordName()  {
		return myName;
	}

	/**
	 * Computes the name of this peer by resolving the local host name
	 * and adding the current portname.
	 */
	protected InetSocketAddress _getMyName() {
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			InetSocketAddress name = new InetSocketAddress(localhost, port);
			return name;
		} catch (UnknownHostException e) {
			System.err.println("Cannot resolve the Internet address of the local host.");
			System.err.println(e);
		}
		return null;
	}

	public void createGroup(int port) {
		joining = false;
		this.port = port;
		myName = _getMyName();
		myKey = keyOfName(myName);
		start();
	}

	public void joinGroup(InetSocketAddress knownPeer, int port)  {
		joining = true;
		this.port = port;
		connectedAt = knownPeer;
		myName = _getMyName();
		myKey = keyOfName(myName);
		start();
	}

	public void leaveGroup() {
		// More code needed here!
	}

	public InetSocketAddress succ() {
		return suc; // You might want to modify this.
	}

	public InetSocketAddress pred() {
		return pre; // You might want to modify this.
	}

	public InetSocketAddress lookup(int key) throws IOException {
		/*
		 * The below works fine for singleton groups, but you might
		 * want to connect to the rest of the group to lookup the
		 * responsible if the group is larger.
		 */
		
		if(keyOfName(pre) == myKey) {
			return myName;
		}
		
		if (Helper.between(key, keyOfName(pre), myKey)) {
			return myName;
		}
		System.out.println("Lookup kaldt");
		return contactSuccessor("lookup," + key);
	}
	
	public InetSocketAddress contactSuccessor(String m) throws IOException {
		Socket successor = new Socket(InetAddress.getLocalHost(), suc.getPort());
		BufferedReader reader = new BufferedReader(new InputStreamReader(successor.getInputStream()));
		String answer = reader.readLine();
		String[] answers = answer.split(",");
		InetSocketAddress res = new InetSocketAddress(answers[0], Integer.parseInt(answers[1]));
		reader.close();
		successor.close();
		return res;
	}
	
	/**
	 *
	 * Waits for the next client to connect on port number portNumber or takes the 
	 * next one in line in case a client is already trying to connect. Returns the
	 * socket of the connection, null if there were any failures.
	 */
	private Socket waitForConnection(ServerSocket socket) {
		Socket res = null;
		try {
			res = socket.accept();
		} catch (IOException e) {
			// We return null on IOExceptions
		}
		return res;
	}

	public void run() {
		System.out.println("My name is " + myName + " and my key is " + myKey);
		
		/*
		 * If joining we should now enter the existing group and
		 * should at some point register this peer on its port if not
		 * already done and start listening for incoming connection
		 * from other peers who want to enter or leave the
		 * group. After leaveGroup() was called, the run() method
		 * should return so that the threat running it might
		 * terminate.
		 */
		suc = myName;
		pre = myName;
		
		try {
			if(joining) {
				Socket knownPeer = new Socket(connectedAt.getHostName(), connectedAt.getPort());
				PrintWriter toKnownPeer = new PrintWriter(knownPeer.getOutputStream(),true);
				toKnownPeer.println("lookup," + myKey);
				BufferedReader fromPeers = new BufferedReader(new InputStreamReader(knownPeer.getInputStream()));
				String answer = fromPeers.readLine();
				String[] parameters = answer.split(",");
				suc = new InetSocketAddress(parameters[0], Integer.parseInt(parameters[1]) );
				toKnownPeer.close();
				knownPeer.close();
				fromPeers.close();
				
				

				Socket sucSocket = new Socket(InetAddress.getLocalHost(), suc.getPort());
				PrintWriter toSuc = new PrintWriter(sucSocket.getOutputStream(),true);
				toSuc.println("getPre");
				BufferedReader fromSuc = new BufferedReader(new InputStreamReader(sucSocket.getInputStream()));
				String preString = fromSuc.readLine();
				String[] preStrings = preString.split(",");
				pre = new InetSocketAddress(preStrings[0], Integer.parseInt(preStrings[1]));
				
				toSuc.println("changePre," + myName.getHostName() + "," + myName.getPort());
				toSuc.close();
				fromSuc.close();
				sucSocket.close();
				
				Socket preSocket = new Socket(InetAddress.getLocalHost(), pre.getPort());
				PrintWriter toPre = new PrintWriter(preSocket.getOutputStream(),true);
				toPre.println("changeSuc," + myName.getHostName() + "," + myName.getPort());
				preSocket.close();
				toPre.close();				
				
				joining = false;
			}
			active = true;
			ServerSocket mySocket = new ServerSocket(port);
			while(active) {
				Socket client = waitForConnection(mySocket);
				if (client != null) {
					BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
					String msg = fromClient.readLine();
					String[] parameters = msg.split(",");
					PrintWriter toClient = new PrintWriter(client.getOutputStream(),true);
					if (parameters[0].equals("lookup")) {
						InetSocketAddress clientSuccessor = lookup(Integer.parseInt(parameters[1]));
						toClient.println(clientSuccessor.getAddress() + "," + clientSuccessor.getPort());
					} else if (parameters[0].equals("getPre")) {
						toClient.println(pre.getAddress() + "," + pre.getPort());
					} else if (parameters[0].equals("changePre")) {
						pre = new InetSocketAddress(parameters[1], Integer.parseInt(parameters[2]));
					} else if (parameters[0].equals("changeSuc")) {
						suc = new InetSocketAddress(parameters[1], Integer.parseInt(parameters[2]));
					}					
				}
				client.close();
				client = null;
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws InterruptedException {
		ChordNameService peer1 = new ChordNameServiceImpl();
		ChordNameService peer2 = new ChordNameServiceImpl();
		ChordNameService peer3 = new ChordNameServiceImpl();

		peer1.createGroup(40501);
		peer2.joinGroup(peer1.getChordName(),40502);
		Thread.sleep(4000);
		peer3.joinGroup(peer2.getChordName(),40503);

//		peer1.leaveGroup();
//		peer3.leaveGroup();
//		peer2.leaveGroup();
	}

}
