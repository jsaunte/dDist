
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

import java.util.concurrent.*;

public class DistributedTextEditor extends JFrame {

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
	private DocumentEventCapturer dec = new DocumentEventCapturer();
	private ServerSocket serverSocket;

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

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		area1.addKeyListener(k1);
		setTitle("Disconnected");
		setVisible(true);
		area1.insert("Example of how to capture stuff from the event queue and replay it in another buffer.\n" +
				"Try to type and delete stuff in the top area.\n" + 
				"Then figure out how it works.\n", 0);

		er = new EventReplayer(dec, area2);
		ert = new Thread(er);
		ert.start();
	}

	private KeyListener k1 = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
			changed = true;
			Save.setEnabled(true);
			SaveAs.setEnabled(true);
		}
	};

	Action Listen = new AbstractAction("Listen") {
		public void actionPerformed(ActionEvent e) {
			saveOld();
			area1.setText("");
			// TODO: Become a server listening for connections on some port.
			
			InetAddress local;
			try {
				local = InetAddress.getLocalHost();
				setTitle("I'm listening on: " + local.getHostAddress() + ":" + portNumber.getText());
				Runnable server = new Runnable() {
					public void run() {
						registerOnPort();
						while(true) {
							Socket client = waitForConnectionFromClient();
							if (client != null) {
								System.out.println("Connection from: " + client);
							} else {
								break;
							}
						}
					}
				};
				Thread serverThread = new Thread(server);
				serverThread.start();
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			changed = false;
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
			System.err.println("Cannot open server socket on port number" + portNumber);
			System.err.println(e);
			System.exit(-1);			
		}
	}
	
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

	Action Connect = new AbstractAction("Connect") {
		public void actionPerformed(ActionEvent e) {
			saveOld();
			area1.setText("");
			setTitle("Connecting to " + ipaddress.getText() + ":" + portNumber.getText() + "...");
			try {
				Socket client = new Socket(ipaddress.getText(),Integer.parseInt(portNumber.getText()));
			} catch (NumberFormatException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			changed = false;
			Save.setEnabled(false);
			SaveAs.setEnabled(false);
		}
	};

	Action Disconnect = new AbstractAction("Disconnect") {
		public void actionPerformed(ActionEvent e) {	
			setTitle("Disconnected");
			// TODO
		}
	};

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

	public static void main(String[] arg) {
		new DistributedTextEditor();
	}        

}
