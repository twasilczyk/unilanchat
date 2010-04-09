package net;

import java.net.*;

import tools.CachedDataProvider;

/**
 * Klasa dostarczająca nazwy hostów.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class NameService
{
	private final static LocalHostNameProvider localHostNameProvider =
		new LocalHostNameProvider();

	protected final static String defaultHostName = "localhost";

	protected static String localHostName = defaultHostName;

	static class LocalHostNameProvider extends CachedDataProvider
	{
		public LocalHostNameProvider()
		{
			super("LocalHostNameProvider");
		}

		@Override protected void loadData()
		{
			try
			{
				localHostName = InetAddress.getLocalHost().getHostName();
			}
			catch (UnknownHostException ex)
			{
				localHostName = defaultHostName;
			}
		}
	}

	/**
	 * Zwraca nazwę hosta komputera, na którym jest uruchomiona aplikacja.
	 *
	 * @return nazwa hosta
	 */
	public static String getLocalHostName()
	{
		localHostNameProvider.waitForData();

		return localHostName;
	}
}
