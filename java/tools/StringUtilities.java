package tools;

/**
 * Klasa pomocnicza przy pracy ze Stringami.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class StringUtilities
{
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

	protected final static String[] sizeSuffixes = {
		"B", "kB", "MB", "GB", "TB"
	};

	public static String formatFileSize(long size)
	{
		double sSize = size;
		int sizeSuffix = 0;
		while (sSize >= 1000 && sizeSuffix < sizeSuffixes.length - 1)
		{
			sSize /= 1000;
			sizeSuffix++;
		}

		if (sSize >= 100)
			return String.format("%.0f%s", sSize, sizeSuffixes[sizeSuffix]);
		else if (sSize >= 10)
			return String.format("%.1f%s", sSize, sizeSuffixes[sizeSuffix]);
		else
			return String.format("%.2f%s", sSize, sizeSuffixes[sizeSuffix]);
	}
}
