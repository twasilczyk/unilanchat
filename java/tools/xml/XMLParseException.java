package tools.xml;

/**
 * Wyjątek do przekazywania błędów związanych z parsowaniem XML.
 *
 * @author Piotr Gajowiak
 */
public class XMLParseException extends Exception
{
	public XMLParseException(Throwable cause)
	{
		super(cause);
	}	
}
