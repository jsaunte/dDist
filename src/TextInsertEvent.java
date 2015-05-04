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
	private LamportClock lc;
	
	public TextInsertEvent(int offset, String text, LamportClock lc) {
		this.offset = offset;
		this.text = text;
		this.lc = lc;
		lc.increment();
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
	public LamportClock getClock() {
		return lc;
	}
}

