import java.io.Serializable;


public interface TextEvent extends Serializable {
	void doEvent(final DistributedTextEditor editor);
	
	LamportClock getClock();
}
