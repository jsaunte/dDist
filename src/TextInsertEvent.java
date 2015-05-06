import java.awt.EventQueue;

import javax.swing.text.DocumentFilter;

/**
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class TextInsertEvent implements TextEvent  {
	private String text;
	private int offset;
	private TimeStamp ts;
	
	public TextInsertEvent(int offset, String text, TimeStamp ts) {
		this.offset = offset;
		this.text = text;
		this.ts = ts;
	}
	public String getText() { return text; }
	
	@Override
	public void doEvent(final DistributedTextEditor editor) {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				DocumentFilter filter = editor.getDocumentFilter();
				editor.setDocumentFilter(null);
				editor.getTextArea().insert(text, offset);
				editor.setDocumentFilter(filter);
			}
			
		});
	}
	
	@Override
	public TimeStamp getTimeStamp() {
		return ts;
	}
	@Override
	public int compareTo(TextEvent other) {
		return ts.compareTo(other.getTimeStamp());
	}
	@Override
	public int getOffset() {
		return offset;
	}
	@Override
	public void setOffset(int value) {
		offset = value;
	}
}