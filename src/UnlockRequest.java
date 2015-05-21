import java.io.Serializable;


public class UnlockRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 221793521202811473L;
	private TimeStamp ts;
	
	public UnlockRequest(TimeStamp ts) {
		this.ts = ts;
	}

	public TimeStamp getTs() {
		return ts;
	}
}
