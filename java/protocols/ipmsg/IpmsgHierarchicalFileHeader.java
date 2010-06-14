package protocols.ipmsg;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Reprezentuje nagłówek identyfikujący przesyłany plik
 * podczas transferowania katalogu.
 *
 * @author Piotr Gajowiak
 */
class IpmsgHierarchicalFileHeader
{
	/**
	 * Długość nagłówka w bajtach.
	 */
	long headerSize;

	/**
	 * Nazwa pliku.
	 */
	String fileName;

	/**
	 * Rozmiar pliku w bajtach, dla katalogu 0.
	 */
	long fileSize;

	/**
	 * Atrybut określający czy plik jest katalogiem, plikiem właściwym bądź
	 * komendą powrót do katalogu wyżej.
	 */
	long fileAttribute;

	/**
	 * Atrybuty dodatkowe.
	 */
	String[] extendedAttributes = null;

	/**
	 * Konstruktor nagłówków odbieranych.
	 *
	 * @param data bufor zawierający dane do zparsowania
	 * @param length ilość bajtów z bufora którą należy uwzględnić w parsowaniu
	 * @return obiekt zparsowanego nagłówka
	 */
	public static IpmsgHierarchicalFileHeader fromRawData(byte[] data, int length)
	{
		if (data == null)
			throw new NullPointerException();

		String[] splittedData = (new String(data, 0, length, IpmsgPacket.protocolCharset)).split(":");

		if (splittedData.length < 4)
			throw new IllegalArgumentException("Nieprawidlowa liczba sekcji");

		IpmsgHierarchicalFileHeader header = new IpmsgHierarchicalFileHeader();

		try
		{
			header.headerSize = Long.parseLong(splittedData[0], 16);
		}
		catch (NumberFormatException ex)
		{
			throw new IllegalArgumentException("Nieprawidlowy rozmiar naglowka", ex);
		}

		header.fileName = splittedData[1].trim();

		try
		{
			header.fileSize = Long.parseLong(splittedData[2], 16);
		}
		catch (NumberFormatException ex)
		{
			throw new IllegalArgumentException("Nieprawidlowy rozmiar pliku", ex);
		}

		try
		{
			header.fileAttribute = Long.parseLong(splittedData[3], 16);
		}
		catch (NumberFormatException ex)
		{
			throw new IllegalArgumentException("Nieprawidlowy atrybut pliku", ex);
		}

		if (header.fileAttribute != IpmsgTransferredFile.FLAG_FILE_REGULAR &&
				header.fileAttribute != IpmsgTransferredFile.FLAG_FILE_DIR &&
				header.fileAttribute != IpmsgTransferredFile.FLAG_FILE_RETPARENT)
			throw new IllegalArgumentException("Niprawidlowa wartosc atrybutu pliku");

		if (header.fileAttribute == IpmsgTransferredFile.FLAG_FILE_RETPARENT &&
				(!header.fileName.equals(".") || header.fileSize != 0))
			throw new IllegalArgumentException("");

		if (splittedData.length > 4)
		{
			header.extendedAttributes = new String[splittedData.length - 4];
			for(int i = 4; i < splittedData.length; i++)
				header.extendedAttributes[i - 4] = splittedData[i].trim();
		}

		return header;
	}

	/**
	 * Zwraca nagłówek oznaczający powrót do katalogu wyżej
	 *
	 * @return
	 */
	public static IpmsgHierarchicalFileHeader getReturnParentHeader()
	{
		IpmsgHierarchicalFileHeader header = new IpmsgHierarchicalFileHeader();
		header.fileName = ".";
		header.fileSize = 0;
		header.fileAttribute = IpmsgTransferredFile.FLAG_FILE_RETPARENT;
		return header;
	}

	protected IpmsgHierarchicalFileHeader()
	{
	}

	/**
	 * Konstruktor nagłówków wysyłanych.
	 * 
	 * @param file plik, dla którego należy utworzyć nagłówek
	 * @throws FileNotFoundException gdy podany w parametrze plik nie istnieje
	 */
	public IpmsgHierarchicalFileHeader(File file) throws FileNotFoundException
	{
		if (file == null)
			throw new NullPointerException();
		if (!file.exists())
			throw new FileNotFoundException("Plik " + file.getName() + " nie istnieje");
		fileName = file.getName();
		fileSize = file.length();
		fileAttribute = file.isFile() ? IpmsgTransferredFile.FLAG_FILE_REGULAR
				: IpmsgTransferredFile.FLAG_FILE_DIR;
		headerSize = fileName.length() +
				Long.toHexString(fileSize).length() +
				Long.toHexString(fileAttribute).length() + 8;
	}

	/**
	 * Serializuje obiekt nagłówka do tablicy bajtów.
	 *
	 * @return zserialozowany obiekt nagłówka
	 */
	public byte[] toRawData()
	{
		headerSize = fileName.length() +
				Long.toHexString(fileSize).length() +
				Long.toHexString(fileAttribute).length() + 12;
		if (extendedAttributes != null)
			for(String extendedAttribute: extendedAttributes)
				headerSize += extendedAttribute.length() + 1;
		StringBuilder builder = new StringBuilder("");
		builder.append(String.format("%08x", headerSize));
		builder.append(":");
		builder.append(fileName);
		builder.append(":");
		builder.append(Long.toHexString(fileSize));
		builder.append(":");
		builder.append(Long.toHexString(fileAttribute));

		if (extendedAttributes != null)
			for (String extendedAttribute: extendedAttributes)
			{
				builder.append(":");
				builder.append(extendedAttribute);
			}
		builder.append(":");

		return builder.toString().getBytes(IpmsgPacket.protocolCharset);
	}
}
