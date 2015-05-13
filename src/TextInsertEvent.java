import java.awt.EventQueue;

import javax.swing.text.DocumentFilter;

/**
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class TextInsertEvent implements TextEvent  {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3677145398605145841L;
	private String text;
	private TimeStamp ts;
	
	public TextInsertEvent(String text, TimeStamp ts) {
		this.text = text;
		this.ts = ts;
	}
	public String getText() { return text; }
	
	@Override
	public void doEvent(final DistributedTextEditor editor, final int pos) {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				DocumentFilter filter = editor.getDocumentFilter();
				editor.setDocumentFilter(null);
				try {
					editor.getTextArea().insert(text, pos);
				} catch (IllegalArgumentException e) {
					
				}
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
	public int getLength() {
		return text.length();
	}
}