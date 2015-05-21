import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;


public class Peer implements Runnable, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3160849123787642705L;
	private int id;
	private DistributedTextEditor editor;
	private EventReplayer replayer;
	private Socket client;
	private LamportClock lc;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private String ip;
	private int port;
	private boolean locked;

	public Peer(DistributedTextEditor editor, EventReplayer replayer, int id, Socket c, ObjectOutputStream output, ObjectInputStream input, LamportClock lc, String ip, int port) {
		this.editor = editor;
		this.replayer = replayer;
		this.id = id;
		client = c;
		this.lc = lc;
		locked = false;
		this.ip = ip;
		this.port = port;
		this.output = output;
		this.input = input;
	}

	@Override
	public void run() {
		try {
			Object o;
			while((o = input.readObject()) != null) {
				if(o instanceof TextEvent) {
					TextEvent e = (TextEvent) o;
					lc.setMaxTime(e.getTimeStamp());
					replayer.getEventHistoryLock().lock();
					replayer.getEventHistory().add(e);
					replayer.getEventHistoryLock().unlock();
//					System.out.println("Før ack sendes");
					replayer.getDocumentEventCapturer().sendObjectToAllPeers(new Acknowledge(e));
					replayer.getMapLock().lock();
					replayer.addAcknowledgement(e.getTimeStamp(), e.getTimeStamp().getID());
					replayer.getMapLock().unlock();
				} else if (o instanceof Acknowledge){
					Acknowledge a = (Acknowledge) o;
					replayer.getMapLock().lock();
					replayer.addAcknowledgement(a.getEvent().getTimeStamp(), id);
					replayer.getMapLock().unlock();
//					if (editor.getListen()) {
//						System.out.println("Acks");
//						for(TimeStamp t : replayer.getAcknowledgements().keySet()) {
//							System.out.println(t.getTime());
//						}
//						System.out.println("Events");
//						System.out.println(replayer.getEventHistory().size());
//					}
				} else if (o instanceof CaretUpdate) {
					CaretUpdate cu = (CaretUpdate) o;
					replayer.updateCaretPos(cu.getID(), cu.getPos());
				} else if (o instanceof LockRequest) {
					editor.setLocked(true);
					writeObjectToStream(new LockAcknowledge(lc.getTimeStamp()));
				} else if (o instanceof UnlockRequest) {
					editor.setLocked(false);
				} else if (o instanceof LockAcknowledge) {
					locked = true;
				}
			}
			if(!client.isClosed()) {
				writeObjectToStream(null);
			}
			client.close();
		} catch (EOFException e) {
			e.printStackTrace();
		} catch (IOException | ClassNotFoundException e) {
			if(!editor.getActive()) {
				editor.disconnect();
			}
			editor.setErrorMessage("Connection lost");
			e.printStackTrace();
		}
		if(!editor.getActive()) {
			editor.disconnect();
			editor.setTitle("Disconnected");
		} else {
			editor.setTitleToListen();
			editor.setDocumentFilter(null);
		}
	}
	
	public synchronized void writeObjectToStream(Object o) {
		try {
			if(!client.isClosed()) {
				output.writeObject(o);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isConnected() {
		return !client.isClosed();
	}
	
	public int getId() {
		return id;
	}
	
	public boolean isLocked() {
		return locked;
	}
	
	public String getIP() {
		return ip;
	}

	public int getPort() {
		return port;
	}
}