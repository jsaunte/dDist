public class LamportClock implements Comparable<LamportClock> {
	private int counter;
	private int id;
	
	public LamportClock(int id) {
		this.id = id;
	}
	
	public int getTime() {
		return counter;
	}
	
	public TimeStamp getTimeStamp() {
		return new TimeStamp(counter, id);
	}
	
	public void increment() {
		counter++;
	}
	
	public void setMaxTime(TimeStamp other) {
		counter = Math.max(counter, other.getTime());
	}
	
	@Override
	public int compareTo(LamportClock other) {
		if(counter == other.counter) {
			return id - other.id;
		}
		return counter - other.counter;
	}	
}