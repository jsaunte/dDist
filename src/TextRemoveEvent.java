import java.awt.EventQueue;

import javax.swing.text.DocumentFilter;


public class TextRemoveEvent implements TextEvent /*extends MyTextEvent*/ {

	private int length;
	private int offset;
	
	public TextRemoveEvent(int offset, int length) {
		this.offset = offset;
		this.length = length;
	}
	
	public int getLength() { return length; }

	@Override
	public void doEvent(final DistributedTextEditor editor) {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				DocumentFilter filter = editor.getDocumentFilter();
				editor.setDocumentFilter(null);
				editor.getTextArea().replaceRange(null, offset, offset + length);
				editor.setDocumentFilter(filter);
			}
			
		});
	}
}
