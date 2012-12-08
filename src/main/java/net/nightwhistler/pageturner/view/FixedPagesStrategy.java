package net.nightwhistler.pageturner.view;

import java.util.ArrayList;
import java.util.List;

import net.nightwhistler.pageturner.epub.PageTurnerSpine;

import android.graphics.Canvas;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout.Alignment;
import android.widget.TextView;

public class FixedPagesStrategy implements PageChangeStrategy {

	private Spanned text;
	
	private int pageNum;
	
	private List<Integer> pageOffsets = new ArrayList<Integer>();
	
	private BookView bookView;
	private TextView childView;
	
	private int storedPosition = -1;
	
	public FixedPagesStrategy(BookView bookView) {
		this.bookView = bookView;
		this.childView = bookView.getInnerView();
	}
	
	@Override
	public void clearStoredPosition() {
		this.pageNum = 0;
		this.storedPosition = 0;
	}
	
	@Override
	public void clearText() {
		this.text = new SpannedString("");
		this.childView.setText(text);
	}
	
	private void loadPages() {
		
		this.pageOffsets = new ArrayList<Integer>();
		
		TextPaint textPaint = childView.getPaint();
		int boundedWidth = childView.getWidth();

		StaticLayout layout = new StaticLayout(this.text, textPaint, boundedWidth , Alignment.ALIGN_NORMAL, 1.0f, bookView.getLineSpacing(), false);
		layout.draw(new Canvas());
		
		int pageHeight = bookView.getHeight() - ( 2 * bookView.getVerticalMargin());
		
		int totalLines = layout.getLineCount();
		int pageNum = 0;
		int topLine = 0;
		int bottomLine = 0;
		
		while ( bottomLine < totalLines -1 ) {
			topLine = layout.getLineForVertical( pageNum * pageHeight );
			bottomLine = layout.getLineForVertical( (pageNum + 1) * pageHeight );
			
			int pageOffset = layout.getLineStart(topLine);
			
			//Make sure we don't enter the same offset twice
			if (pageOffsets.isEmpty() ||  pageOffset != this.pageOffsets.get(this.pageOffsets.size() -1)) {			
				this.pageOffsets.add(pageOffset);
			}
			
			pageNum++;
		}		
		
	}
	
	
	@Override
	public void reset() {
		clearStoredPosition();
		this.pageOffsets.clear();
		clearText();
	}
	
	private void updateStoredPosition() {
		for ( int i=0; i < this.pageOffsets.size(); i++ ) {
			if ( this.pageOffsets.get(i) > this.storedPosition ) {
				this.pageNum = i -1;
				this.storedPosition = -1;
				return;
			}
		}
		
		this.pageNum = this.pageOffsets.size() - 1;
		this.storedPosition = -1;
	}
	
	@Override
	public void updatePosition() {
		
		if ( pageOffsets.isEmpty() || text.length() == 0) {
			return;
		}		
		
		if ( storedPosition != -1 ) {
			updateStoredPosition();
		}
		
		if ( this.pageNum >= pageOffsets.size() -1 ) {
			childView.setText( this.text.subSequence(pageOffsets.get(pageNum), text.length() ));
		} else {
			int start = this.pageOffsets.get(pageNum);
			int end = this.pageOffsets.get(pageNum +1 );
			childView.setText( this.text.subSequence(start, end));
		}		
	}
	
	@Override
	public void setPosition(int pos) {
		this.storedPosition = pos;
		updatePosition();
	}
	
	@Override
	public void setRelativePosition(double position) {
		
		int intPosition = (int) (this.text.length() * position);
		setPosition(intPosition);
		
	}
	
	public int getPosition() {
		return this.pageOffsets.get(this.pageNum);
	}
	
	public android.text.Spanned getText() {
		return text;
	}
	
	public boolean isAtEnd() {
		return pageNum == this.pageOffsets.size() - 1;
	}
	
	public boolean isAtStart() {
		return this.pageNum == 0;
	}
	
	public boolean isScrolling() {
		return false;
	}
	
	@Override
	public void pageDown() {
	
		if ( isAtEnd() ) {
			PageTurnerSpine spine = bookView.getSpine();
		
			if ( spine == null || ! spine.navigateForward() ) {
				return;
			}
			
			this.clearText();
			this.pageNum = 0;
			bookView.loadText();
			
		} else {
			this.pageNum = Math.min(pageNum +1, this.pageOffsets.size() -1 );
			updatePosition();
		}
	}
	
	@Override
	public void pageUp() {
	
		if ( isAtStart() ) {
			PageTurnerSpine spine = bookView.getSpine();
		
			if ( spine == null || ! spine.navigateBack() ) {
				return;
			}
			
			this.clearText();
			this.storedPosition = Integer.MAX_VALUE;
			this.bookView.loadText();
		} else {
			this.pageNum = Math.max(pageNum -1, 0);
			updatePosition();
		}
	}
	
	@Override
	public void loadText(Spanned text) {
		this.text = text;
		this.pageNum = 0;
		loadPages();
		updatePosition();
	}
}
