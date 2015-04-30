import javax.swing.JTextArea;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * Takes the event recorded by the DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class EventReplayer implements Runnable {

	private DocumentEventCapturer dec;
	private JTextArea area;
	private Socket client;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private DistributedTextEditor editor;
	protected LinkedBlockingQueue<MyTextEvent> localEventHistory = new LinkedBlockingQueue<MyTextEvent>();
	/*
	 * The constructor creates Output- and Input-Streams, and creates a thread which continuously will read TextEvent-objects from the InputStream
	 * When the InputStream receives null, the thread will write null to the other, and then both peers will close their sockets. 
	 * It calls on method on the editor to update it appropriately. 
	 */
	public EventReplayer(DistributedTextEditor editor, DocumentEventCapturer dec, JTextArea area, Socket c) {
		this.dec = dec;
		this.area = area;
		this.client = c;
		this.editor = editor;
		try {
			output = new ObjectOutputStream(c.getOutputStream());
			input = new ObjectInputStream(c.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		startReadInputStreamThread();
	}

	public void run() {
		boolean wasInterrupted = false;
		while (!wasInterrupted) {
			try {
				MyTextEvent mte = dec.take();
				if (mte instanceof TextInsertEvent) {
					final TextInsertEvent tie = (TextInsertEvent)mte;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							try {
								output.writeObject(tie);			
							} catch (Exception e) {
								System.err.println(e);
								/* We catch all exceptions, as an uncaught exception would make the 
								 * EDT unwind, which is now healthy.
								 */
							}
						}
					});
				} else if (mte instanceof TextRemoveEvent) {
					final TextRemoveEvent tre = (TextRemoveEvent)mte;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							try {
								output.writeObject(tre);
							} catch (Exception e) {
								System.err.println(e);
								/* We catch all exceptions, as an uncaught exception would make the 
								 * EDT unwind, which is now healthy.
								 */
							}
						}
					});
				} 
			} catch (Exception _) {
				wasInterrupted = true;
			}
		}
		System.out.println("I'm the thread running the EventReplayer, now I die!");
	}
	
	private void startReadInputStreamThread() {
		Runnable streamToQueue = new Runnable() {
			@Override
			public void run() {
				try {
					MyTextEvent event;
					while((event =  (MyTextEvent) input.readObject()) != null) {
						localEventHistory.add(event);						
					}
					if(!client.isClosed()) {
						output.writeObject(null);
					}
					client.close();
				} catch (IOException | ClassNotFoundException e) {
					if(!editor.getActive()) {
						editor.disconnect();
					}
					editor.setErrorMessage("Connection lost");
				}
				if(!editor.getActive()) {
					editor.disconnect();
					editor.setTitle("Disconnected");
				} else {
					editor.setTitleToListen();
					editor.setDocumentFilter(null);
				}
			}

		};
		Thread queueThread = new Thread(streamToQueue);
		queueThread.start();
	}
	/* 
	 * Will send null to the other peer if the connection is not closed.
	 */
	public void stopStreamToQueue() {
		try {
			if(!client.isClosed()) {
				output.writeObject(null);
			}
		} catch (IOException e) {
			editor.setErrorMessage("Connection lost stop");
		}
	}
	
	public void flushQueue() {
//		editor.setDocumentFilter(null);
		try {
			while(!localEventHistory.isEmpty()) {
				MyTextEvent event = localEventHistory.take();
				if (event instanceof TextInsertEvent) {
					final TextInsertEvent tie = (TextInsertEvent)event;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							try {
								area.insert(tie.getText(), tie.getOffset());				
							} catch (Exception e) {
								System.err.println(e);
								/* We catch all exceptions, as an uncaught exception would make the 
								 * EDT unwind, which is now healthy.
								 */
							}
						}
					});
				} else if (event instanceof TextRemoveEvent) {
					final TextRemoveEvent tre = (TextRemoveEvent)event;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							try {
								area.replaceRange(null, tre.getOffset(), tre.getOffset()+tre.getLength());
							} catch (Exception e) {
								System.err.println(e);
								/* We catch all exceptions, as an uncaught exception would make the 
								 * EDT unwind, which is now healthy.
								 */
							}
						}
					});
				}
			}
		} catch (InterruptedException e) {
			
		}
//		try {
//			Thread.sleep(1500);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		if(!dec.eventHistory.isEmpty()) {
			System.out.println("not local: " + dec.eventHistory.peek().toString());
		}
		if(!localEventHistory.isEmpty()) {
			System.out.println("local: " + localEventHistory.peek().toString());
		}

//		editor.setDocumentFilter(dec);
	}
}
