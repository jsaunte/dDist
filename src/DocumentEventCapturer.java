import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.rowset.spi.SyncResolver;
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
	private Lock eventHistoryLock;
	private Lock peerLock;
	private DistributedTextEditor editor;

	public DocumentEventCapturer(LamportClock lc, DistributedTextEditor editor) {
		this.editor = editor;
		this.lc = lc;
		eventHistoryLock = new ReentrantLock();
		peerLock = new ReentrantLock();
		peers = new ArrayList<Peer>();
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
		sendObjectToAllPeers(e);
	}

	public synchronized void remove(FilterBypass fb, int offset, int length)
			throws BadLocationException {
		lc.increment();
		TextEvent e = new TextRemoveEvent(length, lc.getTimeStamp());
		eventHistoryLock.lock();
		eventHistory.add(e);
		eventHistoryLock.unlock();
		sendObjectToAllPeers(e);
	}

	public synchronized void replace(FilterBypass fb, int offset, int length,
			String str, AttributeSet a) throws BadLocationException {
		if (length > 0) {
			lc.increment();
			TextEvent e1 = new TextRemoveEvent(length, lc.getTimeStamp());
			eventHistoryLock.lock();
			eventHistory.add(e1);
			eventHistoryLock.unlock();
			sendObjectToAllPeers(e1);
		}
		lc.increment();
		TextEvent e2 = new TextInsertEvent(str, lc.getTimeStamp());
		eventHistoryLock.lock();
		eventHistory.add(e2);
		eventHistoryLock.unlock();
		sendObjectToAllPeers(e2);
	}
	
	public Lock getEventHistoryLock() {
		return eventHistoryLock;
	}
	
	public void sendObjectToAllPeers(Object o) {
		for(Peer p : peers) {
			p.writeObjectToStream(o);
		}
	}
	
	public ArrayList<Peer> getPeers() {
		return peers;
	}
	
	public void addPeer(Peer p) {
		peerLock.lock();
		peers.add(p);
		updateConnectionStatusArea();
		peerLock.unlock();
	}
	
	public void updateConnectionStatusArea() {
		String res = "";
		for(Peer peer : peers) {
			res += "Connected to: " + peer.getIP() + " who is listening on port: " + peer.getPort() + " with ID: " + peer.getId() + "\n";
		}
		editor.setTextInArea2(res);
	}

	public int getNextId() {
		int nextid = 2;
		for(Peer p : peers) {
			if(p.getId() >= nextid) {
				nextid = p.getId() + 1;
			}
		}
		return nextid;
	}
	
	public void setPeers(ArrayList<Peer> peers) {
		this.peers = peers;
	}
	
	public Lock getPeerLock() {
		return peerLock;
	}
}