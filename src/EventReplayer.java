import javax.swing.JTextArea;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	private PriorityBlockingQueue<TextEvent> eventHistory;
	private HashMap<TimeStamp, Boolean> map;
	private Lock mapLock;
	
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
		eventHistory = dec.eventHistory;
		map = new HashMap<TimeStamp, Boolean>();
		mapLock = new ReentrantLock();
		try {
			output = dec.getOutputStream();
			input = new ObjectInputStream(c.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		startReadInputStreamThread();
	}

	public void run() {
		boolean wasInterrupted = false;
		while (!wasInterrupted) {
			if(!eventHistory.isEmpty()) {
				TextEvent head = eventHistory.peek();
				TimeStamp match = null;
				mapLock.lock();
				for(TimeStamp t : map.keySet()) {
					if(head.getTimeStamp().equals(t)) {
						try {
							match = t;
							TextEvent e = eventHistory.take();
							e.doEvent(editor);
							if(editor.getActive()) {
								System.out.println("at doEvent");
							}
							
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}
				if(match != null) {
					map.remove(match);
				}
				mapLock.unlock();
			}		
		}
		System.out.println("I'm the thread running the EventReplayer, now I die!");
	}
	
	private void startReadInputStreamThread() {
		Runnable streamToQueue = new Runnable() {
			@Override
			public void run() {
				try {
					Object o;
					while((o = input.readObject()) != null) {
						if(o instanceof TextEvent) {
							TextEvent e = (TextEvent) o;
							eventHistory.add(e);
							output.writeObject(new Acknowledge(e));
							mapLock.lock();
							map.put(e.getTimeStamp(), true);
							mapLock.unlock();
						} else if (o instanceof Acknowledge){
							Acknowledge a = (Acknowledge) o;
							mapLock.lock();
							map.put(a.getEvent().getTimeStamp(), true);
							mapLock.unlock(); 
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
