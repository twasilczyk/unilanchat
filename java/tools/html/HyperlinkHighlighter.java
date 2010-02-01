package tools.html;

import java.awt.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

/**
 * Klasa implementująca funkcjonalność CSS a:hover
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class HyperlinkHighlighter
{
	protected final JTextComponent pane;
	protected final HTMLDocument doc;
	private final PaneListener paneListener = new PaneListener();
	protected Element highlightedHyperlink;
	protected final Style styleHover, styleNormal;

	public HyperlinkHighlighter(JTextComponent pane)
	{
		if (pane == null)
			throw new NullPointerException();

		this.pane = pane;
		this.doc = (HTMLDocument)pane.getDocument();
		pane.addMouseListener(paneListener);
		pane.addMouseMotionListener(paneListener);
		
		StyleContext ss = doc.getStyleSheet();
		Style styleHoverT = ss.getStyle("a:hover");
		if (styleHoverT == null)
			styleHoverT = ss.addStyle("a:hover", null);
		Style styleNormalT = ss.getStyle("a");
		if (styleNormalT == null)
			styleNormalT = ss.addStyle("a", null);
		this.styleHover = styleHoverT;
		this.styleNormal = styleNormalT;
	}

	class PaneListener implements MouseListener, MouseMotionListener
	{

		public void mouseClicked(MouseEvent arg0) { }
		public void mousePressed(MouseEvent arg0) { }
		public void mouseReleased(MouseEvent arg0) { }
		public void mouseEntered(MouseEvent arg0) { }
		public void mouseExited(MouseEvent arg0) { }
		public void mouseDragged(MouseEvent arg0) { }

		public void mouseMoved(MouseEvent e)
		{
			int pos = pane.viewToModel(e.getPoint());
			if (pos <= 0)
			{
				removeHyperlinkHighlight();
				return;
			}

			Element elem = doc.getCharacterElement(pos);
			if (elem == null)
			{
				removeHyperlinkHighlight();
				return;
			}

			if (elem.getAttributes().getAttribute(HTML.Tag.A) != null)
				highlightHyperlink(elem);
			else
				removeHyperlinkHighlight();
		}

		private synchronized void removeHyperlinkHighlight()
		{
			if (highlightedHyperlink == null)
				return;
			changeHightlightStyle(highlightedHyperlink, false);
			highlightedHyperlink = null;
		}

		private synchronized void highlightHyperlink(Element hyperlinkElement)
		{
			if (hyperlinkElement == null)
				throw new NullPointerException();
			if (hyperlinkElement != highlightedHyperlink)
			{
				removeHyperlinkHighlight();
				changeHightlightStyle(hyperlinkElement, true);
				highlightedHyperlink = hyperlinkElement;
			}
		}

		private void changeHightlightStyle(Element el, boolean highlight)
		{
			if (el == null)
				throw new NullPointerException();
			int start = el.getStartOffset();
			int end = el.getEndOffset();
			//TODO: może da się to zrobić na elemencie, a nie znakach?
			doc.setCharacterAttributes(start, end - start,
					highlight?styleHover:styleNormal, false);
		}
	}
}
