package net;

import java.net.*;

/**
 * Klasa dostarczająca nazw hostów.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class NameService
{
	private final static LocalHostNameProvider localHostNameProvider =
		new LocalHostNameProvider();

	static class LocalHostNameProvider extends Thread
	{
		protected final static int refreshRate = 60000;
		public final static String defaultHostName = "localhost";
		
		public boolean isLocalHostNameReady = false;
		public String localHostName = defaultHostName;

		public LocalHostNameProvider()
		{
			super("LocalHostNameProvider");
			setDaemon(true);
			start();
		}

		@Override public void run()
		{
			while (true)
			{
				try
				{
					localHostName = InetAddress.getLocalHost().getHostName();
				}
				catch (UnknownHostException ex)
				{
					localHostName = defaultHostName;
				}

				if (!isLocalHostNameReady)
					synchronized (localHostNameProvider)
					{
						isLocalHostNameReady = true;
						notifyAll();
					}
				
				try
				{
					sleep(refreshRate);
				}
				catch (InterruptedException ex)
				{
					return;
				}
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
		if (!localHostNameProvider.isLocalHostNameReady)
			synchronized (localHostNameProvider)
			{
				if (!localHostNameProvider.isLocalHostNameReady)
					try
					{
						localHostNameProvider.wait(100);
					}
					catch (InterruptedException ex)
					{
						return LocalHostNameProvider.defaultHostName;
					}
				if (!localHostNameProvider.isLocalHostNameReady)
				{
					localHostNameProvider.isLocalHostNameReady = true;
					localHostNameProvider.notifyAll();
				}
			}

		return localHostNameProvider.localHostName;
	}
}
