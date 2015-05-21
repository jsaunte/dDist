import java.io.Serializable;


public class NewPeerDataAcknowledgement implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3536627244958245512L;
	private TimeStamp ts;
	
	public NewPeerDataAcknowledgement(TimeStamp timeStamp) {
		ts = timeStamp;
	}

	public TimeStamp getTimeStamp() {
		return ts;
	}
}
