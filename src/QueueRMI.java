import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.LinkedBlockingQueue;


public interface QueueRMI extends Remote {
	
	LinkedBlockingQueue<MyTextEvent> getQueue() throws RemoteException;
}
