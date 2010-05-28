package main;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Observable;

import org.w3c.dom.*;

import tools.xml.*;

/**
 * Klasa odpowiadająca za przechowywanie informacji o konfiguracji. Klasy
 * korzystające z konfiguracji tu zawartej, powinny nasłuchiwać zmian w niej.
 *
 * Po wykonaniu serii wywołań setterów, należy wywołać notifyObservers().
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class Configuration extends Observable
{

	/**
	 * Konstruktor prywatny. Oznacza to, że tworzenie obiektów konfiguracji
	 * jest ściśle kontrolowane przez tą klasę.
	 */
	private Configuration()
	{
	}

	/**
	 * Pobiera instancję klasy.
	 *
	 * @return klasa konfiguracji
	 */
	public static Configuration getInstance()
	{
		if (instance == null)
			throw new NullPointerException();
		return instance;
	}

	// <editor-fold defaultstate="collapsed" desc="Serializacja/deserializacja">

	/**
	 * Kodowanie, przy użyciu którego serialozowana jest konfiguracja.
	 */
	protected final static Charset configurationCharset = Charset.forName("utf8");

	private final Document serializationDoc = XMLUtilities.createNewDocument();

	/**
	 * Egzemplarz klasy przechowującej konfigurację. Powinien być jeden na całą
	 * aplikację.
	 */
	protected static Configuration instance;

	/**
	 * Serializuje obiekt konfiguracji do obiektu drzewa XML.
	 * @return drzewo XML reprezentujące konfigurację
	 */
	public Node serialize()
	{
		Element configurationEl = serializationDoc.createElement("configuration");

		Element nickEl = serializationDoc.createElement("nick");
		configurationEl.appendChild(nickEl);
		Text nickVal = serializationDoc.createTextNode(getNick());
		nickEl.appendChild(nickVal);

		Element ignoreAutoResponsesEl = serializationDoc.createElement("ignoreAutoResponses");
		configurationEl.appendChild(ignoreAutoResponsesEl);
		Text ignoreAutoResponsesVal = serializationDoc.createTextNode(ignoreAutoResponses ? "true" : "false");
		ignoreAutoResponsesEl.appendChild(ignoreAutoResponsesVal);

		Element autoUpdateEl = serializationDoc.createElement("autoUpdate");
		configurationEl.appendChild(autoUpdateEl);
		Text autoUpdateVal = serializationDoc.createTextNode(autoUpdate ? "true" : "false");
		autoUpdateEl.appendChild(autoUpdateVal);

		Element debugModeEl = serializationDoc.createElement("debugMode");
		configurationEl.appendChild(debugModeEl);
		Text debugModeVal = serializationDoc.createTextNode(debugMode ? "true" : "false");
		debugModeEl.appendChild(debugModeVal);

		return configurationEl;
	}

	/**
	 * Zwraca obiekt konfiguracji na podstawie drzewa XML zadanego parametrem
	 * 
	 * @param node drzewo XML
	 * @return utworzony obiekt konfiguracji
	 */
	public static Configuration deserialize(Node node)
	{
		Configuration conf = new Configuration();
		if (!node.getNodeName().equals("configuration"))
			throw new IllegalArgumentException("To nie jest węzeł konfiguracji");

		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++)
		{
			if (list.item(i).getNodeType() != Node.ELEMENT_NODE)
				continue;
			Node tempNode = list.item(i);
			if (tempNode.getNodeName().trim().equals("nick"))
				conf.nick = list.item(i).getTextContent().trim();
			else if (tempNode.getNodeName().trim().equals("ignoreAutoResponses"))
			{
				if (tempNode.getTextContent().equals("true"))
					conf.ignoreAutoResponses = true;
				else if (tempNode.getTextContent().equals("false"))
					conf.ignoreAutoResponses = false;
			}
			else if (tempNode.getNodeName().trim().equals("autoUpdate"))
			{
				if (tempNode.getTextContent().equals("true"))
					conf.autoUpdate = true;
				else if (tempNode.getTextContent().equals("false"))
					conf.autoUpdate = false;
			}
			else if (tempNode.getNodeName().trim().equals("debugMode"))
			{
				if (tempNode.getTextContent().equals("true"))
					conf.debugMode = true;
				else if (tempNode.getTextContent().equals("false"))
					conf.debugMode = false;
			}
		}

		return conf;
	}

	/**
	 * Tworzy obiekt konfiguracji na podstawie pliku zadanego parametrem
	 * i ustawia bieżącą zmienną konfiguracji na właśnie wczytaną
	 * 
	 * @param path plik, z którego należy wczytać konfigurację
	 */
	public static void loadInstance(String path)
	{
		if (instance != null)
			throw new RuntimeException("Konfiguracja została już wczytana");
		try
		{
			File file = new File(path);
			RandomAccessFile raf = new RandomAccessFile(new File(path), "r");
			byte[] b = new byte[(int)file.length()];

			raf.readFully(b);
			raf.close();

			String data = new String(b, 0, b.length, configurationCharset);

			instance = deserialize(XMLUtilities.deserializeNode(data));
		}
		catch(FileNotFoundException ex)
		{
			instance = new Configuration();
			return;
		}
		catch(XMLParseException ex)
		{
			instance = new Configuration();
			return;
		}
		catch(IOException ex)
		{
			throw new RuntimeException("Błąd wczytywania pliku konfiguracji", ex);
		}
		catch (IllegalArgumentException ex)
		{
			instance = new Configuration();
		}
	}

	/**
	 * Zapisuje zserializowaną konfigurację do pliku zadanego ścieżką.
	 *
	 * @param path ścieżka do pliku
	 */
	public static void saveInstance(String path)
	{
		String data = XMLUtilities.serializeNode(getInstance().serialize());
		File file = new File(path);
		if(!file.exists())
		{
			try
			{
				file.createNewFile();
			}
			catch(IOException ex)
			{
				throw new RuntimeException("Błąd podczas tworzenia pliku konfiguracji.", ex);
			}
		}

		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(data);
			out.close();
		}
		catch(IOException ex)
		{
			throw new RuntimeException("Błąd podczas zapisywania pliku konfiguracji.", ex);
		}
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Ustawienia ogólne">

	private String nick = null;

	/**
	 * Pobiera nick użytkownika, wykorzystywany jako identyfikator w rozmowach.
	 *
	 * @return nick
	 */
	public String getNick()
	{
		if (nick == null)
			return System.getProperty("user.name", "anonim");
		return nick;
	}

	/**
	 * Ustala nowy nick użytkownika.
	 *
	 * @see #getNick()
	 * @param nick nowy nick
	 */
	public void setNick(String nick)
	{
		if (!((this.nick == null && nick == null) ||
			(nick != null && nick.equals(this.nick))))
			setChanged();
		this.nick = nick;
	}

	private boolean ignoreAutoResponses = true;

	/**
	 * Czy automatyczne odpowiedzi (w protokołach umożliwiających ich
	 * zidentyfikowanie) powinny być ignorowane.
	 *
	 * Niektóre implementacje klientów różnych protokołów pozwalają na ustalenie
	 * automatycznej odpowiedzi, jeżeli użytkownik jest nieobecny. Takie
	 * wiadomości zazwyczaj tylko zaśmiecają okno rozmów.
	 *
	 * @return <code>true</code>, jeżeli automatyczne odpowiedzi powinny być
	 * ignorowane
	 */
	public boolean getIgnoreAutoResponses()
	{
		return ignoreAutoResponses;
	}

	/**
	 * Ustala, czy automatyczne odpowiedzi powinny być ignorowane.
	 *
	 * @param set czy ignorować automatyczne odpowiedzi
	 * @see #getIgnoreAutoResponses()
	 */
	public void setIgnoreAutoResponses(boolean set)
	{
		if (set != ignoreAutoResponses)
			setChanged();
		ignoreAutoResponses = set;
	}

	private boolean autoUpdate = true;

	/**
	 * Czy moduł automatycznych aktualizacji ma być włączony.
	 *
	 * @return <code>true</code>, jeżeli moduł automatycznych aktualizacji jest
	 * włączony
	 */
	public boolean getAutoUpdate()
	{
		return autoUpdate;
	}

	/**
	 * Włącza, lub wyłącza moduł automatycznych aktualizacji.
	 *
	 * @param enabled czy włączyć moduł automatycznych aktualizacji
	 * @see #getAutoUpdate()
	 */
	public void setAutoUpdate(boolean enabled)
	{
		if (enabled != autoUpdate)
			setChanged();
		autoUpdate = enabled;
	}

	private boolean debugMode = false;

	/**
	 * Czy tryb debug ma być włączony.
	 *
	 * @return <code>true</code>, jeżeli tryb debug jest włączony
	 */
	public boolean getDebugMode()
	{
		return debugMode;
	}

	/**
	 * Włącza, lub wyłącza tryb debug.
	 *
	 * @param enabled czy włączyć tryb debug
	 * @see #getDebugMode()
	 */
	public void setDebugMode(boolean enabled)
	{
		if (enabled != debugMode)
			setChanged();
		debugMode = enabled;
	}

	// </editor-fold>

}
