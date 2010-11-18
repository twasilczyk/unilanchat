package main;

import java.awt.*;
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

		XMLUtilities.appendTextNode(configurationEl, "nick", getNick());
		XMLUtilities.appendTextNode(configurationEl, "ignoreAutoResponses", ignoreAutoResponses);
		XMLUtilities.appendTextNode(configurationEl, "autoUpdate", autoUpdate);
		XMLUtilities.appendTextNode(configurationEl, "debugMode", debugMode);

		if (mainViewDimensions != null)
			configurationEl.appendChild(mainViewDimensions.serialize(
				serializationDoc, "mainViewDimensions"));

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
			Node currentNode = list.item(i);
			String currentNodeName = currentNode.getNodeName();
			
			if (currentNodeName.equals("nick"))
				conf.nick = currentNode.getTextContent().trim();
			else if (currentNodeName.equals("ignoreAutoResponses"))
				conf.ignoreAutoResponses = XMLUtilities.getBoolValue(currentNode,
					conf.ignoreAutoResponses);
			else if (currentNodeName.equals("autoUpdate"))
				conf.autoUpdate = XMLUtilities.getBoolValue(currentNode,
					conf.autoUpdate);
			else if (currentNodeName.equals("debugMode"))
				conf.debugMode = XMLUtilities.getBoolValue(currentNode,
					conf.debugMode);
			else if (currentNodeName.equals("mainViewDimensions"))
				conf.mainViewDimensions = WindowDimensions.deserialize(currentNode);
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
			BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file), configurationCharset));
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

	private WindowDimensions mainViewDimensions = null;

	/**
	 * Pobiera zapisaną konfigurację wymiarów okna głównego.
	 *
	 * @return obiekt konfiguracji wymiarów okna programu lub <code>null</code>,
	 * jeżeli ma być domyślna
	 */
	public WindowDimensions getMainViewDimensions()
	{
		return mainViewDimensions;
	}

	/**
	 * Ustawia konfigurację wymiarów okna głównego.
	 *
	 * @param mainViewDimensions wymiary okna głównego
	 */
	public void setMainViewDimensions(WindowDimensions mainViewDimensions)
	{
		if (mainViewDimensions == null)
			throw new NullPointerException();
		this.mainViewDimensions = mainViewDimensions;
	}

	// </editor-fold>

	/**
	 * Klasa przechowująca położenie i wymiary okna
	 */
	public static class WindowDimensions
	{
		/**
		 * Odległość okna od lewej krawędzi ekranu.
		 */
		public final int left;
		
		/**
		 * Odległość okna od górnej krawędzi ekranu.
		 */
		public final int top;

		/**
		 * Szerokość okna.
		 */
		public final int width;

		/**
		 * Wysokość okna.
		 */
		public final int height;

		public WindowDimensions(int left, int top, int width, int height)
		{
			this.left = left;
			this.top = top;
			this.width = width;
			this.height = height;
		}

		/**
		 * Serializuje obiekt wymiarów okna do obiektu drzewa XML.
		 * @return drzewo XML reprezentujące wymiary okna
		 */
		public Node serialize(Document serializationDoc, String elementName)
		{
			Element winDimEl = serializationDoc.createElement(elementName);

			XMLUtilities.appendTextNode(winDimEl, "left", left);
			XMLUtilities.appendTextNode(winDimEl, "top", top);
			XMLUtilities.appendTextNode(winDimEl, "width", width);
			XMLUtilities.appendTextNode(winDimEl, "height", height);

			return winDimEl;
		}

		/**
		 * Zwraca obiekt wymiarów okna na podstawie drzewa XML zadanego parametrem
		 *
		 * @param node drzewo XML
		 * @return utworzony obiekt wymiarów okna
		 */
		public static WindowDimensions deserialize(Node node)
		{
			Integer left, top, width, height;
			left = top = width = height = null;

			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength(); i++)
			{
				Node currentNode = list.item(i);
				String currentNodeName = currentNode.getNodeName();

				if (currentNodeName.equals("left"))
					left = XMLUtilities.getIntValue(currentNode, null);
				else if(currentNodeName.equals("top"))
					top = XMLUtilities.getIntValue(currentNode, null);
				else if (currentNodeName.equals("width"))
					width = XMLUtilities.getIntValue(currentNode, null);
				else if (currentNodeName.equals("height"))
					height = XMLUtilities.getIntValue(currentNode, null);
			}

			try
			{
				return new WindowDimensions(left, top, width, height);
			}
			catch (NullPointerException ex) // jeżeli jakaś wartość nie będzie podana
			{
				return null;
			}
		}

		/**
		 * Przycina wymiary okna do rozmiaru widocznego ekranu.
		 *
		 * @return obiekt z przyciętymi wymiarami
		 */
		public WindowDimensions shrinkToVisible()
		{
			Dimension scrDim = Toolkit.getDefaultToolkit().getScreenSize();

			if (scrDim == null || scrDim.width <= 0 || scrDim.height <= 0)
				return this;

			int sLeft = this.left;
			int sTop = this.top;
			int sWidth = this.width;
			int sHeight = this.height;

			if (sWidth > scrDim.width)
				sWidth = scrDim.width;
			if (sHeight > scrDim.height)
				sHeight = scrDim.height;
			if (sLeft + sWidth > scrDim.width)
				sLeft = scrDim.width - sWidth;
			if (sTop + sHeight > scrDim.height)
				sTop = scrDim.height - sHeight;

			return new WindowDimensions(sLeft, sTop, sWidth, sHeight);
		}
	}

}
