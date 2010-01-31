package protocols;

/**
 * Wiadomość przychodząca, czyli odebrana przez jedno z kont, do przeczytania
 * przez użytkownika
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class IncomingMessage extends Message
{
	protected final String author;

	public IncomingMessage(ChatRoom room, String author)
	{
		super(room);
		if(author == null)
			throw new NullPointerException();
		this.author = author;
	}

	@Override public String getAuthor()
	{
		return author;
	}
}
