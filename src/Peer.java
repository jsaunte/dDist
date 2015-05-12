import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;


public class Peer extends Thread {
	
	private int id;
	private int caretPos;
	private DistributedTextEditor editor;
	private EventReplayer replayer;
	private Socket c;
	private LamportClock lc;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private Lock eventHistoryLock;
	
	public Peer(DistributedTextEditor editor, EventReplayer replayer, int id, Socket c, LamportClock lc) {
		this.editor = editor;
		this.replayer = replayer;
		this.id = id;
		this.c = c;
		this.lc = lc;
		caretPos = 0;
		
		try {
			output = new ObjectOutputStream(c.getOutputStream());
			input = new ObjectInputStream(c.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void start() {
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
		
	}
	

}
