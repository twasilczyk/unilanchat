package tools;

import java.net.*;
import java.util.*;

/**
 * Klasa pomocnicza dla protokołu IPv4
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class IP4Utilities
{
	private IP4Utilities() { }

	/**
	 * Lista adresów interfejsów w komputerze (cache).
	 */
	protected static final List<Inet4Address> ifaceAdresses =
			new Vector<Inet4Address>();

	/**
	 * Lista adresów broadcast interfejsów w komputerze (cache).
	 */
	protected static final List<Inet4Address> broadcastAdresses =
			new Vector<Inet4Address>();

	/**
	 * Czy adresy zostały wczytane.
	 *
	 * @todo Może zrobić jakieś odświeżanie?
	 * @see #ifaceAdresses
	 * @see #broadcastAdresses
	 */
	protected static boolean adressesLoaded = false;

	/**
	 * Sprawdza, czy podany adres IP jest przypisany do jednego z interfejsów.
	 *
	 * @param ip Adres IP w postaci xxx.xxx.xxx.xxx
	 * @return Czy podany adres jest lokalny
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
	 * @return Lista adresów IPv4
	 */
	public static List<Inet4Address> getLocalAdresses()
	{
		loadAdresses();
		return ifaceAdresses;
	}

	/**
	 * Pobiera listę adresów broadcast lokalnych interfejsów.
	 *
	 * @return Lista adresów IPv4
	 */
	public static List<Inet4Address> getBroadcastAdresses()
	{
		loadAdresses();
		return broadcastAdresses;
	}

	/**
	 * Inicjuje (o ile jest taka potrzeba) tablice z adresami.
	 *
	 * @see #adressesLoaded
	 * @see #ifaceAdresses
	 * @see #broadcastAdresses
	 */
	protected static void loadAdresses()
	{
		if (adressesLoaded)
			return;

		synchronized (ifaceAdresses)
		{
			if (adressesLoaded)
				return;

			Enumeration<NetworkInterface> interfaces;

			ifaceAdresses.clear();
			broadcastAdresses.clear();

			try
			{
				interfaces = NetworkInterface.getNetworkInterfaces();
			}
			catch (SocketException e)
			{
				adressesLoaded = true;
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
						broadcastAdresses.add((Inet4Address)bcast);
					if (adres instanceof Inet4Address)
						ifaceAdresses.add((Inet4Address)adres);

				}
			}
			
			adressesLoaded = true;
		}
	}

	/**
	 * Zamienia adres IP w formie tekstowej na obiekt typu InetAddress.
	 *
	 * @param ip Adres IPv4 w postaci xxx.xxx.xxx.xxx
	 * @return Adres typu InetAddress
	 * @throws UnknownHostException Jeżeli podany adres jest nieprawidłowy
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

	public static String getLocalHostName()
	{
		try
		{
			return InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e)
		{
			return "localhost";
		}
	}
}
