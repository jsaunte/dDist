import java.awt.EventQueue;

import javax.swing.text.DocumentFilter;

public class TextRemoveEvent implements TextEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2690947405139638827L;
	private int length;
	private TimeStamp ts;
	
	public TextRemoveEvent(int length, TimeStamp ts) {
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
				try {
					editor.getTextArea().replaceRange(null, pos - length, pos);
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
}