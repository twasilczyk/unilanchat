package protocols.ipmsg;

import java.io.*;
import java.net.*;

/**
 * Klasa zajmująca się transferem pliku w obrębie danego konta użytkownika.
 *
 * @author Piotr Gajowiak
 */
public class IpmsgFileTransferThread extends Thread
{
	/**
	 * Konto użytkownika, dla którego pracuje wątek.
	 */
	protected IpmsgAccount account;

	/**
	 * Gniazdo protokołu TCP, na którym wątek akceptuje połączenia.
	 */
	protected ServerSocket serverSocket;

	/**
	 * Domyślny port, na którym nasłuchuje wątek.
	 */
	public static final int serverPort = 2425;

	/**
	 * Domyślny timeout na zaakceptowanym połączeniu.
	 */
	public static final int connectionSocketTimeout = 5000;

	/**
	 * Konstruktor wątku dla danego konta.
	 *
	 * @param account konto dla którego działa wątek.
	 */
	public IpmsgFileTransferThread(IpmsgAccount account) throws IOException
	{
		super("ULC-IpMsg-IpmsgFileTransferThread");
		if(account == null)
			throw new NullPointerException();
		setDaemon(true);
		this.account = account;
		serverSocket = new ServerSocket(serverPort);
		serverSocket.setSoTimeout(500);
		start();
	}

	@Override public void run()
	{
		Socket socket = null;

		try
		{
			while(true)
			{
				try
				{
					if (isInterrupted())
						break;

					try
					{
						socket = serverSocket.accept();
						socket.setSoTimeout(connectionSocketTimeout);
					}
					catch (SocketTimeoutException e)
					{
						continue;
					}

					if (isInterrupted())
						break;

					FileRecognitionThread thread = new FileRecognitionThread(socket);
					thread.setDaemon(true);
					thread.start();
				}
				catch (IOException ex)
				{
					throw new RuntimeException(ex);
				}
			}
		}
		finally
		{
			try
			{
				serverSocket.close();
				if (socket != null && socket.isConnected())
					socket.close();
			}
			catch (IOException ex2)
			{
			}
		}
	}

	private static int fileRecognitionThreadCount = 0;

	/**
	 * Wątek wyszukujący plik na liście plików danego konta.
	 * Przekazuje wysyłanie pliku do odpowiedniego obiektu.
	 */
	class FileRecognitionThread extends Thread
	{
		protected Socket socket;

		public static final int bufferSize = 8192;

		public FileRecognitionThread(Socket socket) throws SocketException
		{
			super("ULC-IpMsg-FileRecognitionThread-" +
				(fileRecognitionThreadCount++));

			if(socket == null)
				throw new NullPointerException();
			if(!socket.isConnected())
				throw new SocketException("Gniazdo nie jest polaczone");
			this.socket = socket;
		}

		@Override public void run()
		{
			byte[] buffer = new byte[bufferSize];
			int readSize, currentSize = 0;

			InputStream stream;
			try
			{
				stream = socket.getInputStream();

				IpmsgPacket packet = null;

				// TODO: Trzeba to poprawic jakos madrze bo okazalo sie ze Ipmsg nie dodaje \0 na koncu wiadomosci
				while((readSize = stream.read(buffer, currentSize, bufferSize - currentSize)) != -1)
				{
					currentSize += readSize;
					if(currentSize == bufferSize)
						throw new IOException();
					try
					{
						packet = IpmsgPacket.fromRAWData(buffer, currentSize);
						break;
					}
					catch (IllegalArgumentException ex)
					{
					}
				}

				// Akceptujemy jedynie zadania wyslania pliku badz folderu reszte ignorujemy
				if (packet.getCommand() != IpmsgPacket.COMM_GETFILEDATA &&
					packet.getCommand() != IpmsgPacket.COMM_GETDIRFILES)
				{
					socket.close();
					return;
				}

				IpmsgFileReceiveRequestHeader header;

				// Naglowek zadania pobrania musi sie parsowac
				try
				{
					header = IpmsgFileReceiveRequestHeader.fromRawData(packet.data);
				}
				catch(IllegalArgumentException ex)
				{
					socket.close();
					return;
				}

				IpmsgSentFile file = account.getSentFile(socket.getInetAddress().getHostAddress(), header);
				// Jesli pliku nie zadeklarowano do wysłania to konczymy
				if (file == null)
				{
					socket.close();
					return;
				}
				
				/*
				 * Jesli plik byl zadeklarowany jako katalog, a ktos zada pliku
				 * to konczymy, analogicznie w symetrycznym przypadku
				 */
				if (!(packet.getCommand() == IpmsgPacket.COMM_GETFILEDATA &&
					file.isFile() && header.offset != null ||
					packet.getCommand() == IpmsgPacket.COMM_GETDIRFILES &&
					!file.isFile() && header.offset == null))
				{
					socket.close();
					return;
				}

				// Ostatecznie oddelegowujemy plik do wyslania
				file.send(socket, header.offset);
			}
			catch (SocketTimeoutException ex)
			{
				try
				{
					socket.close();
				}
				catch (IOException ex1)
				{
				}

				throw new RuntimeException("TODO: zaimplementować obsługę SocketTimeoutException", ex);
			}
			catch (IOException ex)
			{
				try
				{
					socket.close();
				}
				catch (IOException ex1)
				{
				}
				throw new RuntimeException(ex);
			}
		}
	}
}
