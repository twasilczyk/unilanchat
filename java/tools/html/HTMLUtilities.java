package tools.html;

import java.io.*;
import java.net.URL;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import tools.GUIUtilities;

/**
 * Klasa pomocnicza przy pracy z HTML.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class HTMLUtilities
{
	private HTMLUtilities() { }

	/**
	 * Konwertuje znaki specjalne HTML na ich encje. Zamienia &amp;, &lt;, &gt;
	 * i &quot;.
	 * 
	 * @param text tekst do przekonwertowania
	 * @return tekst bezpieczny, do umieszczenia w kodzie HTML
	 */
	public static String escape(String text)
	{
		if (text == null)
			throw new NullPointerException();
		return text.replace("&", "&amp;").replace("<", "&lt;").
				replace(">", "&gt;").replace("\"", "&quot;");
	}

	/**
	 * Zamienia wszystkie znaki końca linii na przełamanie wiersza w HTML.
	 *
	 * @param text tekst do przetworzenia
	 * @return tekst z dodanymi ciągami &lt;br /&gt; w miejsce końców linii
	 */
	public static String nl2br(String text)
	{
		if (text == null)
			throw new NullPointerException();
		return text.replace("\n", "<br />");
	}

	/**
	 * Łączy ciągi podanym separatorem.
	 *
	 * @todo funkcja nie jest ściśle związana z HTML, przydało by się ją
	 * wydzielić do osobnej klasy
	 * @param strings tablica ciągów do złączenia
	 * @param separator separator
	 * @return połączony ciąg, rozdzielony separatorami
	 */
	public static String join(String[] strings, String separator)
	{
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < strings.length; i++)
		{
			if (i > 0)
				sb.append(separator);
			sb.append(strings[i]);
		}
		return sb.toString();
	}

	/**
	 * Wyrażenie regularne znajdujące adresy URL.
	 */
	protected static final Pattern tagURLsPattern =
			Pattern.compile(
			"(\\s[(\\[<{]?)((https?://|ftp://|www\\.)\\S+?)([)\\]>}.:,;]?\\s)",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	/**
	 * Znajduje wszystkie adresy URL w tekście i zamienia je na łącza.
	 *
	 * @param text tekst zawierający adresy
	 * @return tekst z adresami zamienionymi w łącza
	 */
	public static String tagURLs(String text)
	{
		if (text == null)
			throw new NullPointerException();

		StringBuffer result = new StringBuffer(text.length());
		Matcher matcher = tagURLsPattern.matcher(" " + text.replaceAll(" ", "  ") + " ");
		
		while (matcher.find())
		{
			String link = matcher.group(2);
			String preLink = matcher.group(1);
			String postLink = matcher.group(4);
			String linkLowerCase = link.toLowerCase();

			String url;
			if (!linkLowerCase.startsWith("http://") &&
				!linkLowerCase.startsWith("https://") &&
				!linkLowerCase.startsWith("ftp://"))
				url = "http://" + link;
			else
				url = link;

			matcher.appendReplacement(result,
					preLink + "<a href=\"" + url + "\">" + link + "</a>" + postLink);
		}
		matcher.appendTail(result);
		return result.toString().trim().replaceAll("  ", " ");
	}

	static final String[] browsers = {
		"firefox", "opera", "konqueror", "epiphany",
		"seamonkey", "galeon", "kazehakase", "mozilla", "netscape"
	};

	/**
	 * Otwiera podany URL w przeglądarce zainstalowanej w systemie użytkownika.
	 *
	 * Bare Bones Browser Launch;
	 * Public Domain Software - Free to Use as You Like;
	 * Autor: Dem Pilafian;
	 * Wersja: 2.0 (May 26, 2009).
	 *
	 * @param url URL do otworzenia w przeglądarce
	 */
	public static void openURL(String url)
	{
		String osName = System.getProperty("os.name");
		try
		{
			if (osName.startsWith("Mac OS"))
			{
				Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
				java.lang.reflect.Method openURL = fileMgr.getDeclaredMethod("openURL",
					new Class[] {String.class});
				openURL.invoke(null, new Object[] {url});
			}
			else if (osName.startsWith("Windows"))
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
			else
			{ //prawdopodobnie linux / unix
				boolean found = false;
				for (String browser : browsers)
				{
					found = Runtime.getRuntime().exec(
						new String[]{"which", browser}).waitFor() == 0;
					if (found)
					{
						Runtime.getRuntime().exec(new String[] {browser, url});
						break;
					}
				}
				if (!found)
					throw new RuntimeException("Nie znaleziono żadnej działającej przeglądarki");
			}
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null,
			"Error attempting to launch web browser\n" + e.toString());
		}
	}

	/**
	 * Wczytuje plik css z URL do podanego arkusza styli.
	 *
	 * @param ss arkusz, do którego ma zostać wczytany plik
	 * @param sheet URL, który ma zostać wczytany
	 */
	public static void loadCSSRules(StyleSheet ss, URL sheet)
	{
		try
		{
			ss.loadRules(new InputStreamReader(sheet.openStream()), null);
		}
		catch (IOException e)
		{
		}
	}

	/**
	 * Umieszcza kod HTML przed końcem podanego elementu.
	 *
	 * @param elem element
	 * @param html kod HTML
	 */
	public static void insertBeforeEnd(final Element elem, final String html)
	{
		GUIUtilities.swingInvokeAndWait(new Runnable()
		{
			public void run()
			{
				try
				{
					HTMLDocument doc = (HTMLDocument)elem.getDocument();

					if (elem.getElementCount() == 1 &&
						elem.getElement(0).getName().equals("p-implied") &&
						elem.getElement(0).getElementCount() <= 1 &&
						elem.getElement(0).getElement(0).getEndOffset() <= 2)
						doc.setInnerHTML(elem, html);
					else
						doc.insertBeforeEnd(elem, html);
				}
				catch (BadLocationException ex)
				{
					throw new RuntimeException(ex);
				}
				catch (IOException ex)
				{
					throw new RuntimeException(ex);
				}
			}
		});
	}

	/**
	 * Umieszcza kod HTML po końcu podanego elementu.
	 *
	 * @param elem element
	 * @param html kod HTML
	 */
	public static void insertAfterEnd(final Element elem, final String html)
	{
		GUIUtilities.swingInvokeAndWait(new Runnable()
		{
			public void run()
			{
				try
				{
					HTMLDocument doc = (HTMLDocument)elem.getDocument();
					doc.insertAfterEnd(elem, html);
				}
				catch (BadLocationException ex)
				{
					throw new RuntimeException(ex);
				}
				catch (IOException ex)
				{
					throw new RuntimeException(ex);
				}
			}
		});
	}

	/**
	 * Ustawia nową zawartość (HTML) podanego elementu.
	 *
	 * @param elem element
	 * @param html kod HTML
	 * @param waitForSwing czy bieżący wątek ma czekać na wykonanie podmiany
	 */
	public static void setInnerHTML(final Element elem, final String html,
			boolean waitForSwing)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				try
				{
					HTMLDocument doc = (HTMLDocument)elem.getDocument();
					doc.setInnerHTML(elem, html);
				}
				catch (BadLocationException ex)
				{
					throw new RuntimeException(ex);
				}
				catch (IOException ex)
				{
					throw new RuntimeException(ex);
				}
			}
		};

		if (waitForSwing)
			GUIUtilities.swingInvokeAndWait(r);
		else
			SwingUtilities.invokeLater(r);
	}

	/**
	 * Usuwa podany element z dokumentu.
	 *
	 * @param elem element
	 */
	public static void remove(final Element elem)
	{
		GUIUtilities.swingInvokeAndWait(new Runnable()
		{
			public void run()
			{
				try
				{
					HTMLDocument doc = (HTMLDocument)elem.getDocument();

					int offset = elem.getStartOffset();
					int len = elem.getEndOffset() - offset;
					doc.remove(offset, len);
				}
				catch (BadLocationException ex)
				{
					throw new RuntimeException(ex);
				}
			}
		});
	}
}
