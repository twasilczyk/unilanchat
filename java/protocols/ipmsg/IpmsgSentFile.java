package protocols.ipmsg;

import java.io.*;
import java.net.*;
import java.util.Stack;

import protocols.TransferredFile;

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
		calculateFileSize();
		this.contact = contact;
		this.fileID = nextFileID++;
		this.packetID = packetID;
		this.fileName = file.getName();
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
	public void send(Socket socket, Long offset) throws SocketException
	{
		if (socket == null)
				throw new NullPointerException();
		if (!socket.isConnected())
			throw new SocketException("Gniazdo nie jest polaczone");
		if((offset == null) != !isFile)
			throw new IllegalArgumentException("Zadano katalogu, a obiekt jest plikiem");
		synchronized(this)
		{
			if(state == State.COMPLETED)
				throw new RuntimeException("Plik zostal juz wyslany");
			if(thread != null && thread.isAlive())
				throw new RuntimeException("Plik jest wlasnie wysylany");
			if(isFile && (offset > fileSize || offset < 0))
				throw new RuntimeException("Zly offset: " + Long.toString(offset));
			state = State.WAITING_FOR_CONNECTION;
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
		f.fileName = fileName;
		return f;
	}

	/**
	 * Oblicza rozmiar pliku. Dla katalogu będzie to suma długości
	 * wszytkich plików wewnątrz katalogu
	 */
	protected void calculateFileSize()
	{
		if(isFile)
			fileSize = file.length();
		else
		{
			Stack<DirectoryNode> stack = new Stack<DirectoryNode>();
			stack.push(new DirectoryNode(file.listFiles()));
			long filesSize = 0;
			while(!stack.empty())
			{
				File nextFile = stack.peek().getNextFile();
				if(nextFile == null)
				{
					stack.pop();
					continue;
				}
				else if(nextFile.isFile())
					filesSize += nextFile.length();
				else
					stack.push(new DirectoryNode(nextFile.listFiles()));
			}
			fileSize = filesSize;
		}
	}

	/**
	 * Klasa wątku wysyłającego plik.
	 */
	class SendingThread extends Thread
	{
		protected Socket socket;

		protected Long offset;

		public SendingThread(Socket socket, Long offset) throws SocketException
		{
			super("ULC-IpMsg-IpmsgSentFile-SendingThread");

			if (socket == null)
				throw new NullPointerException();
			if (!socket.isConnected())
				throw new SocketException("Gniazdo nie jest polaczone");
			this.socket = socket;
			this.offset = offset;
			setPriority(getPriority() - 1);
		}

		@Override public void run()
		{
			FileInputStream fileInputStream = null;
			startNotifying();
			try
			{
				OutputStream outputStream = socket.getOutputStream();

				byte[] buffer = new byte[bufferSize];
				int readChunkSize;
				long transferredSize = 0;

				if (isFile)
				{
					setState(TransferredFile.State.TRANSFERRING);
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
					Stack<DirectoryNode> stack = new Stack<DirectoryNode>();
					IpmsgHierarchicalFileHeader header = new IpmsgHierarchicalFileHeader(file);

					setState(TransferredFile.State.TRANSFERRING);
					outputStream.write(header.toRawData());
					stack.push(new DirectoryNode(file.listFiles()));
					while(!stack.empty())
					{
						File nextFile = stack.peek().getNextFile();
						if(nextFile == null)
						{
							header = IpmsgHierarchicalFileHeader.getReturnParentHeader();
							stack.pop();
							outputStream.write(header.toRawData());
						}
						else
						{
							header = new IpmsgHierarchicalFileHeader(nextFile);
							if(nextFile.isFile())
							{
								outputStream.write(header.toRawData());
								fileInputStream = new FileInputStream(nextFile);
								while((readChunkSize = fileInputStream.read(buffer)) != -1)
								{
									outputStream.write(buffer, 0, readChunkSize);
									transferredSize += readChunkSize;
									setTransferredDataSize(transferredSize);
								}
								fileInputStream.close();
							}
							else
							{
								stack.push(new DirectoryNode(nextFile.listFiles()));
								outputStream.write(header.toRawData());
							}
						}
					}
				}
				setState(TransferredFile.State.COMPLETED);
			}
			catch (SocketException ex)
			{
				setState(TransferredFile.State.ERROR);
			}
			catch (IOException ex)
			{
				setState(TransferredFile.State.ERROR);
				throw new RuntimeException(ex);
			}
			finally
			{
				stopNotifying();
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

	class DirectoryNode
	{
		File[] files;
		int currentPosition = 0;

		public DirectoryNode(File[] files)
		{
			if(files == null)
				throw new NullPointerException();
			this.files = files;
		}

		public File getNextFile()
		{
			if(currentPosition < files.length)
				return files[currentPosition++];
			return null;
		}
	}
}