package protocols.ipmsg;

import java.io.IOException;
import java.net.*;
import protocols.ConnectionLostException;

/**
 * Wątek połączenia IPMsg. Dokładniej mówiąc, nie jest to połączenie, tylko
 * otwarcie portu UDP, do odbioru i wysyłania pakietów protokołu IPMsg.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
class IpmsgConnectionThread extends Thread
{
	/**
	 * Konto, w ramach którego uruchomiony jest wątek.
	 */
	protected final IpmsgAccount ipmsgAccount;

	/**
	 * Gniazdo połączenia.
	 */
	protected DatagramSocket sock;

	private final Object sockSendLocker = new Object();

	/**
	 * Czy połączono.
	 */
	public boolean isConnected = false;

	/**
	 * Czy nie udało się połączyć.
	 */
	public boolean failedConnecting = false;

	/**
	 * Bufor dla odbieranych pakietów.
	 */
	protected final byte[] readBuff = new byte[102400];

	/**
	 * Port, na którym jest nasłuchiwanie, oraz na który są wysyłane pakiety
	 * (zarówno UDP, jak i TCP).
	 */
	protected final static int port = 2425;

	/**
	 * Czas oczekiwania na aktywność gniazda, w milisekundach.
	 */
	protected final static int socketTimeout = 1000;

	/**
	 * Główny konstruktor.
	 *
	 * @param ipmsgAccount konto, w ramach którego uruchomiony ma być wątek
	 */
	public IpmsgConnectionThread(IpmsgAccount ipmsgAccount)
	{
		super("ULC-Ipmsg-IpmsgConnectionThread");
		this.ipmsgAccount = ipmsgAccount;
		this.setDaemon(true);
		this.start();
	}

	@Override public void run()
	{
		try
		{
			sock = new DatagramSocket(port);
			sock.setSoTimeout(socketTimeout);
		}
		catch (SocketException e)
		{
			failedConnecting = true;
			return;
		}

		isConnected = true;

		while (true)
		{
			if (isInterrupted())
				break;

			DatagramPacket packet = new DatagramPacket(readBuff, readBuff.length);
			try
			{
				sock.receive(packet);

				if (isInterrupted())
					break;

				IpmsgPacket ipmsgPacket;

				try
				{
					ipmsgPacket = IpmsgPacket.fromRAWData(
							packet.getData(), packet.getLength());
					ipmsgPacket.ip = packet.getAddress().getHostAddress();
				}
				catch (IllegalArgumentException e)
				{
					System.err.println(e.getMessage());
					System.err.println(
						"Pakiet od " + packet.getAddress().getHostAddress() + ", " +
						"Dane: [" + new String(packet.getData(), 0, packet.getLength()) + "]"
						);
					continue;
				}

				ipmsgAccount.handlePacket(ipmsgPacket);
			}
			catch (SocketTimeoutException e)
			{
			}
			catch (IOException e)
			{
				break;
			}
		}

		sock.close();
		isConnected = false;
	}

	/**
	 * Zamyka port i kończy wątek połączenia.
	 */
	public void disconnect()
	{
		isConnected = false;
		this.interrupt();
	}

	/**
	 * Wysyła pakiet za pomocą otwartego portu UDP.
	 *
	 * @param packet pakiet do wysłania
	 */
	public void send(DatagramPacket packet) throws ConnectionLostException
	{
		if (packet == null)
			throw new NullPointerException();
		if (!isConnected)
			throw new ConnectionLostException();
		synchronized(sockSendLocker)
		{
			if (!isConnected)
				throw new ConnectionLostException();
			try
			{
				sock.send(packet);
			}
			catch (SocketTimeoutException e)
			{
			}
			catch (IOException e)
			{
				throw new ConnectionLostException();
			}
		}
	}
}
