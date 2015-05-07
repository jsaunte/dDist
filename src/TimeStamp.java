import java.io.Serializable;

public class TimeStamp implements Serializable, Comparable<TimeStamp> {
	/**
	 * 
	 */
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