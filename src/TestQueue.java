import java.util.concurrent.PriorityBlockingQueue;


public class TestQueue {
	private static PriorityBlockingQueue<TextEvent> queue;
	
	public static void main(String[] args) {
		queue = new PriorityBlockingQueue<TextEvent>();
		
		TextEvent e1 = new TextInsertEvent(0, "a", new TimeStamp(4, 1));
		TextEvent e2 = new TextInsertEvent(1, "b", new TimeStamp(2, 2));
		TextEvent e3 = new TextInsertEvent(2, "c", new TimeStamp(3, 1));
		TextEvent e4 = new TextInsertEvent(3, "d", new TimeStamp(0, 2));
		TextEvent e5 = new TextInsertEvent(4, "e", new TimeStamp(1, 1));
		
		queue.add(e1);
		queue.add(e2);
		queue.add(e3);
		queue.add(e4);
		queue.add(e5);
		
		for(int i = 0; i < 5; i++) {
			try {
				System.out.println("TimeStamp: " + queue.take().getTimeStamp().getTime());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
