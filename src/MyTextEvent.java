import java.io.Serializable;

/**
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class MyTextEvent implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4003721487989963698L;
	MyTextEvent(int offset) {
		this.offset = offset;
	}
	private int offset;
	int getOffset() { return offset; }
}