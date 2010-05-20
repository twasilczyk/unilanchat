package tools;

import java.awt.*;
import java.awt.image.*;

/**
 * Filtr usuwający z obrazków kanał alpha, zastępujący przezroczyste obszary
 * podanym kolorem.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class RemoveAlphaImageFilter extends RGBImageFilter
{
	protected final int background;
	protected final int backgroundRed;
	protected final int backgroundGreen;
	protected final int backgroundBlue;

	public RemoveAlphaImageFilter(int background)
	{
		background &= 0x00FFFFFF;
		background |= 0xFF000000;

		this.background = background;
		this.backgroundRed = ((background & 0x00FF0000) >> 16) & 0xFF;
		this.backgroundGreen = ((background & 0x0000FF00) >> 8) & 0xFF;
		this.backgroundBlue = ((background & 0x000000FF) >> 0) & 0xFF;
	}

	@Override public int filterRGB(int x, int y, int rgb)
	{
		int alpha = ((rgb & 0xFF000000) >> 24) & 0xFF;
		rgb |= 0xFF000000;

		if (alpha == 0x00)
			return background;
		else if (alpha == 0xFF)
			return rgb;
		else
		{
			int r = ((rgb & 0x00FF0000) >> 16) & 0xFF;
			int g = ((rgb & 0x0000FF00) >> 8)  & 0xFF;
			int b = ((rgb & 0x000000FF) >> 0)  & 0xFF;

			r = (r * alpha + backgroundRed * (0xFF - alpha)) / 0xFF;
			g = (g * alpha + backgroundGreen * (0xFF - alpha)) / 0xFF;
			b = (b * alpha + backgroundBlue * (0xFF - alpha)) / 0xFF;

			return 0xFF000000 | (r << 16) | (g << 8) | (b << 0);
		}
	}

	public static Image removeAlphaChannel(Image src, int background)
	{
		ImageFilter filter = new RemoveAlphaImageFilter(background);

		return Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(
			new FilteredImageSource(src.getSource(), filter),
			filter));
	}
}
