import java.io.Serializable;

/**
 * 
 * @author Hjortehandlerne
 * The CaretUpdate is used to tell the other peer to update the caret-position for this id.
 */

public class CaretUpdate implements Serializable, TextEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1557292428356483699L;
	private int pos;
	private TimeStamp ts;
	
	public CaretUpdate(int pos, TimeStamp ts) {
		this.pos = pos;
		this.ts = ts;
	}
	
	public int getPos() {
		return pos;
	}
	
	@Override
	public int compareTo(TextEvent arg0) {
		return ts.compareTo(arg0.getTimeStamp());
	}

	@Override
	public void doEvent(DistributedTextEditor editor, int pos) {
		editor.getEventReplayer().updateCaretPos(ts.getID(), pos);
	}

	@Override
	public TimeStamp getTimeStamp() {
		return ts;
	}

	@Override
	public int getLength() {
		return 0;
	}
}
