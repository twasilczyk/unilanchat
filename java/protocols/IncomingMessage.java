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

	protected String rawContents;

	public IncomingMessage(ChatRoom room, String author)
	{
		super(room);
		if(author == null)
			throw new NullPointerException();
		this.author = author;
	}

	public String getAuthor()
	{
		return author;
	}

	public void setRawContents(String contents)
	{
		if (contents == null)
			throw new NullPointerException();
		rawContents = contents;
	}

	public String getRawContents()
	{
		if (rawContents == null)
			return getContents();
		return rawContents;
	}

	public boolean isRawContentsDifferent()
	{
		if (rawContents == null)
			return false;
		return !rawContents.trim().equals(this.contents.trim());
	}
}
