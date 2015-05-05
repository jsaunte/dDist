import javax.swing.JTextArea;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

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
	private PriorityBlockingQueue<TextEvent> sequencingQueue = new PriorityBlockingQueue<TextEvent>();
	
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
		if(editor.getActive()) {
			coordinatorThread();
		}
	}

	public void run() {
		boolean wasInterrupted = false;
		while (!wasInterrupted) {
			try {
				final TextEvent event = dec.take();
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						try {
							if(!editor.getActive()) {
								output.writeObject(event);
							} else {
								sequencingQueue.add(event);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
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
					TextEvent event;
					while((event =  (TextEvent) input.readObject()) != null) {
						if(editor.getActive()) {
							sequencingQueue.add(event);
						} else {
							event.doEvent(editor);
						}
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
	
	private void coordinatorThread() {
		Runnable r = new Runnable() {

			@Override
			public void run() {
				while(editor.getActive()) {
					try {
						TextEvent event = sequencingQueue.take();
						output.writeObject(event);
						event.doEvent(editor);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		};
		Thread t = new Thread(r);
		t.start();
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
}
