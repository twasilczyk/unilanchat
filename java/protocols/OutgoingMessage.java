package protocols;

/**
 * Wiadomość wychodząca, czyli napisana przez użytkownika
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class OutgoingMessage extends Message
{
	public OutgoingMessage(ChatRoom room)
	{
		super(room);
	}

	@Override public String getAuthor()
	{
		return "Ja";
	}
}
