import java.util.Comparator;


public class TextEventComparator implements Comparator<TextEvent>{

	@Override
	public int compare(TextEvent o1, TextEvent o2) {
		return o1.getTimeStamp().compareTo(o2.getTimeStamp());
	}

}
