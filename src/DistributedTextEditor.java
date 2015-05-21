import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

import javax.swing.*;
import javax.swing.text.*;


public class DistributedTextEditor extends JFrame {

	private static final long serialVersionUID = -1412971829037207445L;

	private static DistributedTextEditor editor;

	private JTextArea area1 = new JTextArea(20,120);
	private JTextArea area2 = new JTextArea(20,120);     
	private JTextField ipaddress = new JTextField("localhost");     
	private JTextField portNumber = new JTextField("4242");     

	private EventReplayer er;
	private Thread ert; 

	private JFileChooser dialog = 
			new JFileChooser(System.getProperty("user.dir"));

	private String currentFile = "Untitled";
	private boolean changed = false;
	private boolean connected = false;
	private boolean active = false;
	private boolean locked = false;
	private boolean listen = false;
	private DocumentEventCapturer dec;
	private ServerSocket serverSocket;
	private Socket clientSocket;
	private LamportClock lc;
	private int serverport;

	public DistributedTextEditor() {
		area1.setFont(new Font("Monospaced",Font.PLAIN,12));

		area2.setFont(new Font("Monospaced",Font.PLAIN,12));
		((AbstractDocument)area1.getDocument()).setDocumentFilter(dec);
		area2.setEditable(false);

		Container content = getContentPane();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		JScrollPane scroll1 = 
				new JScrollPane(area1, 
						JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
						JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		content.add(scroll1,BorderLayout.CENTER);

		JScrollPane scroll2 = 
				new JScrollPane(area2, 
						JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
						JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		content.add(scroll2,BorderLayout.CENTER);	

		content.add(ipaddress,BorderLayout.CENTER);	
		content.add(portNumber,BorderLayout.CENTER);	

		JMenuBar JMB = new JMenuBar();
		setJMenuBar(JMB);
		JMenu file = new JMenu("File");
		JMenu edit = new JMenu("Edit");
		JMB.add(file); 
		JMB.add(edit);

		file.add(Listen);
		file.add(Connect);
		file.add(Disconnect);
		file.addSeparator();
		file.add(Save);
		file.add(SaveAs);
		file.add(Quit);

		edit.add(Copy);
		edit.add(Paste);
		edit.getItem(0).setText("Copy");
		edit.getItem(1).setText("Paste");

		Save.setEnabled(false);
		SaveAs.setEnabled(false);
		Disconnect.setEnabled(false);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		area1.addKeyListener(k1);
		area1.addMouseListener(m1);
		setTitle("Disconnected");
		setVisible(true);
		area1.insert("Welcome to Hjortehandlerne's distributed text editor. \n", 0);

		this.addWindowListener(w1);
	}

	private WindowListener w1 = new WindowListener() {
		/**
		 * Kill all active threads
		 */
		@Override
		public void windowClosing(WindowEvent e) {
			disconnect();
		}

		@Override
		public void windowActivated(WindowEvent e) {}
		@Override
		public void windowClosed(WindowEvent e) {}
		@Override
		public void windowDeactivated(WindowEvent e) {}
		@Override
		public void windowDeiconified(WindowEvent e) {}
		@Override
		public void windowIconified(WindowEvent e) {}
		@Override
		public void windowOpened(WindowEvent e) {}	
	};

	private KeyListener k1 = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
			changed = true;
			Save.setEnabled(true);
			SaveAs.setEnabled(true);
		}

		/**
		 * The keyReleased event ensures that the caret-position is updated for both peers, when the user moves the caret with the arrow-keys.
		 */		
		public void keyReleased(KeyEvent e) {
			int left = e.VK_LEFT;
			int right = e.VK_RIGHT;
			int up = e.VK_UP;
			int down = e.VK_DOWN;
			if(e.getKeyCode() == left || e.getKeyCode() == right || e.getKeyCode() == up || e.getKeyCode() == down) {
				if (dec == null) return;
				dec.sendObjectToAllPeers(new CaretUpdate(area1.getCaretPosition(), lc.getID()));
				er.updateCaretPos(lc.getID(), area1.getCaretPosition());
			}
		}
	};

