package tools;

import java.io.*;
import java.net.*;
import java.util.*;

import org.w3c.dom.*;

import tools.xml.*;

/**
 * Klasa służąca do automatycznej aktualizacji programu.
 *
 * @author Piotr Gajowiak
 */
public class Updater extends Observable
{
	/**
	 * Przechowuje adresy źródłowe informujące o aktualizacjach.
	 */
	protected final ArrayList<URL> updateURLs = new ArrayList<URL>();

	/**
	 * Numer ostatnio wykrytej wersji.
	 */
	protected String currentVersion;

	/**
	 * Numer ostatnio wykrytej wersji nightly.
	 */
	protected String nightlyVersion;

	/**
	 * Adres strony, na której można znaleźć najnowszą wersję programu.
	 */
	protected String homepage;

	/**
	 * Limit czasu oczekiwania na połączenie z serwerem.
	 */
	protected final int connectTimeout = 10000;

	/**
	 * Limit czasu oczekiwania na dane od serwera po już połączeniu z nim.
	 */
	protected final int readTimeout = 5000;

	/**
	 * Wątek pobierający informacje o nowej wersji.
	 */
	protected UpdaterThread thread;

	/**
	 * Dodaje URL do listy adresów, z ktorych będzie pobierana informacja
	 * o nowej wersji.
	 *
	 * @param url URL z którego będzie pobierana informacja o wersji
	 */
	public void addUpdateServer(URL url)
	{
		if(url == null)
			throw new NullPointerException();
		synchronized(updateURLs)
		{
			updateURLs.add(url);
		}
	}

	/**
	 * Zwraca numer ostatnio wykrytej wersji.
	 *
	 * @return numer ostatnio wykrytej wersji
	 */
	public String getCurrentVersion()
	{
		return currentVersion;
	}

	/**
	 * Zwraca numer ostatnio wykrytej wersji nightly.
	 *
	 * @return numer ostatnio wykrytej wersji nightly
	 */
	public String getNightlyVersion()
	{
		return nightlyVersion;
	}

	/**
	 * Zwraca adres strony, na której można znaleźć najnowszą wersję programu.
	 *
	 * @return adres strony, na której można znaleźć najnowszą wersję programu
	 */
	public String getHomepage()
	{
		return homepage;
	}

	/**
	 * Sprawdza czy nie pojawiły się aktualizacje, jeśli tak
	 * to powiadamia o tym wszystkich swoich obserwatorów.
	 */
	public synchronized void checkForUpdates()
	{
		if (thread != null && thread.isAlive())
			return;
		thread = new UpdaterThread();
		thread.start();
	}

	class UpdaterThread extends Thread
	{
		public UpdaterThread()
		{
			super("UpdaterThread");
			setDaemon(true);
		}

		@Override public void run()
		{
			synchronized(updateURLs)
			{
				for(URL url : updateURLs)
				{
					try
					{
						URLConnection urlConnection = url.openConnection();

						urlConnection.setConnectTimeout(connectTimeout);
						urlConnection.setReadTimeout(readTimeout);

						BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

						if (!urlConnection.getContentType().contains("xml") &&
							!urlConnection.getContentType().equals("text/plain"))
							continue;
							
						StringBuffer stringBuffer = new StringBuffer();
						char[] buffer = new char[1024];
						int length;

						while ((length = in.read(buffer)) != -1)
							stringBuffer.append(buffer, 0, length);

						in.close();

						Node documentNode = XMLUtilities.deserializeNode(stringBuffer.toString());

						if (!documentNode.getNodeName().trim().equals("updateInfo"))
							continue;

						NodeList nodeList = documentNode.getChildNodes();

						for (int i = 0; i < nodeList.getLength(); i++)
						{
							Node node = nodeList.item(i);

							if (node.getNodeType() != Node.ELEMENT_NODE)
								continue;
							
							if (node.getNodeName().trim().equals("version"))
							{
								NodeList nodeList2 = node.getChildNodes();

								for(int j = 0; j < nodeList2.getLength(); j++)
								{
									Node node2 = nodeList2.item(j);

									if (node2.getNodeType() != Node.ELEMENT_NODE)
										continue;

									if (node2.getNodeName().trim().equals("currentVersion") &&
											!node2.getTextContent().trim().equals(currentVersion))
									{
										currentVersion = node2.getTextContent().trim();

										setChanged();
									}
									else if (node2.getNodeName().trim().equals("nightlyVersion") &&
											!node2.getTextContent().trim().equals(nightlyVersion))
									{
										nightlyVersion = node2.getTextContent().trim();

										setChanged();
									}
								}
							}
							else if (node.getNodeName().trim().equals("homepage"))
								homepage = node.getTextContent().trim();
						}

						if (currentVersion != null && homepage != null)
						{
							if (nightlyVersion == null)
								nightlyVersion = currentVersion;
							notifyObservers();
							break;
						}

					}
					catch (SocketTimeoutException ex)
					{
						// Zbty dlugi czas oczekiwania na dane
					}
					catch (IOException ex)
					{
						// Najprawdopodobniej sie nie polaczono z hostem
					}
					catch (XMLParseException ex)
					{
						// Blad parsowania XML
					}
					
				}
			}
		}
	}
}
