import java.io.Serializable;


public class PeerWrapper implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2230700558582030063L;
	private int id, port;
	private String ip;
	
	public PeerWrapper(int id, int port, String ip) {
		this.id = id;
		this.port = port;
		this.ip = ip;
	}

	public int getId() {
		return id;
	}

	public int getPort() {
		return port;
	}

	public String getIP() {
		return ip;
	}
}
