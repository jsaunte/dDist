import java.io.Serializable;

/**
 * 
 * @author Hjortehandlerne
 * The Acknowledgement-class is used by peers, to tell the sender of an event that the event is received, and that he may do the event. 
 */

public class Acknowledge implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5526031236266334635L;
	private TextEvent e;
	
	public Acknowledge(TextEvent e) {
		this.e = e;
	}
	
	public TextEvent getEvent() {
		return e;
	}
}
