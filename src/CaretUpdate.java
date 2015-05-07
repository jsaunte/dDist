import java.io.Serializable;


public class CaretUpdate implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1557292428356483699L;
	private int pos, id;
	
	public CaretUpdate(int pos, int id) {
		this.pos = pos;
		this.id = id;
	}
	
	public int getPos() {
		return pos;
	}
	
	public int getID() {
		return id;
	}
}
