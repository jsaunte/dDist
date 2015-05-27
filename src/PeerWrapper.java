import java.io.Serializable;


/**
 * 
 * @author Hjortehandlerne
 * 	The PeerWrapper is used by ConnectionData to send relevant information about a peer, so that an incomming peer can connect to other peers in the network.
 *  The PeerWrapper is necessary since the Peer class contains objects which can not be sent over a socket, due to lack of serialization.
 *
 */
public class PeerWrapper implements Serializable {
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
