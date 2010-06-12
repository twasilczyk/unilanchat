package protocols.ipmsg;

/**
 * Klasa nagłówka żądania odebrania pliku.
 *
 * @author Piotr Gajowiak
 */
class IpmsgFileReceiveRequestHeader
{
	/**
	 * ID pakietu, w którym było zawarte żądanie wysłania pliku.
	 */
	protected long packetID;

	/**
	 * ID pliku, który był zawarty w nagłówku żądania wysłania pliku.
	 */
	protected long fileID;

	/**
	 * Ilość początkowych bajtów która ma zostać pominięta podczas wysyłania.
	 */
	protected long offset;

	/**
	 * Kontruktor nagłówka dla pakietów odbieranych.
	 *
	 * @param raw dane które należy zparsować
	 * @return obiekt nagłówka
	 */
	public static IpmsgFileReceiveRequestHeader fromRawData(String raw)
	{
		String[] data = raw.split(":");

		if(data.length != 3)
			throw new IllegalArgumentException("Niepoprawna liczba sekcji");

		IpmsgFileReceiveRequestHeader header = new IpmsgFileReceiveRequestHeader();

		try
		{
			header.packetID = Long.parseLong(data[0], 16);
			header.fileID = Long.parseLong(data[1], 16);
			header.offset = Long.parseLong(data[2], 16);
		}
		catch(NumberFormatException ex)
		{
			throw new IllegalArgumentException("Blad parsowania packetID, fileID, badz offset");
		}

		return header;
	}

	protected IpmsgFileReceiveRequestHeader()
	{
	}

	/**
	 * Kontruktor nagłówka dla pakietów wysyłanych.
	 *
	 * @param file wysyłany plik
	 * @param offset ilość pierwszych bajtów które mają zostać pominięte
	 * podczas transmisji
	 */
	protected IpmsgFileReceiveRequestHeader(IpmsgReceivedFile file, long offset)
	{
		if(offset < 0)
			throw new IllegalArgumentException("Ujemny offset");
		if(!file.isFile)
			throw new IllegalArgumentException("Argument jest katalogiem");
		this.packetID = file.packetID;
		this.fileID = file.fileID;
		this.offset = offset;
	}

	@Override public String toString()
	{
		StringBuilder builder = new StringBuilder("");

		builder.append(Long.toString(packetID, 16));
		builder.append(":");
		builder.append(Long.toString(fileID, 16));
		builder.append(":");
		builder.append(Long.toString(offset, 16));
		builder.append(":");

		return builder.toString();
	}
}
