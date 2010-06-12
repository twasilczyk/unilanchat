package protocols.ipmsg;

import java.io.*;
import java.net.*;

/**
 * Klasa pliku wysyłanego.
 *
 * @author Piotr Gajowiak
 */
public class IpmsgSentFile extends IpmsgTransferredFile
{
	/**
	 * Licznik określający kolejny ID pliku.
	 */
	protected static long nextFileID = 0;

	/**
	 * Konstruktor pliku wysyłanego.
	 *
	 * @param file plik do wysłania
	 * @param contact kontakt, do którego wysyłany jest plik
	 * @param packetID ID pakietu, który inicjuje wysłanie
	 * @throws FileNotFoundException gdy podany w parametrze plik nie istnieje
	 */
	public IpmsgSentFile(File file, IpmsgContact contact, long packetID) throws FileNotFoundException
	{
		if (file == null || contact == null)
			throw new NullPointerException();
		if (!file.exists())
			throw new FileNotFoundException("Plik \"" + file.getAbsolutePath() + "\" nie istnieje");
		this.file = file;
		this.isFile = file.isFile();
		if(isFile)
			this.fileSize = file.length();
		else
			this.fileSize = 0;
		this.contact = contact;
		this.fileID = nextFileID++;
		this.packetID = packetID;
	}

	protected IpmsgSentFile()
	{
	}

	/**
	 * Wysyła plik do strumienia, który jest powiązany z gniazdem.
	 * podanym w parametrze.
	 *
	 * @param socket gniado, do którego należy wysłać plik
	 * @param offset ilość początkowych bajtów, która ma zostać pominięta
	 * @throws SocketException gdy podane w parametrze gniazdo nie jest połączone
	 */
	public void send(Socket socket, long offset) throws SocketException
	{
		if (socket == null)
				throw new NullPointerException();
		if (!socket.isConnected())
			throw new SocketException("Gniazdo nie jest polaczone");
		synchronized(this)
		{
			if(state == States.COMPLETED)
				throw new RuntimeException("Plik zostal juz wyslany");
			if(thread != null && thread.isAlive())
				throw new RuntimeException("Plik jest wlasnie wysylany");
			if(isFile && offset > fileSize || offset < 0)
				throw new RuntimeException("Zly offset: " + Long.toString(offset));
			thread = new SendingThread(socket, offset);
			thread.setDaemon(true);
			transferredDataSize = 0;
			thread.start();
		}
	}
	
	@Override protected Object clone()
	{
		IpmsgSentFile f = new IpmsgSentFile();
		f.contact = contact;
		f.file = file;
		f.fileID = fileID;
		f.packetID = packetID;
		f.fileSize = fileSize;
		f.isFile = isFile;
		return f;
	}

	/**
	 * Klasa wątku wysyłającego plik.
	 */
	protected class SendingThread extends Thread
	{
		protected Socket socket;

		protected long offset;

		public SendingThread(Socket socket, long offset) throws SocketException
		{
			if (socket == null)
				throw new NullPointerException();
			if (!socket.isConnected())
				throw new SocketException("Gniazdo nie jest polaczone");
			this.socket = socket;
			this.offset = offset;
		}

		@Override public void run()
		{
			FileInputStream fileInputStream = null;
			try
			{
				OutputStream outputStream = socket.getOutputStream();

				byte[] buffer = new byte[bufferSize];
				int readChunkSize;
				long transferredSize = 0;

				setState(States.TRANSFERRING);
				if (isFile)
				{
					fileInputStream = new FileInputStream(file);
					while(offset != 0)
						offset -= fileInputStream.skip(offset);
					
					while((readChunkSize = fileInputStream.read(buffer)) != -1)
					{
						outputStream.write(buffer, 0, readChunkSize);
						transferredSize += readChunkSize;
						setTransferredDataSize(transferredSize);
					}
				}
				else
				{
					throw new UnsupportedOperationException("Wysylanie folderow jescze nie dziala");
				}
				setState(States.COMPLETED);
			}
			catch (IOException ex)
			{
				setState(States.ERROR);
			}
			finally
			{
				try
				{
					if(fileInputStream != null)
						fileInputStream.close();
					socket.close();
				}
				catch (IOException ex)
				{
				}
			}
		}
	}
}
