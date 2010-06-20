package components.swing;

import java.awt.*;
import javax.swing.JPanel;

import tools.GUIUtilities;

/**
 * Komponent wyświetlający obrazek.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class JImagePanel extends JPanel
{
	/**
	 * Obrazek wyświetlany na komponencie.
	 */
	private Image image = null;

	/**
	 * Główny konstruktor, przedstawiający pusty panel (bez wyświetlanego
	 * obrazka).
	 */
	public JImagePanel()
	{
		this.setOpaque(false);
		this.setBackground(Color.WHITE);
	}

	/**
	 * Konstuktor inicjujący panel wybranym obrazkiem.
	 * 
	 * @param image obrazek, który ma być wyświetlony na panelu, lub
	 * <code>null</code> jeżeli panel ma być pusty
	 */
	public JImagePanel(Image image)
	{
		this.setOpaque(false);
		this.setBackground(Color.WHITE);
		this.image = image;
	}

	/**
	 * Ustawia wybrany obrazek jako nowy, który ma być wyświetlany na panelu.
	 * Aby usunąć bieżący obrazek bez wyświetlania nowego, należy podać wartość
	 * <code>null</code>.
	 *
	 * @param image nowy obrazek do wyświetlenia, lub <code>null</code> aby usunąć
	 */
	public void setImage(Image image)
	{
		if (image == this.image)
			return;
		this.image = image;
		invalidate();
	}

	/**
	 * Ustawia (lub usuwa) wybrany obrazek jako nowy, oraz odświeża panel na
	 * którym jest wyświetlony.
	 *
	 * @param image nowy obrazek do wyświetlenia, lub <code>null</code> aby usunąć
	 * @see #setImage(java.awt.Image)
	 */
	public void setAndRefreshImage(Image image)
	{
		if (image == this.image)
			return;
		setImage(image);
		GUIUtilities.validateRoot(this);
	}

	@Override public void paintComponent(Graphics g)
	{
		if (isOpaque())
		{
			g.setColor(this.getBackground());
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
		}
		if (image != null && image.getWidth(null) > 0 && image.getHeight(null) > 0)
			g.drawImage(image,
					((this.getWidth() - image.getWidth(null)) / 2),
					((this.getHeight() - image.getHeight(null)) / 2), null);
	}
}
