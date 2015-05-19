import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;


public class ConnectionData {
	private PriorityBlockingQueue<TextEvent> eventHistory;
	private HashMap<TimeStamp, Set<Integer>> acknowledgements;
	private HashMap<Integer, Integer> carets;
	private ArrayList<Peer> peers;
	private int id, hostid;
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
	
	public ArrayList<Peer> getPeers() {
		return peers;
	}

	public ConnectionData(PriorityBlockingQueue<TextEvent> eventHistory, HashMap<TimeStamp, Set<Integer>> acks,
			HashMap<Integer, Integer> carets, int id, String textField, TimeStamp ts, int hostid, ArrayList<Peer> peers) {
		this.eventHistory = eventHistory;
		this.acknowledgements = acks;
		this.carets = carets;
		this.id = id;
		this.textField = textField;
		this.ts = ts;
		this.hostid = hostid;
		this.peers = peers;
	}
	
}
