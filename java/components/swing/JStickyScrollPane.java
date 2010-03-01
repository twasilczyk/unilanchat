package components.swing;

import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;

/**
 * Komponent przewijalnego pola dla obiektów o zmiennym rozmiarze. Jeżeli
 * opakowywany obiekt zmieni rozmiar, a pole było przewinięte do samego dołu,
 * po ewentualnym wydłużeniu obiektu pole zostanie przewinięte na koniec.
 * Jeżeli pasek przewijania nie był na samym dole, poziom przewinięcia się nie
 * zmieni.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class JStickyScrollPane extends JScrollPane
{
	/**
	 * Czy pionowy suwak jest "przyklejony" do dolnej krawędzi.
	 */
	protected boolean verticalIsSticky = true;

	private final ScrollBarListener verticalScrollBarListener = new ScrollBarListener();

	/**
	 * Nowe pole, bez ustalonego komponentu z zawartością.
	 *
	 * @see #setViewportView(Component)
	 */
	public JStickyScrollPane()
	{
		this.getVerticalScrollBar().addAdjustmentListener(verticalScrollBarListener);
	}

	/**
	 * Nowe pole, z ustalonym komponentem z zawartością.
	 *
	 * @param c zawartość
	 */
	public JStickyScrollPane(Component c)
	{
		this();
		this.setViewportView(c);
	}
	
	class ScrollBarListener implements AdjustmentListener
	{
		int viewHeight, paneHeight;

		public void adjustmentValueChanged(AdjustmentEvent e)
		{
			Component view = getViewport().getView();
			if (view == null)
			{
				viewHeight = 0;
				verticalIsSticky = true;
				return;
			}

			JScrollBar vScroll = getVerticalScrollBar();

			int newViewHeight = view.getHeight();
			int newPaneHeight = getHeight();

			boolean atEnd = vScroll.getValue() >= vScroll.getMaximum() -
						vScroll.getVisibleAmount();

			if (viewHeight == newViewHeight && paneHeight == newPaneHeight)
			// user przewinął suwak
				verticalIsSticky = atEnd;
			else
			{ // zmienił się rozmiar komponentu widoku, lub pola przewijanego
				if (verticalIsSticky)
				{
					if (!atEnd)
						vScroll.setValue(vScroll.getMaximum());
				}
				else
					verticalIsSticky = atEnd;
			}

			viewHeight = newViewHeight;
			paneHeight = newPaneHeight;
		}
	}
}
