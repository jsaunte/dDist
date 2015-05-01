import java.awt.EventQueue;

import javax.swing.text.DocumentFilter;

/**
 * 
 * @author Jesper Buus Nielsen
 *
 */
public class TextInsertEvent implements TextEvent /*extends MyTextEvent*/ {

	private String text;
	private int offset;
	
	public TextInsertEvent(int offset, String text) {
		this.offset = offset;
		this.text = text;
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
}

