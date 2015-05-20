import java.io.Serializable;


public class NewPeerDataRequest implements Serializable {
	private int id, port, caretPos;
	
	public NewPeerDataRequest(int id, int port, int caretPos) {
		this.id = id;
		this.port = port;
		this.caretPos = caretPos;
	}
	
	public int getId() {
		return id;
	}
	
	public int getPort() {
		return port;
	}
	
	public int getCaretPos() {
		return caretPos;
	}
}
