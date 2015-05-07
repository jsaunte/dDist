import java.io.Serializable;

/**
 * 
 * @author Hjortehandlerne
 * The TimeStamps are issued by a LamportClock, on a given event. 
 * The TimeStamp has the time of the LamportClock when it was issued, and the ID of the user who issued the timestamp.
 * The TimeStamp is comparable, so that the PriorityQueue may sort Events according to their timestamps.
 */

public class TimeStamp implements Serializable, Comparable<TimeStamp> {
	private static final long serialVersionUID = 8381138328320766514L;
	private int counter, id;
	
	public TimeStamp(int counter, int id) {
		this.counter = counter;
		this.id = id;
	}
	
	public int getTime() {
		return counter;
	}
	
	public int getID() {
		return id;
	}

	@Override
	public int compareTo(TimeStamp other) {
		if(counter == other.counter) {
			return id - other.id;
		}
		return counter - other.counter;
	}
	
	public boolean equals(Object other) {
		if(other == null) return false;
		if(this == other) return true;
		if(this.getClass() != other.getClass()) return false;
		TimeStamp t = (TimeStamp) other;
		if(counter == t.counter && id == t.id) return true;
		return false;
	}
	
	public int hashCode() {
		return (31*id) + (31*counter);
	}
}