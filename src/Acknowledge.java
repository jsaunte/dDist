import java.io.Serializable;


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
