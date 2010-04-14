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
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.INDENT, "yes");
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

			return doc.getFirstChild();
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
}
