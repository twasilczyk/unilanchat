package protocols.ipmsg;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Nagłówek reprezentujący kombinację nagłówków żądania wysłania pliku.
 *
 * @author Piotr Gajowiak
 */
class IpmsgFileListSendRequestHeader
{
	/**
	 * Zbiór nagłówków żądania wysłania pliku.
	 */
	protected ArrayList<IpmsgFileSendRequestHeader> headerList = new ArrayList<IpmsgFileSendRequestHeader>();

	/**
	 * Kontruktor nagłówka dla pakietów odbieranych.
	 *
	 * @param data dane, które należy zparsować
	 * @return obiekt nagłówka
	 */
	public static IpmsgFileListSendRequestHeader fromRawData(String data)
	{
		String[] lines = data.split("\007");

		IpmsgFileListSendRequestHeader fileListHeader = new IpmsgFileListSendRequestHeader();

		for(String line: lines)
		{
			try
			{
				fileListHeader.headerList.add(IpmsgFileSendRequestHeader.fromRawData(line));
			}
			catch(IllegalArgumentException ex)
			{
				throw new IllegalArgumentException("Blad przy parsowaniu naglowkow listy plikow", ex);
			}
		}

		return fileListHeader;
	}

	/**
	 * Konstruktor nagłówka dla pakietów wysyłanych.
	 *
	 * @param sentFiles pliki, które chcemy wysłać
	 */
	public IpmsgFileListSendRequestHeader(Vector<IpmsgSentFile> sentFiles)
	{
		if(sentFiles == null)
			throw new NullPointerException();
		for(IpmsgSentFile file: sentFiles)
			headerList.add(new IpmsgFileSendRequestHeader(file));
	}

	protected IpmsgFileListSendRequestHeader()
	{
	}

	@Override public String toString()
	{
		StringBuilder builder = new StringBuilder("");

		for(IpmsgFileSendRequestHeader header: headerList)
		{
			builder.append(header.toString());
			builder.append("\007");
		}

		return builder.toString();
	}
}
