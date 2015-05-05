import java.io.Serializable;
import java.util.Comparator;

public interface TextEvent extends Serializable, Comparator<TextEvent> {
	void doEvent(final DistributedTextEditor editor);
	
	TimeStamp getTimeStamp();
}
