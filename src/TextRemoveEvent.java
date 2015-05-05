import java.awt.EventQueue;

import javax.swing.text.DocumentFilter;

public class TextRemoveEvent implements TextEvent {

	private int length;
	private int offset;
	private TimeStamp ts;
	
	public TextRemoveEvent(int offset, int length, TimeStamp ts) {
		this.offset = offset;
		this.length = length;
		this.ts = ts;
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

	@Override
	public TimeStamp getTimeStamp() {
		return ts;
	}

	@Override
	public int compareTo(TextEvent other) {
		return ts.compareTo(other.getTimeStamp());
	}
}