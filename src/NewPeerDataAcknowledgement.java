import java.io.Serializable;


public class NewPeerDataAcknowledgement implements Serializable {
	private TimeStamp ts;
	
	public NewPeerDataAcknowledgement(TimeStamp timeStamp) {
		ts = timeStamp;
	}

	public TimeStamp getTimeStamp() {
		return ts;
	}
}
