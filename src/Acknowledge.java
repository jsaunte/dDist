import java.io.Serializable;


public class Acknowledge implements Serializable{
	private TextEvent e;
	
	public Acknowledge(TextEvent e) {
		this.e = e;
	}
	
	public TextEvent getEvent() {
		return e;
	}
}
