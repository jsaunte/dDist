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
	private Lock ackLock, eventHistoryLock, caretLock;
	private LamportClock lc;
	private HashMap<Integer, Integer> carets;
	private boolean wasInterrupted = false;
	private int maxIdSoFar;
	
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
		ackLock = new ReentrantLock();
		caretLock = new ReentrantLock();
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
				ackLock.lock();
				boolean isAck = true;
				if(acknowledgements.containsKey(head.getTimeStamp()) && !dec.getPeers().isEmpty()) {
					Set<Integer> set = acknowledgements.get(head.getTimeStamp());
					dec.getPeerLock().lock();
					for(Peer p : dec.getPeers()) {
						if(!set.contains(p.getId())) {
							isAck = false;
							break;
						}
					}
					dec.getPeerLock().unlock();
					if(isAck && head.equals(eventHistory.peek()))  {
						try {
							eventHistoryLock.lock();
							match = head.getTimeStamp();							
							TextEvent e = eventHistory.take();
							int idOfSender = e.getTimeStamp().getID();
							caretLock.lock();
							int pos = carets.get(idOfSender);
							e.doEvent(editor, pos);
							updateAllCarets(e, pos);
							caretLock.unlock();
							eventHistoryLock.unlock();

						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
				}
				}
				if(match != null) {
					acknowledgements.remove(match);
				}
				ackLock.unlock();
			}		
		}
		System.out.println("I'm the thread running the EventReplayer, now I die!");
	}
	
	
	/* 
	 * Will send null to the other peer if the connection is not closed.
	 */
	public void stopStreamToQueue() {
		dec.getPeerLock().lock();
		for(Peer p : dec.getPeers()) {
			if (p.isConnected()) {
				p.writeObjectToStream(null);
			}
		}
		dec.getPeerLock().unlock();
		wasInterrupted = true;
	}
	
	public void updateCaretPos(int id, int pos) {
		caretLock.lock();
		carets.put(id, pos);
		caretLock.unlock();
	}
	
	private void updateAllCarets(TextEvent e, int pos) {
//		caretLock.lock();
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
//		caretLock.unlock();
	}
	
	public PriorityBlockingQueue<TextEvent> getEventHistory() {
		return eventHistory;
	}

	public Lock getMapLock() {
		return ackLock;
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
	
	public void addAcknowledgement(TimeStamp ts, int id) {
		ackLock.lock();
		if(!acknowledgements.containsKey(ts)) {
			acknowledgements.put(ts, new HashSet<Integer>());
		}
		acknowledgements.get(ts).add(id);
		ackLock.unlock();
	}
	public HashMap<TimeStamp, Set<Integer>> getAcknowledgements() {
		return acknowledgements;
	}

	public HashMap<Integer, Integer> getCarets() {
		return carets;
	}

	public void setEventHistory(PriorityBlockingQueue<TextEvent> eventHistory) {
		eventHistoryLock.lock();
		for(TextEvent e : eventHistory) {
			this.eventHistory.put(e);
		}
		eventHistoryLock.unlock();
	}

	public void setAcknowledgements(
			HashMap<TimeStamp, Set<Integer>> acknowledgements) {
		ackLock.lock();
		this.acknowledgements = acknowledgements;
		ackLock.unlock();
	}

	public void setCarets(HashMap<Integer, Integer> carets) {
		caretLock.lock();
		this.carets = carets;
		caretLock.unlock();
	}

	public void addCaretPos(int id, int caretPos) {
		caretLock.lock();
		carets.put(id, caretPos);
		caretLock.unlock();
	}

	public void removePeer(Peer peer) {
		dec.getPeerLock().lock();
		dec.getPeers().remove(peer);
		dec.updateConnectionStatusArea();
		dec.getPeerLock().unlock();
	
		caretLock.lock();
		carets.remove(peer.getId());
		caretLock.unlock();
	}
	
	public Lock getCaretLock() {
		return caretLock;
	}
}