	/**
	 * This mouselistener ensures that both peers have an updated caret-position for this user, when he moves his caret by a mouseclick.
	 */
	private MouseListener m1 = new MouseAdapter() {
		public void mouseReleased(MouseEvent e) {
			if(e.getButton() == e.BUTTON1 && connected) {
				dec.sendObjectToAllPeers(new CaretUpdate(area1.getCaretPosition(), lc.getID()));
				er.updateCaretPos(lc.getID(), area1.getCaretPosition());
			}
		}
	};


	/*
	 * This action is called when the Listen-button is fired. 
	 * It creates a serversocket, and awaits a connection.
	 * When the connection is made, the textfields are emptied, and the title is changed accordingly. 
	 * An eventcapturer is created, and the eventreplayer is started with the given eventcapturer. 
	 * The GUI-menu is updated appropriately.
	 */
	Action Listen = new AbstractAction("Listen") {
		public void actionPerformed(ActionEvent e) {
			saveOld();		
			final InetAddress local;
			active = true;
			try {
				local = InetAddress.getLocalHost();
				Runnable server = new Runnable() {
					public void run() {
						serverport = Integer.parseInt(portNumber.getText());
						registerOnPort();
						editor.setTitleToListen();						
						clientSocket = waitForConnectionFromClient();
						lc = new LamportClock(1);
						area1.setText("");
						resetArea2();
						if (clientSocket != null) {
							listen = true;
							connected = true;
							dec = new DocumentEventCapturer(lc, editor);
							setDocumentFilter(dec);
							er = new EventReplayer(editor, dec, lc); 
							ert = new Thread(er);
							ert.start();
							
							try {
								ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
								ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
								JoinNetworkRequest request = (JoinNetworkRequest) input.readObject();
								Peer peer = new Peer(editor, er, 2, clientSocket, output, input, lc, clientSocket.getInetAddress().getHostAddress(), request.getPort());
								ConnectionData cd = new ConnectionData(er.getEventHistory(), er.getAcknowledgements(), er.getCarets(), 2, area1.getText(), lc.getTimeStamp(), lc.getID(), dec.getPeers(), serverSocket.getLocalPort());
								dec.addPeer(peer);
								Thread t = new Thread(peer);
								t.start();
								peer.writeObjectToStream(cd);
							} catch (IOException | ClassNotFoundException e) {
								e.printStackTrace();
							}
						}
						waitForConnection();
					}
				};
				Thread serverThread = new Thread(server);
				serverThread.start();
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}

			changed = false;
			Disconnect.setEnabled(true);
			Listen.setEnabled(false);
			Connect.setEnabled(false);
			Save.setEnabled(false);
			SaveAs.setEnabled(false);
		}
	};
	
