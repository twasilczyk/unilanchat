package tools.systemintegration;

import java.io.IOException;
import javax.swing.JOptionPane;

import tools.WholeStreamReader;

/**
 * Klasa do zarządzania procesami systemowymi.
 * 
 * @author Piotr Gajowiak
 */
public class SystemProcesses
{
	/**
	 * Wykonuje polecenie zadane w parametrze i zwraca jego wynik.
	 * 
	 * @param command polecenie do wykonania
	 * @return wynik działania polecenia
	 */
	public static String systemExec(String command)
	{
		try
		{
			Process process = Runtime.getRuntime().exec(command);
			WholeStreamReader reader = new WholeStreamReader(process.getInputStream());
			reader.start();
			process.waitFor();
			reader.join();
			return reader.getResult();
		}
		catch (IOException ex)
		{
			throw new RuntimeException("Nie udało się uruchomić polecenia: " + command, ex);
		}
		catch (InterruptedException ex)
		{
			throw new RuntimeException("Nie udało się uruchomić polecenia: " + command, ex);
		}
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
}
