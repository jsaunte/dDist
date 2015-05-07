import java.io.Serializable;


public class CaretUpdate implements Serializable {
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
