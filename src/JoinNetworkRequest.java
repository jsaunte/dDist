import java.io.Serializable;


public class JoinNetworkRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2128633945717953545L;
	private int port;
	
	public JoinNetworkRequest(int port) {
		this.port = port;
	}
	
	public int getPort() {
		return port;
	}
}