	private void waitForConnection() {
		while(active) {
			Socket client = waitForConnectionFromClient();
			if(client != null) {
				try {
					ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
					ObjectInputStream input = new ObjectInputStream(client.getInputStream());
					Object o = input.readObject();
					
					if(o instanceof JoinNetworkRequest) {
						JoinNetworkRequest request = (JoinNetworkRequest) o;
						dec.sendObjectToAllPeers(new LockRequest(lc.getTimeStamp()));
						waitForAllToLock();
						locked = true;
						int id = dec.getNextId();
						Peer p = new Peer(editor, er, id, client, output, input, lc, client.getInetAddress().getHostAddress(), request.getPort());
						ConnectionData cd = new ConnectionData(er.getEventHistory(), er.getAcknowledgements(), er.getCarets(), id, area1.getText(), lc.getTimeStamp(), lc.getID(), dec.getPeers(), serverSocket.getLocalPort());
						p.writeObjectToStream(cd);
						dec.addPeer(p);
						Thread t = new Thread(p);
						t.start();
						er.addCaretPos(id, 0);
					} else if(o instanceof NewPeerDataRequest) {
						NewPeerDataRequest request = (NewPeerDataRequest) o;
						
						Peer newPeer = new Peer(editor, er, request.getId(), client, output, input, lc, client.getLocalAddress().getHostName(), request.getPort());
						dec.addPeer(newPeer);
						er.addCaretPos(request.getId(), request.getCaretPos());
						newPeer.writeObjectToStream(new NewPeerDataAcknowledgement(lc.getTimeStamp()));
						Thread t = new Thread(newPeer);
						t.start();
					}
				} catch (IOException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private void waitForAllToLock() {
		for(Peer p : dec.getPeers()) {
			while(!p.isLocked() && p.isConnected()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	/**
	 *
	 * Will register this server on the port number portNumber. Will not start waiting
	 * for connections. For this you should call waitForConnectionFromClient().
	 */
	private void registerOnPort() {
		try {
			serverSocket = new ServerSocket(Integer.parseInt(portNumber.getText()));
		} catch (IOException e) {
			serverSocket = null;
			System.err.println("Cannot open server socket on port number" + portNumber.getText());
			System.err.println(e);
			System.exit(-1);			
		}
	}

	/**
	 * Closes the serversocket
	 */
	public void deregisterOnPort() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
				serverSocket = null;
			} catch (IOException e) {
				System.err.println(e);
			}
		}
	}

	/**
	 *
	 * Waits for the next client to connect on port number portNumber or takes the 
	 * next one in line in case a client is already trying to connect. Returns the
	 * socket of the connection, null if there were any failures.
	 */
	private Socket waitForConnectionFromClient() {
		Socket res = null;
		try {
			res = serverSocket.accept();	
		} catch (IOException e) {
			// We return null on IOExceptions
		}
		return res;
	}

	/*
	 * This action is called when the Connect-button is fired. 
	 * It empties the textareas, and creates a ClientSocket, on the IP-adress and the Port-number taken from the textinput-areas.
	 * An eventcapturer is created, and the eventreplayer is started with the given eventcapturer. 
	 * The GUI-menu is updated appropriately.
	 */
	Action Connect = new AbstractAction("Connect") {
		public void actionPerformed(ActionEvent e) {
			saveOld();
			area1.setText("");
			resetArea2();
			try {
				clientSocket = new Socket(ipaddress.getText(),Integer.parseInt(portNumber.getText()));
				Random r = new Random();
				serverport = 10000 + r.nextInt(8999); // random port :D
				
				serverSocket = new ServerSocket(serverport);
				active = true;
				editor.setTitleToListen();

				connected = true;
				
				ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
				ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
				output.writeObject(new JoinNetworkRequest(serverport));
				
				ConnectionData data = getConnectionData(clientSocket, input);
				
				lc = new LamportClock(data.getId());
				lc.setMaxTime(data.getTs());				
				dec = new DocumentEventCapturer(lc, editor);
				er = new EventReplayer(editor, dec, lc); 
				ert = new Thread(er);
				ert.start();
				
				Peer peer = new Peer(editor, er, data.getHostId(), clientSocket, output, input, lc, clientSocket.getInetAddress().getHostAddress(), data.getPort());
				dec.addPeer(peer);
				Thread thread = new Thread(peer);
				thread.start();
				
				er.setAcknowledgements(data.getAcknowledgements());
				er.setEventHistory(data.getEventHistory());
				er.setCarets(data.getCarets());
				er.getCarets().put(lc.getID(), 0);
				
				
				
				for(PeerWrapper p : data.getPeers()) {
					Socket socket;
					try {
						socket = connectToPeer(p.getIP(), p.getPort());
						ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
						ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
						
						// TODO : Note that caret pos is hardcoded to 0. Might give problem
						outputStream.writeObject(new NewPeerDataRequest(lc.getID(), serverSocket.getLocalPort(), 0));
//						NewPeerDataAcknowledgement ack = (NewPeerDataAcknowledgement) input.readObject();
						Peer newPeer = new Peer(editor, er, p.getId(), socket, outputStream, inputStream, lc, p.getIP(), p.getPort());
						dec.addPeer(newPeer);
						Thread t = new Thread(newPeer);
						t.start();
					} catch(IOException ex) {
						continue;
					}
				}
				Thread t1 = new Thread(new Runnable() {
					
					@Override
					public void run() {
						waitForConnection();						
					}
				});
				t1.start();
				area1.setText(data.getTextField());
				area1.setCaretPosition(0);
				setDocumentFilter(dec);

				dec.sendObjectToAllPeers(new UnlockRequest(lc.getTimeStamp()));
				
				changed = false;
				Connect.setEnabled(false);
				Disconnect.setEnabled(true);
				Listen.setEnabled(false);
				Save.setEnabled(false);
				SaveAs.setEnabled(false);
			} catch (NumberFormatException | IOException  e1) {
				setTitle("Unable to connect");
			}
		}
		
		private Socket connectToPeer(String ip, int port) {
			try {
				Socket peer = new Socket(ip, port);
				return peer;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		private ConnectionData getConnectionData(Socket clientSocket, ObjectInputStream input) {
			ConnectionData res;
			try {
				Object o = input.readObject();
				res = (ConnectionData) o;
				return res;
			} catch (IOException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	};

	/*
	 * This action is called when the Disconnect-button is fired.
	 */
	Action Disconnect = new AbstractAction("Disconnect") {
		public void actionPerformed(ActionEvent e) {
			disconnect();
		}
	};

	/* 
	 * The disconnect method will stop the eventreplayer, and send null to the other peer, to stop their reading from the stream.
	 * If a server calls disconnect, the serversocket will be closed.
	 * The GUI-menu is updated appropriately.
	 */
	public void disconnect() {
		setTitle("Disconnected");
		active = false;
		if(connected == true) {
			er.stopStreamToQueue();
			ert.interrupt();
			setDocumentFilter(null);
			connected = false;
		}				
		deregisterOnPort();
		Disconnect.setEnabled(false);
		Connect.setEnabled(true);
		Listen.setEnabled(true);
		Save.setEnabled(true);
		SaveAs.setEnabled(true);

	}

	Action Save = new AbstractAction("Save") {
		public void actionPerformed(ActionEvent e) {
			if(!currentFile.equals("Untitled"))
				saveFile(currentFile);
			else
				saveFileAs();
		}
	};

	Action SaveAs = new AbstractAction("Save as...") {
		public void actionPerformed(ActionEvent e) {
			saveFileAs();
		}
	};

	Action Quit = new AbstractAction("Quit") {
		public void actionPerformed(ActionEvent e) {
			saveOld();
			System.exit(0);
		}
	};

	ActionMap m = area1.getActionMap();
	Action Copy = m.get(DefaultEditorKit.copyAction);
	Action Paste = m.get(DefaultEditorKit.pasteAction);

	private void saveFileAs() {
		if(dialog.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
			saveFile(dialog.getSelectedFile().getAbsolutePath());
	}

	private void saveOld() {
		if(changed) {
			if(JOptionPane.showConfirmDialog(this, "Would you like to save "+ currentFile +" ?","Save",JOptionPane.YES_NO_OPTION)== JOptionPane.YES_OPTION)
				saveFile(currentFile);
		}
	}

	private void saveFile(String fileName) {
		try {
			FileWriter w = new FileWriter(fileName);
			area1.write(w);
			w.close();
			currentFile = fileName;
			changed = false;
			Save.setEnabled(false);
		}
		catch(IOException e) {
		}
	}

	public boolean getActive() {
		return active;
	}

	public DocumentFilter getDocumentFilter() {
		return dec;
	}

	public JTextArea getTextArea() {
		return area1;
	}
	
	public DocumentEventCapturer getDocumentEventCapturer() {
		return dec;
	}

	public void setDocumentFilter(DocumentFilter filter) {
		((AbstractDocument)area1.getDocument()).setDocumentFilter(filter);
	}

	public void setErrorMessage(String s) {
		area2.setText("Error: " + s);
	}

	public void setTitleToListen() {
//		InetAddress local;
//		local = serverSocket.getInetAddress();
//		setTitle("I'm listening on: " + local.getHostAddress() + ":" + portNumber.getText());
		try {
			String serverString = serverSocket.getInetAddress().getLocalHost().toString();
			String serverIP = serverString.split("/")[1];
			editor.setTitle("Listening on: " + serverIP + ":" + serverport);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
	}

	public void resetArea2() {
		area2.setText("");
	}

	public static void main(String[] arg) {
		editor = new DistributedTextEditor();
	}

	public void setLocked(boolean b) {
		locked = b;
		area1.setEnabled(!b);
	}
	
	public boolean getListen() {
		return listen;
	}

	public void setTextInArea2(String res) {
		area2.setText(res);		
	}
}