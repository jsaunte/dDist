import java.io.Serializable;

public interface TextEvent extends Serializable, Comparable<TextEvent> {
	void doEvent(final DistributedTextEditor editor, final int pos);
	
	TimeStamp getTimeStamp();
	
	int getOffset();
	
	void setOffset(int value);
	
	int getLength();
}
