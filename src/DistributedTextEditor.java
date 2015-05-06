
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;


public class DistributedTextEditor extends JFrame {

	private static DistributedTextEditor editor;

	private JTextArea area1 = new JTextArea(20,120);
	private JTextArea area2 = new JTextArea(20,120);     
	private JTextField ipaddress = new JTextField("IP address here");     
	private JTextField portNumber = new JTextField("Port number here");     

	private EventReplayer er;
	private Thread ert; 

	private JFileChooser dialog = 
			new JFileChooser(System.getProperty("user.dir"));

	private String currentFile = "Untitled";
	private boolean changed = false;
	private boolean connected = false;
	private boolean active = false;
	private DocumentEventCapturer dec;
	private ServerSocket serverSocket;
	private Socket clientSocket;
	private LamportClock lc;

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
		setTitle("Disconnected");
		setVisible(true);
		area1.insert("Example of how to capture stuff from the event queue and replay it in another buffer.\n" +
				"Try to type and delete stuff in the top area.\n" + 
				"Then figure out how it works.\n", 0);

	}

	private KeyListener k1 = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
			changed = true;
			Save.setEnabled(true);
			SaveAs.setEnabled(true);
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
						registerOnPort();
						editor.setTitleToListen();
						while(active) {							
							clientSocket = waitForConnectionFromClient();
							lc = new LamportClock(1);
							area1.setText("");
							resetArea2();
							if (clientSocket != null) {
								setTitle("Connection from: " + clientSocket.getInetAddress().getHostAddress());
								connected = true;
								dec = new DocumentEventCapturer(lc, clientSocket);
								setDocumentFilter(dec);
								er = new EventReplayer(editor, dec, area2, clientSocket); 
								ert = new Thread(er);
								ert.start();
							}
						}
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
				setTitle("Connected to " + ipaddress.getText() + ":" + portNumber.getText() + "...");
				connected = true;
				lc = new LamportClock(2);
				dec = new DocumentEventCapturer(lc, clientSocket);
				((AbstractDocument)area1.getDocument()).setDocumentFilter(dec);
				er = new EventReplayer(editor, dec, area2, clientSocket);
				ert = new Thread(er);
				ert.start();
				changed = false;
				Connect.setEnabled(false);
				Disconnect.setEnabled(true);
				Listen.setEnabled(false);
				Save.setEnabled(false);
				SaveAs.setEnabled(false);

			} catch (NumberFormatException | IOException e1) {
				setTitle("Unable to connect");
			}

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

	public void setDocumentFilter(DocumentFilter filter) {
		((AbstractDocument)area1.getDocument()).setDocumentFilter(filter);
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

	public void resetArea2() {
		area2.setText("");
	}

	public boolean getActive() {
		return active;
	}

	public void setTitleToListen() {
		InetAddress local;
		try {
			local = InetAddress.getLocalHost();
			setTitle("I'm listening on: " + local.getHostAddress() + ":" + portNumber.getText());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}

	public void setErrorMessage(String s) {
		area2.setText("Error: " + s);
	}
	
	public DocumentFilter getDocumentFilter() {
		return dec;
	}
	
	public JTextArea getTextArea() {
		return area1;
	}

	public static void main(String[] arg) {
		editor = new DistributedTextEditor();
	}
}