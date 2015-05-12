import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * 
 * This class captures and remembers the text events of the given document on
 * which it is put as a filter. Normally a filter is used to put restrictions on
 * what can be written in a buffer. In our case we just use it to see all the
 * events and make a copy.
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class DocumentEventCapturer extends DocumentFilter {

	/*
	 * We are using a blocking queue for two reasons: 1) They are thread safe,
	 * i.e., we can have two threads add and take elements at the same time
	 * without any race conditions, so we do not have to do explicit
	 * synchronization. 2) It gives us a member take() which is blocking, i.e.,
	 * if the queue is empty, then take() will wait until new elements arrive,
	 * which is what we want, as we then don't need to keep asking until there
	 * are new elements.
	 */
	protected PriorityBlockingQueue<TextEvent> eventHistory = new PriorityBlockingQueue<TextEvent>();
	private ArrayList<Peer> peers;
	private LamportClock lc;
	private Socket client;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private Lock eventHistoryLock;

	public DocumentEventCapturer(LamportClock lc, Socket client) {
		this.client = client;
		this.lc = lc;
		eventHistoryLock = new ReentrantLock();
		peers = new ArrayList<Peer>();
		try {
			output = new ObjectOutputStream(client.getOutputStream());
			input = new ObjectInputStream(client.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * If the queue is empty, then the call will block until an element arrives.
	 * If the thread gets interrupted while waiting, we throw
	 * InterruptedException.
	 * 
	 * @return Head of the recorded event queue.
	 * @throws RemoteException
	 */
	TextEvent take() throws InterruptedException, RemoteException {
		return eventHistory.take();
	}

	public synchronized void insertString(FilterBypass fb, int offset,
			String str, AttributeSet a) throws BadLocationException {
		lc.increment();
		TextEvent e = new TextInsertEvent(str, lc.getTimeStamp());
		eventHistoryLock.lock();
		eventHistory.add(e);
		eventHistoryLock.unlock();
		writeObjectToStream(e);
	}

	public synchronized void remove(FilterBypass fb, int offset, int length)
			throws BadLocationException {
		lc.increment();
		TextEvent e = new TextRemoveEvent(length, lc.getTimeStamp());
		eventHistoryLock.lock();
		eventHistory.add(e);
		eventHistoryLock.unlock();
		writeObjectToStream(e);
	}

	public synchronized void replace(FilterBypass fb, int offset, int length,
			String str, AttributeSet a) throws BadLocationException {
		if (length > 0) {
			lc.increment();
			TextEvent e1 = new TextRemoveEvent(length, lc.getTimeStamp());
			eventHistoryLock.lock();
			eventHistory.add(e1);
			eventHistoryLock.unlock();
			writeObjectToStream(e1);
		}
		lc.increment();
		TextEvent e2 = new TextInsertEvent(str, lc.getTimeStamp());
		eventHistoryLock.lock();
		eventHistory.add(e2);
		eventHistoryLock.unlock();
		writeObjectToStream(e2);
	}
	
	public ObjectOutputStream getOutputStream() {
		return output;
	}
	
	public Lock getEventHistoryLock() {
		return eventHistoryLock;
	}

	public ObjectInputStream getInputStream() {
		return input;
	}
	
	public void sendObjectToAllPeers(Object o) {
		for(Peer p : peers) {
			p.writeObjectToStream(o);
		}
	}
}
