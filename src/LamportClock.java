
/**
 * 
 * @author Hjortehandlerne
 *	The LamportClock serves as a logical clock for a Distributed Text Editor.
 *	The LamportClock increments it's time whenever an textevent has occured.
 *	It issues timestamps whenever asked for. 
 */
public class LamportClock  {
	private int counter;
	private int id;
	
	public LamportClock(int id) {
		this.id = id;
	}
	
	public int getTime() {
		return counter;
	}
	
	public int getID() {
		return id;
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
}