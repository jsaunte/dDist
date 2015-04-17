import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.LinkedBlockingQueue;


public class QueueRMIImpl extends UnicastRemoteObject implements QueueRMI {
	
	private LinkedBlockingQueue<MyTextEvent> queue;
	
	protected QueueRMIImpl() throws RemoteException {
		super();
		queue = new LinkedBlockingQueue<MyTextEvent>();		
	}

	@Override
	public LinkedBlockingQueue<MyTextEvent> getQueue() {
		return queue;
	}

}
