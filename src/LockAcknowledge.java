import java.io.Serializable;


public class LockAcknowledge implements Serializable {
	private TimeStamp ts;
	
	public LockAcknowledge(TimeStamp ts) {
		this.ts = ts;
	}
	
	public TimeStamp getTimeStamp() {
		return ts;
	}
}
