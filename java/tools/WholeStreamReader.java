package tools;

import java.io.*;

/**
 * Klasa do czytania całego strumienia danych.
 * 
 * @author Piotr Gajowiak
 */
public class WholeStreamReader extends Thread
{

	private InputStream is;
	private StringWriter sw;

	/**
	 * Tworzy obiekt na podstawie strumienia wejściowego.
	 *
	 * @param is strumień wejściowy
	 */
	public WholeStreamReader(InputStream is)
	{
		this.is = is;
		sw = new StringWriter();
	}

	@Override public void run()
	{
		try
		{
			int c;
			while ((c = is.read()) != -1)
				sw.write(c);
		}
		catch (IOException e)
		{
		}
	}

	/**
	 * Zwraca wynik czytania ze strumienia.
	 * 
	 * @return wynik czytania ze strumienia
	 */
	public String getResult()
	{
		return sw.toString();
	}
}
