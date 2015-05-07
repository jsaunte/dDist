import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.HashMap;
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
	private Socket client;
	private ObjectInputStream input;
	private DistributedTextEditor editor;
	private PriorityBlockingQueue<TextEvent> eventHistory;
	private HashMap<TimeStamp, Boolean> map;
	private Lock mapLock, eventHistoryLock;
	private LamportClock lc;
	private HashMap<Integer, Integer> carets;
	
	/*
	 * The constructor creates Output- and Input-Streams, and creates a thread which continuously will read TextEvent-objects from the InputStream
	 * When the InputStream receives null, the thread will write null to the other, and then both peers will close their sockets. 
	 * It calls on method on the editor to update it appropriately. 
	 */
	public EventReplayer(DistributedTextEditor editor, DocumentEventCapturer dec, Socket c, LamportClock lc) {
		this.dec = dec;
		this.lc = lc;
		this.client = c;
		this.editor = editor;
		eventHistory = dec.eventHistory;
		map = new HashMap<TimeStamp, Boolean>();
		carets = new HashMap<Integer, Integer>();
		carets.put(1, 0);
		carets.put(2, 0);
		mapLock = new ReentrantLock();
		eventHistoryLock = dec.getEventHistoryLock();
		input = dec.getInputStream();
		startReadInputStreamThread();
	}
	
	/** 
	 * When the EventReplayer runs, it empties the eventHistory. Whenever an element is taken out of the queue, it checks that the event has been acknowledged. 
	 * Then it updates the position of the carets, depending on which event it is.  
	 */
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
					map.remove(match);
				}
				mapLock.unlock();
			}		
		}
		System.out.println("I'm the thread running the EventReplayer, now I die!");
	}
	
	/**
	 * This thread continously reads input from the other peer. 
	 * Reading an event, will store the event in the priority-queue, and acknowledges the event for both peers.
	 * Reading an acknowledgement will put this acknowledgement into a map.
	 * Reading a caretUpdate will update the position of the caret for the given id.
	 */
	private void startReadInputStreamThread() {
		Runnable streamToQueue = new Runnable() {
			@Override
			public void run() {
				try {
					Object o;
					while((o = input.readObject()) != null) {
						if(o instanceof TextEvent) {
							TextEvent e = (TextEvent) o;
							lc.setMaxTime(e.getTimeStamp());
							eventHistoryLock.lock();
							eventHistory.add(e);
							eventHistoryLock.unlock();
							dec.writeObjectToStream(new Acknowledge(e));
							mapLock.lock();
							map.put(e.getTimeStamp(), true);
							mapLock.unlock();
						} else if (o instanceof Acknowledge){
							Acknowledge a = (Acknowledge) o;
							mapLock.lock();
							map.put(a.getEvent().getTimeStamp(), true);
							mapLock.unlock(); 
						} else if (o instanceof CaretUpdate) {
							CaretUpdate cu = (CaretUpdate) o;
							carets.put(cu.getID(), cu.getPos());
						}
					}
					if(!client.isClosed()) {
						dec.writeObjectToStream(null);
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
		if(!client.isClosed()) {
			dec.writeObjectToStream(null);
		}
	}
	
	public void updateCaretPos(int id, int pos) {
		carets.put(id, pos);
	}
}
