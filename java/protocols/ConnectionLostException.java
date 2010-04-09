package protocols;

/**
 * Wyjątek oznaczający próbę komunikacji po utracie połączenia: zamknięciu
 * gniazda UDP, lub sesji TCP.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ConnectionLostException extends RuntimeException
{
	public ConnectionLostException()
	{
		super("Próba wysłania pakietu bez połączenia");
	}
}
