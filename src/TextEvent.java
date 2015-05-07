import java.io.Serializable;

public interface TextEvent extends Serializable, Comparable<TextEvent> {
	/**
	 * 
	 * @param editor - the editor on which we wish to remove the filter, before doing an event, and reinstating the filter.
	 * @param pos - the position where the event should be executed.
	 * The doEvent method executes an event, and ensures that the program wont loop infintely, by removing the filter, and reinstating after the update.
	 */
	void doEvent(final DistributedTextEditor editor, final int pos);
	
	TimeStamp getTimeStamp();

	int getLength();
}
