package protocols.ipmsg;

import java.io.*;
import java.net.*;

import main.Configuration;
import protocols.*;

/**
 * Klasa pliku odbieranego.
 *
 * @author Piotr Gajowiak
 */
public class IpmsgReceivedFile extends IpmsgTransferredFile implements ReceivedFile
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
		this.isFile = (header.fileAttribute == IpmsgTransferredFile.FLAG_FILE_REGULAR);
		this.fileName = header.fileName;
	}

	/**
	 * Odbiera plik i zapisuje go do pliku o ścieżce zadanej w parametrze.
	 *
	 * @param path ścieżka, pod którą należy zapisać plik
	 */
	public synchronized void receive(File target)
	{
		file = target;
		if(file.exists() && (!file.canWrite() || file.isDirectory()))
			throw new IllegalArgumentException("Plik juz istnieje i nie można go nadpisać");
		if(state == State.COMPLETED)
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
			startNotifying();
			try
			{
				socket = new Socket(contact.ip, IpmsgFileTransferThread.serverPort);
				IpmsgFileReceiveRequestHeader header =
						new IpmsgFileReceiveRequestHeader(receivedFile, isFile ? 0L : null);
				IpmsgPacket packet = new IpmsgPacket(Configuration.getInstance().getNick());
				packet.setCommand(isFile ? IpmsgPacket.COMM_GETFILEDATA : IpmsgPacket.COMM_GETDIRFILES);
				packet.data = header.toString();

				socketInputStream = socket.getInputStream();
				socketOutputStream = socket.getOutputStream();

				socketOutputStream.write(packet.getRAWData());

				byte[] buffer = new byte[IpmsgTransferredFile.bufferSize];
				int readChunkSize;
				long transferredSize = 0;

				setState(TransferredFile.State.TRANSFERRING);
				if(isFile)
				{
					if (file.exists())
						file.delete();
					if(!file.createNewFile())
						throw new IOException("Nie udalo sie utworzyc");

					fileOutputStream = new FileOutputStream(file);

					while((readChunkSize = socketInputStream.read(buffer)) != -1)
					{
						transferredSize += readChunkSize;
						fileOutputStream.write(buffer, 0, readChunkSize);
						setTransferredDataSize(transferredSize);
						if(transferredSize >= fileSize)
							break;
					}

					if(transferredSize != fileSize)
						throw new IOException();
				}
				else
				{
					File tempFile = null;
					int readByte, i;
					long headerSize;

					// Dopoki jestesmy wewnatrz katalogu
					while(tempFile == null ||
							!(!tempFile.getCanonicalPath().equals(file.getCanonicalPath()) &&
							file.getCanonicalPath().startsWith(tempFile.getCanonicalPath())))
					{
						// Czytanie naglowka
						i = 0;
						while((readByte = socketInputStream.read()) != -1 && readByte != ':')
						{
							if(i >= bufferSize)
								throw new IOException();
							buffer[i++] = (byte)readByte;
						}

						try
						{
							headerSize = Long.parseLong(new String(buffer, 0, i, IpmsgPacket.protocolCharset), 16);
						}
						catch(NumberFormatException ex)
						{
							throw new IOException();
						}

						if(readByte != ':')
							throw new IOException();

						buffer[i++] = (byte)readByte;

						if(headerSize > bufferSize)
							throw new IOException();

						headerSize -= i;

						while(headerSize != 0)
						{
							readChunkSize = socketInputStream.read(buffer, i, (int)headerSize);
							if(readChunkSize == -1)
								break;
							i += readChunkSize;
							headerSize -= readChunkSize;
						}

						IpmsgHierarchicalFileHeader hierarchicalHeader;
						try
						{
							hierarchicalHeader = IpmsgHierarchicalFileHeader.fromRawData(buffer, i);
						}
						catch (IllegalArgumentException ex)
						{
							throw new IOException();
						}
						// Koniec czytania naglowka

						File tempFile2 = new File(tempFile == null ? file.getCanonicalPath() : tempFile.getCanonicalPath()
									+ File.separator + hierarchicalHeader.fileName);

						if(hierarchicalHeader.fileAttribute == IpmsgTransferredFile.FLAG_FILE_REGULAR)
						{
							// Pierwsze utworzenie musi byc katalogiem
							if(tempFile == null)
								throw new IOException();
							if(!tempFile2.createNewFile())
								throw new IOException();
							fileOutputStream = new FileOutputStream(tempFile2);
							long dataSizeLeft = hierarchicalHeader.fileSize;
							setFileSize(hierarchicalHeader.fileSize);
							while(dataSizeLeft != 0)
							{
								readChunkSize = socketInputStream.read(buffer, 0, (int)Math.min(dataSizeLeft, (long)bufferSize));
								if(readChunkSize == -1 && dataSizeLeft != 0)
									throw new IOException();
								fileOutputStream.write(buffer, 0, readChunkSize);
								dataSizeLeft -= readChunkSize;
								transferredSize += readChunkSize;
								setTransferredDataSize(transferredSize);
							}
							fileOutputStream.close();
						}
						else if(hierarchicalHeader.fileAttribute == IpmsgTransferredFile.FLAG_FILE_DIR)
						{
							if(!tempFile2.mkdir())
								throw new IOException();
							tempFile = tempFile2;
						}
						else if(hierarchicalHeader.fileAttribute == IpmsgTransferredFile.FLAG_FILE_RETPARENT)
						{
							// Pierwse utworzenie musi byc katalogiem
							if(tempFile == null)
								throw new IOException();
							tempFile = tempFile.getParentFile();
						}
						else
							throw new IOException("Nieznana flaga przesylania folderow");
					}
				}
				setState(TransferredFile.State.COMPLETED);
			}
			catch (IOException ex)
			{
				setState(TransferredFile.State.ERROR);
				// Jesli plik zostal utworzony przez nas i byl blad to usuwamy
				if(fileOutputStream != null)
					file.delete();
			}
			finally
			{
				stopNotifying();
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
