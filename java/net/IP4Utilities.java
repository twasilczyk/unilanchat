package net;

import java.net.*;
import java.util.*;
import tools.CachedDataProvider;

/**
 * Klasa pomocnicza dla protokołu IPv4.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class IP4Utilities
{
	private IP4Utilities() { }

	private final static IPAddressesProvider localHostNameProvider =
		new IPAddressesProvider();

	/**
	 * Lista adresów interfejsów w komputerze (cache).
	 */
	protected static List<Inet4Address> ifaceAdresses =
			new Vector<Inet4Address>();

	/**
	 * Lista adresów broadcast interfejsów w komputerze (cache).
	 */
	protected static List<Inet4Address> broadcastAdresses =
			new Vector<Inet4Address>();

	static class IPAddressesProvider extends CachedDataProvider
	{
		/**
		 * Ilość sekund od uruchomienia wątku. Po osiągnięciu (około) minuty
		 * przestaje zliczać czas.
		 */
		private int runTime = 0;

		public IPAddressesProvider()
		{
			super("IPAddressesProvider");
			refreshRate = 1000;
		}

		@Override protected void loadData()
		{
			Enumeration<NetworkInterface> interfaces;
			List<Inet4Address> newIfaceAdresses = new Vector<Inet4Address>();
			List<Inet4Address> newBroadcastAdresses = new Vector<Inet4Address>();

			try
			{
				interfaces = NetworkInterface.getNetworkInterfaces();
			}
			catch (SocketException e)
			{
				return;
			}

			while(interfaces.hasMoreElements())
			{
				NetworkInterface ni = interfaces.nextElement();

				try
				{
					if (ni.isLoopback() || !ni.isUp())
						continue;
				}
				catch (SocketException e)
				{
					continue;
				}

				List<InterfaceAddress> adresyInterfejsu = ni.getInterfaceAddresses();

				for (InterfaceAddress ifAdress : adresyInterfejsu)
				{
					InetAddress adres = ifAdress.getAddress();
					InetAddress bcast = ifAdress.getBroadcast();
					if (bcast != null && (bcast instanceof Inet4Address))
						newBroadcastAdresses.add((Inet4Address)bcast);
					if (adres instanceof Inet4Address)
						newIfaceAdresses.add((Inet4Address)adres);
				}
			}

			ifaceAdresses = newIfaceAdresses;
			broadcastAdresses = newBroadcastAdresses;

			if (runTime < 60) // pierwsza minuta
			{
				if (ifaceAdresses.isEmpty() || broadcastAdresses.isEmpty())
					refreshRate = 1000;
				else
					refreshRate = 3000;
				runTime += refreshRate / 1000;
			}
			else
				refreshRate = 10000;
		}
	}

	/**
	 * Sprawdza, czy podany adres IP jest przypisany do jednego z interfejsów.
	 *
	 * @param ip adres IP w postaci <code>xxx.xxx.xxx.xxx</code>
	 * @return <code>true</code>, jeżeli podany adres jest lokalny
	 */
	public static boolean isLocalAdress(String ip)
	{
		if (ip == null)
			throw new NullPointerException();
		for (Inet4Address addr : getLocalAdresses())
			if (addr.getHostAddress().equals(ip))
				return true;
		return false;
	}

	/**
	 * Pobiera listę adresów lokalnych interfejsów.
	 *
	 * @return lista adresów IPv4
	 */
	public static List<Inet4Address> getLocalAdresses()
	{
		localHostNameProvider.waitForData();
		return ifaceAdresses;
	}

	/**
	 * Pobiera listę adresów broadcast lokalnych interfejsów.
	 *
	 * @return lista adresów IPv4
	 */
	public static List<Inet4Address> getBroadcastAdresses()
	{
		localHostNameProvider.waitForData();
		return broadcastAdresses;
	}

	/**
	 * Zamienia adres IP w formie tekstowej na obiekt typu InetAddress.
	 *
	 * @param ip adres IPv4 w postaci <code>xxx.xxx.xxx.xxx</code>
	 * @return adres typu InetAddress
	 * @throws UnknownHostException jeżeli podany adres jest nieprawidłowy
	 */
	public static InetAddress getIPAddress(String ip) throws UnknownHostException
	{
		if (ip == null)
			throw new NullPointerException();

		String[] o = ip.split("\\.", 5);
		if (o.length != 4)
			throw new UnknownHostException(ip);
		byte[] oByte = new byte[4];
		for (int i = 0; i < 4; i++)
		{
			try
			{
				oByte[i] = (byte)Integer.parseInt(o[i]);
			}
			catch (NumberFormatException e)
			{
				throw new UnknownHostException(ip);
			}
		}

		return Inet4Address.getByAddress(oByte);
	}
}
