package views.swing;

import java.awt.*;
import javax.swing.JPanel;

/**
 * Komponent wyświetlający obrazek
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class JImagePanel extends JPanel
{
	public Image image = null;

	public JImagePanel()
	{
		this.setOpaque(false);
		this.setBackground(Color.WHITE);
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
