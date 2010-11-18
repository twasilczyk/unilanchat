package tools.xml;

import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Klasa pomocnicza do obsługi XML.
 *
 * @author Piotr Gajowiak
 */
public class XMLUtilities
{

	/**
	 * Zwraca nowo utworzony dokument XML.
	 *
	 * @return utworzony dokument XML
	 */
	public static Document createNewDocument()
	{
		try
		{
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		}
		catch (ParserConfigurationException ex)
		{
			throw new RuntimeException("Nie udało się utworzyć nowego dokumentu DOM", ex);
		}
	}

	/**
	 * Zwraca zserializowany obiekt drzewa XML.
	 *
	 * @param node drzewo XML
	 * @return zserializowany obiekt
	 */
	public static String serializeNode(Node node)
	{
		if (node == null)
			throw new NullPointerException();
		try
		{
			StringWriter sw = new StringWriter();

			TransformerFactory tf = TransformerFactory.newInstance();
			tf.setAttribute("indent-number", new Integer(2));
			Transformer t = tf.newTransformer();
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty(OutputKeys.METHOD, "xml");
			t.transform(new DOMSource(node), new StreamResult(sw));

			return sw.toString();
		}
		catch (TransformerException ex)
		{
			throw new RuntimeException("Nie udało się wygenerować wyniku serializacji", ex);
		}
	}

	/**
	 * Deserializuje dane do drzewa XML.
	 * 
	 * @param data dane do deserializacji
	 * @return drzewo XML
	 */
	public static Node deserializeNode(String data) throws XMLParseException
	{
		try
		{
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			builder.setErrorHandler(new ErrorHandler() {
				public void warning(SAXParseException exception) { }
				public void error(SAXParseException exception) { }
				public void fatalError(SAXParseException exception) { }
			});
			Document doc = builder.parse(new InputSource(new StringReader(data)));

			return doc.getDocumentElement();
		}
		catch(ParserConfigurationException ex)
		{
			throw new RuntimeException("Nie udalo sie skonfigurowac parsera XML", ex);
		}
		catch(SAXException ex)
		{
			throw new XMLParseException(ex);
		}
		catch(IOException ex)
		{
			throw new RuntimeException("Błąd wczytywania pliku konfiguracji", ex);
		}
	}

	/**
	 * Dodaje do węzła element z zawartością tekstową.
	 *
	 * @param parent węzeł, do którego należy dodać element
	 * @param nodeName nazwa nowego elementu
	 * @param contents zawartość nowego elementu
	 * @return nowy element
	 */
	public static Element appendTextNode(Node parent, String nodeName, String contents)
	{
		Document doc = parent.getOwnerDocument();
		
		Element node = doc.createElement(nodeName);
		parent.appendChild(node);

		Text nodeVal = doc.createTextNode(contents);
		node.appendChild(nodeVal);

		return node;
	}

	/**
	 * Dodaje do węzła element z zawartością tekstową typu boolean.
	 *
	 * @param parent węzeł, do którego należy dodać element
	 * @param nodeName nazwa nowego elementu
	 * @param contents zawartość nowego elementu
	 * @return nowy element
	 */
	public static Element appendTextNode(Node parent, String nodeName, boolean contents)
	{
		return XMLUtilities.appendTextNode(parent, nodeName, (contents ? "true" : "false"));
	}

	/**
	 * Dodaje do węzła element z zawartością tekstową typu int.
	 *
	 * @param parent węzeł, do którego należy dodać element
	 * @param nodeName nazwa nowego elementu
	 * @param contents zawartość nowego elementu
	 * @return nowy element
	 */
	public static Element appendTextNode(Node parent, String nodeName, int contents)
	{
		return XMLUtilities.appendTextNode(parent, nodeName, Integer.toString(contents));
	}

	/**
	 * Interpretuje zawartość węzła jako wartość boolean.
	 *
	 * @param node węzeł do odczytania
	 * @param defaultValue wartość domyślna, w przypadku niepowodzenia odczytania
	 * @return odczytana wartość
	 */
	public static Boolean getBoolValue(Node node, Boolean defaultValue)
	{
		String strVal = node.getTextContent().trim();
		if (strVal.equals("true") || strVal.equals("1") || strVal.equals("yes"))
			return true;
		else if (strVal.equals("false") || strVal.equals("0") || strVal.equals("no"))
			return false;
		else
			return defaultValue;
	}

	/**
	 * Interpretuje zawartość węzła jako wartość liczbową.
	 *
	 * @param node węzeł do odczytania
	 * @param defaultValue wartość domyślna, w przypadku niepowodzenia odczytania
	 * @return odczytana wartość
	 */
	public static Integer getIntValue(Node node, Integer defaultValue)
	{
		try
		{
			return Integer.parseInt(node.getTextContent().trim());
		}
		catch (NumberFormatException ex)
		{
			return defaultValue;
		}
	}
}
