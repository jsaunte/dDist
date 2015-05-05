import java.io.Serializable;

public class TimeStamp implements Comparable<TimeStamp>, Serializable {
	private int counter, id;
	
	public TimeStamp(int counter, int id) {
		this.counter = counter;
		this.id = id;
	}
	
	public int getTime() {
		return counter;
	}

	@Override
	public int compareTo(TimeStamp other) {
		if(counter == other.counter) {
			return id - other.id;
		}
		return counter - other.counter;
	}
}