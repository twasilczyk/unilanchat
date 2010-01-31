package protocols;

import tools.SimpleObservable;

/**
 * Abstrakcyjna wiadomość, przychodząca lub wychodząca
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class Message extends SimpleObservable
{
	protected final ChatRoom room;
	protected String contents;

	public Message(ChatRoom room)
	{
		if (room == null)
			throw new NullPointerException();
		this.room = room;
	}

	public ChatRoom getChatRoom()
	{
		return room;
	}

	public abstract String getAuthor();

	public String getContents()
	{
		return contents;
	}

	public void setContents(String contents)
	{
		if (contents == null)
			throw new NullPointerException();
		this.contents = contents;
	}
}
