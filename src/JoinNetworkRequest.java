import java.io.Serializable;


public class JoinNetworkRequest implements Serializable {
	private int port;
	
	public JoinNetworkRequest(int port) {
		this.port = port;
	}
	
	public int getPort() {
		return port;
	}
}
