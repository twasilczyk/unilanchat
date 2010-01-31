package resources;

import java.net.URL;
import java.util.HashMap;
import java.awt.*;
import java.awt.image.ImageObserver;
import javax.swing.ImageIcon;

/**
 * Klasa odpowiada za odczytywanie zasobów (np. grafiki) z pliku jar.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ResourceManager
{
	static HashMap<String, URL> urls = new HashMap<String, URL>();
	static HashMap<String, ImageIcon> icons = new HashMap<String, ImageIcon>();
	static HashMap<String, Image> images = new HashMap<String, Image>();

	/**
	 * Pobiera uchwyt do pliku dowolnego typu.
	 * 
	 * @param objname Nazwa pliku do pobrania
	 * @return URL pobranego pliku
	 */
	public static URL get(String objname)
	{
		if (objname == null)
			throw new NullPointerException();
		if (urls.containsKey(objname))
			return urls.get(objname);
		
		URL u = ResourceManager.class.getResource(objname);
		urls.put(objname, u); //unchecked
		return u;
	}

	/**
	 * Sprawdza, czy plik o podanej nazwie istnieje w zasobach.
	 *
	 * @param objname Nazwa pliku do sprawdzenia
	 * @return Czy plik istnieje w zasobach
	 */
	public static boolean exists(String objname)
	{
		if (objname == null)
			throw new NullPointerException();
		return (get(objname) != null);
	}

	/**
	 * Pobiera ikonę z zasobów.
	 *
	 * @param iconname Nazwa pliku z ikoną
	 * @return Obiekt obrazka ikony
	 */
	public static ImageIcon getIcon(String iconname)
	{
		if (iconname == null)
			throw new NullPointerException();
		if (icons.containsKey(iconname))
			return icons.get(iconname);
		
		URL u = get(iconname);
		if (u == null)
		{
			icons.put(iconname, null); //unchecked
			System.err.println("Ikona nie istnieje: " + iconname);
			return null;
		}
		
		ImageIcon i = new ImageIcon(u);
		icons.put(iconname, i); //unchecked
		return i;
	}

	/**
	 * Pobiera obrazek z zasobów.
	 *
	 * @param imagename Nazwa obrazka do pobrania
	 * @return Obiekt obrazka
	 */
	public static Image getImage(String imagename)
	{
		if (imagename == null)
			throw new NullPointerException();
		if (images.containsKey(imagename))
			return images.get(imagename);

		URL u = get(imagename);
		if (u == null)
		{
			images.put(imagename, null); //unchecked
			System.err.println("Ikona nie istnieje: " + imagename);
			return null;
		}

		Image i = Toolkit.getDefaultToolkit().getImage(u);

		int triesLimit = 10;
		while ((Toolkit.getDefaultToolkit().checkImage(i, -1, -1, null) & ImageObserver.ALLBITS) == 0
				&& triesLimit-- > 0)
		{
			try
			{
				//taki hack
				i.getWidth(null);
				i.getHeight(null);
				Thread.sleep(1);
			}
			catch (InterruptedException e)
			{
				return null;
			}
		}

		images.put(imagename, i); //unchecked

		return i;
	}
}
