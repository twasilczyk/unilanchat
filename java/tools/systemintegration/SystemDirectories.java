package tools.systemintegration;

import java.io.File;

/**
 * Klasa do zarządzania katalogami w systemie.
 * 
 * @author Piotr Gajowiak
 */
public class SystemDirectories
{
	/**
	 * Zwraca ścieżkę domyślnego katalogu aplikacji.
	 *
	 * @return domyślny katalog aplikacji w systemie
	 */
	public static String getAppStoreDir(String appName, boolean createIfNotExists)
	{
		if (System.getProperty("os.name").contains("Windows"))
		{
			Object r = WindowsRegistry.readValue(
				"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders",
				"AppData");

			if (r instanceof String && (new File((String)r)).isDirectory())
			{
				String s = (String)r + File.separator + appName;
				File file = new File(s);
				if ((new File(s)).isDirectory())
					return s + File.separator;
				if (!file.exists() && createIfNotExists)
				{
					
					if(file.mkdir())
						return s + File.separator;
					throw new RuntimeException("Nie udało się utworzyć katalogu aplikacji");
				}
			}
 			if (!(new File(System.getProperty("user.dir")).exists()))
				throw new RuntimeException("Katalog uruchomionej aplikacji nie istnieje");
			return System.getProperty("user.dir") + File.separator;
			
		}
		if (System.getProperty("user.home") != null && (new File(System.getProperty("user.home")).isDirectory()))
		{
			String s = System.getProperty("user.home") + File.separator + "." + appName;
			if ((new File(s)).isDirectory())
				return s + File.separator;
			if (!(new File(s)).exists() && createIfNotExists)
			{
				File file = new File(s);
				if (file.mkdir())
					return s + File.separator;
				throw new RuntimeException("Nie udało się utworzyć katalogu aplikacji");
			}
		}
		if (!(new File(System.getProperty("user.dir")).isDirectory()))
			throw new RuntimeException("Katalog uruchomionej aplikacji nie istnieje");
		return System.getProperty("user.dir") + File.separator;
	}
}
