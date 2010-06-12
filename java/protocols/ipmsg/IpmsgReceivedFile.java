package protocols.ipmsg;

import java.io.*;
import java.net.*;

import main.Configuration;

/**
 * Klasa pliku odbieranego.
 *
 * @author Piotr Gajowiak
 */
public class IpmsgReceivedFile extends IpmsgTransferredFile
{
	/**
	 * Konstruktor pliku odbieranego.
	 *
	 * @param header nagłówek z danych wiadomości inicjującej wysyłanie
	 * @param contact kontakt od którego jest pobierany plik
	 * @param packetID ID pakietu który inicjował wysyłanie
	 */
	public IpmsgReceivedFile(IpmsgFileSendRequestHeader header, IpmsgContact contact, long packetID)
	{
		if(header == null || contact == null)
			throw new NullPointerException();
		this.contact = contact;
		this.packetID = packetID;
		this.fileID = header.fileID;
		this.fileSize = header.fileSize;
		this.isFile = header.fileAttribute == IpmsgTransferredFile.FLAG_FILE_REGULAR;
	}

	/**
	 * Odbiera plik i zapisuje go do pliku o ścieżce zadanej w parametrze.
	 *
	 * @param path ścieżka, pod którą należy zapisać plik
	 */
	public synchronized void receive(String path)
	{
		file = new File(path);
		if(file.exists())
			throw new IllegalArgumentException("Plik juz istnieje");
		if(state == States.COMPLETED)
			throw new RuntimeException("Plik zostal juz pobrany");
		if(thread != null && thread.isAlive())
			throw new RuntimeException("Plik jest wlasnie pobierany");

		thread = new ReceivingThread(this);
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Wątek zajmujący się pobieraniem pliku.
	 */
	protected class ReceivingThread extends Thread
	{
		IpmsgReceivedFile receivedFile;

		public ReceivingThread(IpmsgReceivedFile receivedFile)
		{
			if(receivedFile == null)
				throw new NullPointerException();
			this.receivedFile = receivedFile;
		}

		@Override public void run()
		{
			Socket socket = null;
			InputStream socketInputStream = null;
			OutputStream socketOutputStream = null;
			FileOutputStream fileOutputStream = null;
			try
			{
				socket = new Socket(contact.ip, IpmsgFileTransferThread.serverPort);
				IpmsgFileReceiveRequestHeader header = new IpmsgFileReceiveRequestHeader(receivedFile, 0);
				IpmsgPacket packet = new IpmsgPacket(Configuration.getInstance().getNick());
				packet.setCommand(isFile ? IpmsgPacket.COMM_GETFILEDATA : IpmsgPacket.COMM_GETDIRFILES);
				packet.data = header.toString();

				socketInputStream = socket.getInputStream();
				socketOutputStream = socket.getOutputStream();

				socketOutputStream.write(packet.getRAWData());

				setState(States.TRANSFERRING);
				if(isFile)
				{
					if(!file.createNewFile())
						throw new IOException("Nie udalo sie utworzyc");

					fileOutputStream = new FileOutputStream(file);

					byte[] buffer = new byte[IpmsgTransferredFile.bufferSize];
					int readChunkSize;
					long transferredSize = 0;

					while((readChunkSize = socketInputStream.read(buffer)) != -1)
					{
						transferredSize += readChunkSize;
						fileOutputStream.write(buffer, 0, readChunkSize);
						if(transferredSize >= fileSize)
							break;
						setTransferredDataSize(transferredSize);
					}

					if(transferredSize != fileSize)
						throw new IOException();
				}
				else
				{
					throw new UnsupportedOperationException();
				}
				setState(States.COMPLETED);
			}
			catch (IOException ex)
			{
				setState(States.ERROR);
				// Jesli plik zostal utworzony przez nas i byl blad to usuwamy
				if(fileOutputStream != null)
					file.delete();
			}
			finally
			{
				try
				{
					// Jesli udalo sie otworzyc strumien dla pliku to zamykamy
					if(fileOutputStream != null)
						fileOutputStream.close();
					socket.close();
				}
				catch (IOException ex1)
				{
				}
			}
		}
	}
}
