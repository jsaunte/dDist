import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * 
 * This class captures and remembers the text events of the given document on
 * which it is put as a filter. Normally a filter is used to put restrictions
 * on what can be written in a buffer. In our case we just use it to see all
 * the events and make a copy. 
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class DocumentEventCapturer extends DocumentFilter {

	/*
	 * We are using a blocking queue for two reasons: 
	 * 1) They are thread safe, i.e., we can have two threads add and take elements 
	 *    at the same time without any race conditions, so we do not have to do  
	 *    explicit synchronization.
	 * 2) It gives us a member take() which is blocking, i.e., if the queue is
	 *    empty, then take() will wait until new elements arrive, which is what
	 *    we want, as we then don't need to keep asking until there are new elements.
	 */
	protected LinkedBlockingQueue<MyTextEvent> eventHistory = new LinkedBlockingQueue<MyTextEvent>();
	private ObjectOutputStream oout;
	private Thread queueThread;
	private final DistributedTextEditor editor;
	private final Socket client;

	/*
	 * The constructor creates Output- and Input-Streams, and creates a thread which continuously will read TextEvent-objects from the InputStream
	 * When the InputStream receives null, the thread will write null to the other, and then both peers will close their sockets. 
	 * It calls on method on the editor to update it appropriately. 
	 */
	public DocumentEventCapturer(Socket c, DistributedTextEditor e) throws IOException {
		this.editor = e;
		this.client = c;
		oout = new ObjectOutputStream(client.getOutputStream());
		final ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
		Runnable streamToQueue = new Runnable() {
			@Override
			public void run() {
				try {
					MyTextEvent event;
					while((event =  (MyTextEvent) ois.readObject()) != null) {
						eventHistory.add(event);
					}
					if(!client.isClosed()) {
						oout.writeObject(null);
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
		queueThread = new Thread(streamToQueue);
		queueThread.start();
	}

	/* 
	 * Will send null to the other peer if the connection is not closed.
	 */
	public void stopStreamToQueue() {
		try {
			if(!client.isClosed()) {
				oout.writeObject(null);
			}
		} catch (IOException e) {
			editor.setErrorMessage("Connection lost stop");
		}
	}

	/**	
	 * If the queue is empty, then the call will block until an element arrives.
	 * If the thread gets interrupted while waiting, we throw InterruptedException.
	 * 
	 * @return Head of the recorded event queue. 
	 * @throws RemoteException 
	 */
	MyTextEvent take() throws InterruptedException, RemoteException {
		return eventHistory.take();
	}

	public void insertString(FilterBypass fb, int offset,
			String str, AttributeSet a)
					throws BadLocationException {

		/* Queue ra copy of the event and then modify the textarea */
		try {
			oout.writeObject(new TextInsertEvent(offset, str));
		} catch (IOException e) {
		}		
		super.insertString(fb, offset, str, a);
	}	

	public void remove(FilterBypass fb, int offset, int length) 					
			throws BadLocationException {
		/* Queue a copy of the event and then modify the textarea */
		try {
			oout.writeObject(new TextRemoveEvent(offset, length));
		} catch (IOException e) {
		}		
		super.remove(fb, offset, length);
	}

	public void replace(FilterBypass fb, int offset,
			int length, 
			String str, AttributeSet a)
					throws BadLocationException {

		/* Queue a copy of the event and then modify the text */
		try {
			if (length > 0) {
				oout.writeObject(new TextRemoveEvent(offset, length));
			}		
			oout.writeObject(new TextInsertEvent(offset, str));
		} catch (IOException e) {
		}
		super.replace(fb, offset, length, str, a);
	} 
}
