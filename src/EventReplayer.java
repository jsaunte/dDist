import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
	private DistributedTextEditor editor;
	private PriorityBlockingQueue<TextEvent> eventHistory;
	private HashMap<TimeStamp, Set<Integer>> acknowledgements;
	private Lock mapLock, eventHistoryLock;
	private LamportClock lc;
	private HashMap<Integer, Integer> carets;
	private boolean wasInterrupted = false;
	
	/*
	 * The constructor creates Output- and Input-Streams, and creates a thread which continuously will read TextEvent-objects from the InputStream
	 * When the InputStream receives null, the thread will write null to the other, and then both peers will close their sockets. 
	 * It calls on method on the editor to update it appropriately. 
	 */
	public EventReplayer(DistributedTextEditor editor, DocumentEventCapturer dec, LamportClock lc) {
		this.dec = dec;
		this.lc = lc;
		this.editor = editor;
		eventHistory = dec.eventHistory;
		acknowledgements = new HashMap<TimeStamp, Set<Integer>>();
		carets = new HashMap<Integer, Integer>();
		carets.put(1, 0);
		carets.put(2, 0);
		mapLock = new ReentrantLock();
		eventHistoryLock = dec.getEventHistoryLock();
	}
	
	/** 
	 * When the EventReplayer runs, it empties the eventHistory. Whenever an element is taken out of the queue, it checks that the event has been acknowledged. 
	 * Then it updates the position of the carets, depending on which event it is.  
	 */
	public void run() {
		while (!wasInterrupted) {
			if(!eventHistory.isEmpty()) {
				TextEvent head = eventHistory.peek();
				TimeStamp match = null;
				mapLock.lock();
				for(TimeStamp t : acknowledgements.keySet()) {
					if(head.getTimeStamp().equals(t)) {
						try {
							match = t;
							eventHistoryLock.lock();
							TextEvent e = eventHistory.take();
							int idOfSender = e.getTimeStamp().getID();
							int pos = carets.get(idOfSender);
							e.doEvent(editor, pos);				
							
							if(e instanceof TextInsertEvent) {
								for(int i : carets.keySet()) {
									if(carets.get(i) >= pos) {
										carets.put(i, carets.get(i) + e.getLength());
									}
								}
							} else if (e instanceof TextRemoveEvent) {
								for(int i : carets.keySet()) {
									if(carets.get(i) >= pos) {
										carets.put(i, carets.get(i) - e.getLength());
									}
								}
							}
							eventHistoryLock.unlock();
							
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}
				if(match != null) {
					acknowledgements.remove(match);
				}
				mapLock.unlock();
			}		
		}
		System.out.println("I'm the thread running the EventReplayer, now I die!");
	}
	
	
	/* 
	 * Will send null to the other peer if the connection is not closed.
	 */
	public synchronized void stopStreamToQueue() {
		for(Peer p : dec.getPeers()) {
			if (p.isConnected()) {
				p.writeObjectToStream(null);
			}
		}
		wasInterrupted = true;
	}
	
	public synchronized void updateCaretPos(int id, int pos) {
		carets.put(id, pos);
	}
	
	public PriorityBlockingQueue<TextEvent> getEventHistory() {
		return eventHistory;
	}

	public Lock getMapLock() {
		return mapLock;
	}

	public Lock getEventHistoryLock() {
		return eventHistoryLock;
	}

	public LamportClock getLc() {
		return lc;
	}
	
	public DocumentEventCapturer getDocumentEventCapturer() {
		return dec;
	}
	
	public synchronized void addAcknowledgement(TimeStamp ts, int id) {
		if(!acknowledgements.containsKey(ts)) {
			acknowledgements.put(ts, new HashSet<Integer>());
		}
		acknowledgements.get(ts).add(id);
	}
}
