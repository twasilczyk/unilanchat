package tools;

import java.util.regex.*;
import javax.swing.JOptionPane;

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
	 * i &quot;
	 * 
	 * @param text Tekst do przekonwertowania
	 * @return Tekst bezpieczny do umieszczenia w kodzie HTML
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
	 * @param text Tekst do przetworzenia
	 * @return Tekst z dodanymi ciągami &lt;br /&gt; w miejsce końców linii
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
	 * @todo Funkcja nie jest ściśle związana z HTML, przydało by się ją
	 * wydzielić do osobnej klasy
	 * @param strings Tablica ciągów do złączenia
	 * @param separator Separator
	 * @return Połączony ciąg rozdzielony separatorami
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
	 * Lista używanych domen najwyższego rzędu, potrzebna przy wykrywaniu URL.
	 */
	protected static final String[] tlds = {
		"com", "net", "org", "info", "biz", "edu", "gov",
		"pl", "eu", "de", "uk", "us", "ru", "cn", "jp", "cz", "sk",
		"to"
		};

	/**
	 * Znaki, które mogą się znaleźć w dalszej (poza domeną) części URL.
	 */
	protected static final String urlAdditionalChars =
			"ęóąśłżźćńĘÓĄŚŁŻŹĆŃ" +
			"#&\\-:;=?\\+\\%/\\.";

	/**
	 * Wyrażenie regularne znajdujące adresy URL.
	 */
	protected static final Pattern tagURLsPattern =
			Pattern.compile("(https?://)?"+
			"[a-zA-Z_0-9\\-]+(\\.\\w[a-zA-Z_0-9\\-]+)*\\.(" + join(tlds, "|") + ")" +
			"(/[\\w" + urlAdditionalChars + "]*)?",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Znajduje wszystkie adresy URL w tekście i zamienia je na łącza.
	 *
	 * @param text Tekst zawierający adresy
	 * @return Tekst z adresami zamienionymi w łącza
	 */
	public static String tagURLs(String text)
	{
		if (text == null)
			throw new NullPointerException();

		StringBuffer result = new StringBuffer(text.length());
		Matcher matcher = tagURLsPattern.matcher(text);
		
		while (matcher.find())
		{
			String link = matcher.group();
			String linkLowerCase = link.toLowerCase();

			if (link.startsWith("@")) //ignorujemy adresy email
				continue;

			String url;
			if (!linkLowerCase.startsWith("http://") &&
				!linkLowerCase.startsWith("https://"))
				url = "http://" + link;
			else
				url = link;

			matcher.appendReplacement(result, "<a href=\"" + url + "\">" + link + "</a>");
		}
		matcher.appendTail(result);
		return result.toString();
	}

	static final String[] browsers = {
		"firefox", "opera", "konqueror", "epiphany",
		"seamonkey", "galeon", "kazehakase", "mozilla", "netscape"
	};

	/**
	 * Otwiera podany URL w przeglądarce zainstalowanej w systemie użytkownika.
	 *
	 * Bare Bones Browser Launch 
	 * Public Domain Software -- Free to Use as You Like
	 *
	 * @author Dem Pilafian
	 * @version 2.0 (May 26, 2009)
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
					throw new RuntimeException();
			}
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null,
			"Error attempting to launch web browser\n" + e.toString());
		}
	}
}
