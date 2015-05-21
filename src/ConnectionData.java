import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;


public class ConnectionData implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9010994792178486010L;
	private PriorityBlockingQueue<TextEvent> eventHistory;
	private HashMap<TimeStamp, Set<Integer>> acknowledgements;
	private HashMap<Integer, Integer> carets;
	private ArrayList<PeerWrapper> peers;
	private int id, hostid, port;
	private String textField;
	private TimeStamp ts;
	
	public PriorityBlockingQueue<TextEvent> getEventHistory() {
		return eventHistory;
	}

	public HashMap<TimeStamp, Set<Integer>> getAcknowledgements() {
		return acknowledgements;
	}

	public HashMap<Integer, Integer> getCarets() {
		return carets;
	}

	public int getId() {
		return id;
	}

	public String getTextField() {
		return textField;
	}

	public TimeStamp getTs() {
		return ts;
	}
	
	public int getHostId() {
		return hostid;
	}
	
	public ArrayList<PeerWrapper> getPeers() {
		return peers;
	}
	
	public int getPort() {
		return port;
	}

	public ConnectionData(PriorityBlockingQueue<TextEvent> eventHistory, HashMap<TimeStamp, Set<Integer>> acks,
			HashMap<Integer, Integer> carets, int id, String textField, TimeStamp ts, int hostid, ArrayList<Peer> peers, int port) {
		this.eventHistory = eventHistory;
		this.acknowledgements = acks;
		this.carets = carets;
		this.id = id;
		this.textField = textField;
		this.ts = ts;
		this.hostid = hostid;
		this.port = port;
		this.peers = new ArrayList<PeerWrapper>();
		for(Peer p : peers) {
			this.peers.add(new PeerWrapper(p.getId(), p.getPort(), p.getIP()));
		}
	}
	
}
