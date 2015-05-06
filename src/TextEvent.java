import java.io.Serializable;

public interface TextEvent extends Serializable, Comparable<TextEvent> {
	void doEvent(final DistributedTextEditor editor);
	
	TimeStamp getTimeStamp();
	
	int getOffset();
	
	void setOffset(int value);
}
