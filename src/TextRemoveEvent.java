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
	public void doEvent(final DistributedTextEditor editor, final int pos) {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				DocumentFilter filter = editor.getDocumentFilter();
				editor.setDocumentFilter(null);
				editor.getTextArea().replaceRange(null, pos - length, pos);
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