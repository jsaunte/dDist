import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class Peer implements Runnable {
	private int id;
	private DistributedTextEditor editor;
	private EventReplayer replayer;
	private Socket client;
	private LamportClock lc;
	private ObjectOutputStream output;
	private ObjectInputStream input;

	public Peer(DistributedTextEditor editor, EventReplayer replayer, int id, Socket c, LamportClock lc) {
		this.editor = editor;
		this.replayer = replayer;
		this.id = id;
		client = c;
		this.lc = lc;

		try {
			output = new ObjectOutputStream(c.getOutputStream());
			input = new ObjectInputStream(c.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
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
					replayer.getDocumentEventCapturer().sendObjectToAllPeers(new Acknowledge(e));
					replayer.getMapLock().lock();
					replayer.addAcknowledgement(e.getTimeStamp(), e.getTimeStamp().getID());
					replayer.getMapLock().unlock();
				} else if (o instanceof Acknowledge){
					Acknowledge a = (Acknowledge) o;
					replayer.getMapLock().lock();
					replayer.addAcknowledgement(a.getEvent().getTimeStamp(), a.getID());
					replayer.getMapLock().unlock(); 
				} else if (o instanceof CaretUpdate) {
					CaretUpdate cu = (CaretUpdate) o;
					replayer.updateCaretPos(cu.getID(), cu.getPos());
				}
			}
			if(!client.isClosed()) {
				writeObjectToStream(null);
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
}