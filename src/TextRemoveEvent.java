import java.awt.EventQueue;

import javax.swing.text.DocumentFilter;


public class TextRemoveEvent implements TextEvent{

	private int length;
	private int offset;
	private LamportClock lc;
	
	public TextRemoveEvent(int offset, int length, LamportClock lc) {
		this.offset = offset;
		this.length = length;
		this.lc = lc;
		lc.increment();
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
	public LamportClock getClock() {
		return lc;
	}
}
