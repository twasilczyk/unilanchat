package tools.html;

import java.awt.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

/**
 * Klasa implementująca funkcjonalność CSS <code>a:hover</code> oraz
 * wyświetlająca tooltip <code>alt</code> tagu <code>img</code> (w implementacji
 * DOM w javie mógłby być problem z <code>title</code>).
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class HyperlinkHighlighter
{
	/**
	 * Komponent, którego zawartość ma być podświetlana.
	 */
	protected final JTextComponent pane;

	/**
	 * Dokument HTML, związany z komponentem.
	 *
	 * @see #pane
	 */
	protected final HTMLDocument doc;

	private final PaneListener paneListener = new PaneListener();

	/**
	 * Aktualnie podświetlony link.
	 */
	protected Element highlightedHyperlink;

	/**
	 * Styl dla linków nie podświetlonych. Musi nadpisywać parametry ze stylu
	 * dla linków podświetlonych, aby po usunięciu podświetlenia link powrócił
	 * do poprzedniego wyglądu.
	 */
	protected final Style styleNormal;

	/**
	 * Styl dla linków podświetlonych
	 */
	protected final Style styleHover;

	/**
	 * Reguły pobrane z arkusza styli.
	 */
	protected final StyleContext cssRules;

	/**
	 * Główny konstruktor.
	 *
	 * @param pane komponent, którego zawartość ma być podświetlana
	 */
	public HyperlinkHighlighter(JTextComponent pane)
	{
		if (pane == null)
			throw new NullPointerException();

		this.pane = pane;
		this.doc = (HTMLDocument)pane.getDocument();
		pane.addMouseListener(paneListener);
		pane.addMouseMotionListener(paneListener);
		
		cssRules = doc.getStyleSheet();
		Style styleHoverT = cssRules.getStyle("a:hover");
		if (styleHoverT == null)
			styleHoverT = cssRules.addStyle("a:hover", null);
		Style styleNormalT = cssRules.getStyle("a");
		if (styleNormalT == null)
			styleNormalT = cssRules.addStyle("a", null);
		this.styleHover = styleHoverT;
		this.styleNormal = styleNormalT;
	}

	class PaneListener implements MouseListener, MouseMotionListener
	{
		public void mouseClicked(MouseEvent e) { }
		public void mousePressed(MouseEvent e) { }
		public void mouseReleased(MouseEvent e) { }
		public void mouseDragged(MouseEvent e) { }

		public void mouseEntered(MouseEvent e)
		{
			mouseMoved(e);
		}

		public void mouseExited(MouseEvent e)
		{
			removeHyperlinkHighlight();
		}

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

			AttributeSet elemAttr = elem.getAttributes();

			boolean isImage = elemAttr.getAttribute(StyleConstants.NameAttribute).
				equals(HTML.Tag.IMG);
			Object imageAltAttrib = elemAttr.getAttribute(HTML.Attribute.ALT);

			if (isImage && imageAltAttrib != null)
				pane.setToolTipText(imageAltAttrib.toString());
			else
				pane.setToolTipText(null);

			if (!isImage && elemAttr.getAttribute(HTML.Tag.A) != null)
				highlightHyperlink(elem);
			else
				removeHyperlinkHighlight();
		}

		/**
		 * Usuwa styl z podświetlonego linku.
		 */
		private synchronized void removeHyperlinkHighlight()
		{
			if (highlightedHyperlink == null)
				return;
			changeHightlightStyle(highlightedHyperlink, false);
			highlightedHyperlink = null;
		}

		/**
		 * Ustawia podświetlenie na wybrany link.
		 * 
		 * @param hyperlinkElement Link do podświetlenia
		 */
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

		/**
		 * Zmienia styl podświetlenia wybranego linku.
		 *
		 * @param el Link do zmiany stylu
		 * @param highlight Czy podświetlić
		 */
		private void changeHightlightStyle(Element el, boolean highlight)
		{
			if (el == null)
				throw new NullPointerException();
			int start = el.getStartOffset();
			int end = el.getEndOffset();

			Style newStyle;

			if (highlight)
			{
				newStyle = styleHover;
				for (String className : HTMLUtilities.getElementClassNames(el))
				{
					Style elStyle = cssRules.getStyle("." + className + ":hover");
					if (elStyle != null)
						newStyle = elStyle;
				}
			}
			else
			{
				newStyle = styleNormal;
				for (String className : HTMLUtilities.getElementClassNames(el))
				{
					Style elStyle = cssRules.getStyle("." + className);
					if (elStyle != null)
						newStyle = elStyle;
				}
			}

			doc.setCharacterAttributes(start, end - start, newStyle, false);
		}
	}
}
