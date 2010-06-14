package protocols.ipmsg;

/**
 * Reprezentuje nagłówek przesyłany podczas żądania wysłania pliku.
 * Dołączany jako kombinacja do danych pakietu przez osobę chcącą wysłać plik.
 * 
 * Przy poprawnym użyciu dołączana jest pewna kombinacja tych nagłówków do
 * sekcji dodatkowej pakietu IpmsgPacket wraz z ustawioną dodatkową flagą
 * IPMSG_FILEATTACHOPT zaraz za danymi wiadomości oddzielona od wiadomości przez
 * '\0' oraz ostatecznie zakończona również przez '\0'.
 * 
 * @author Piotr Gajowiak
 */
class IpmsgFileSendRequestHeader
{
	/**
	 * ID pliku, który ma byc wysłany
	 */
	protected long fileID;

	/**
	 * Nazwa pliku.
	 */
	protected String fileName;

	/**
	 * Rozmiar pliku.
	 */
	protected long fileSize;

	/**
	 * Ostatni czas modyfikacji pliku.
	 */
	protected long modificationTime;

	/**
	 * Stała związana z plikiem, określająca czy jest to plik czy katalog.
	 * @see IpmsgTransferredFile
	 */
	protected long fileAttribute;

	/**
	 * Dodatkowe atrybuty pliku.
	 */
	protected String[] extendedAttributes = null;

	/**
	 * Konstruktor nagłówka dla pakietów odbieranych.
	 *
	 * @param data dane, które należy zparsować
	 * @return obiekt nagłówka
	 */
	public static IpmsgFileSendRequestHeader fromRawData(String data)
	{
		IpmsgFileSendRequestHeader header = new IpmsgFileSendRequestHeader();

		int i = data.indexOf(":");

		try
		{
			header.fileID = Long.parseLong(data.substring(0, i++), 16);
		}
		catch(NumberFormatException ex)
		{
			throw new IllegalArgumentException("Pole fileID jest niepoprawne");
		}

		int j = i;
		
		while(j < data.length())
		{
			if(data.charAt(j) == ':')
			{
				if(j > data.length() - 2 || data.charAt(j + 1) != ':')
					break;
				else
				{
					j += 2;
					continue;
				}
			}
			j++;
		}

		header.fileName = data.substring(i, j).replace("::", ":");

		String fields[] = data.substring(++j).split(":");

		if(fields.length < 3)
			throw new IllegalArgumentException("Niepoprawna liczba sekcji");

		try
		{
			header.fileSize = Long.parseLong(fields[0], 16);
			header.modificationTime = Long.parseLong(fields[1], 16);
			header.fileAttribute = Long.parseLong(fields[2], 16);
		}
		catch(NumberFormatException ex)
		{
			throw new IllegalArgumentException("Niepoprawne pole fileSize," +
					"modificationTime, badz fileAttribute");
		}

		if(fields.length > 3)
		{
			header.extendedAttributes = new String[fields.length - 3];
			for(int k = 0; k < fields.length - 3; k++)
			{
				System.out.println(fields[k + 3].trim());
				header.extendedAttributes[k] = fields[k + 3].trim();
			}
		}

		return header;
	}

	protected IpmsgFileSendRequestHeader()
	{
	}

	/**
	 * Kontruktor nagłówka dla pakietów wysyłanych.
	 *
	 * @param file plik dla którego należy utworzyć nagłówek
	 */
	protected IpmsgFileSendRequestHeader(IpmsgSentFile file)
	{
		if(file == null)
			throw new NullPointerException();
		if(!file.file.exists())
			throw new IllegalArgumentException();
		this.fileID = file.fileID;
		this.fileName = file.file.getName();
		this.fileSize = file.fileSize == null ? 0 : file.fileSize;
		this.modificationTime = file.file.lastModified();
		this.fileAttribute = file.file.isFile() ? IpmsgTransferredFile.FLAG_FILE_REGULAR :
								IpmsgTransferredFile.FLAG_FILE_DIR;
	}


	@Override public String toString()
	{
		StringBuilder builder = new StringBuilder("");
		
		builder.append(Long.toHexString(fileID));
		builder.append(":");
		builder.append(fileName.replace(":", "::"));
		builder.append(":");
		builder.append(Long.toHexString(fileSize));
		builder.append(":");
		builder.append(Long.toHexString(modificationTime));
		builder.append(":");
		builder.append(Long.toHexString(fileAttribute));
		
		if(extendedAttributes != null)
			for(String s: extendedAttributes)
			{
				builder.append(":");
				builder.append(s);
			}
		builder.append(":");

		return builder.toString();
	}
}
