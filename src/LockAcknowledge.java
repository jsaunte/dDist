import java.io.Serializable;


public class LockAcknowledge implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3485464289034232431L;
	private TimeStamp ts;
	
	public LockAcknowledge(TimeStamp ts) {
		this.ts = ts;
	}
	
	public TimeStamp getTimeStamp() {
		return ts;
	}
}
