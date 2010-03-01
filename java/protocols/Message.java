package protocols;

import java.util.Date;

import tools.SimpleObservable;

/**
 * Wiadomość - przychodząca lub wychodząca.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class Message extends SimpleObservable
{
	/**
	 * Liczba zarejestrowanych wiadomości.
	 */
	private static int msgCount = 0;

	/**
	 * Wewnętrzny (unikalny) identyfikator wiadomości.
	 */
	public final int id = msgCount++;

	/**
	 * Data utworzenia (odebrania, lub nadania) wiadomości.
	 */
	public final Date date = new Date();

	/**
	 * Pokój, w ramach którego zarejestrowano wiadomość.
	 */
	protected final ChatRoom room;

	/**
	 * Treść wiadomości.
	 */
	protected String contents;

	/**
	 * Główny konstruktor.
	 *
	 * @param room pokój, w ramach którego zarejestrowano wiadomość
	 */
	public Message(ChatRoom room)
	{
		if (room == null)
			throw new NullPointerException();
		this.room = room;
	}

	/**
	 * Zwraca pokój, w ramach którego zarejestrowano wiadomość.
	 *
	 * @return pokój
	 */
	public ChatRoom getChatRoom()
	{
		return room;
	}

	/**
	 * Zwraca nick autora wiadomości.
	 *
	 * @return autor wiadomości
	 */
	public abstract String getAuthor();

	/**
	 * Zwraca treść wiadomości.
	 *
	 * @return treść wiadomości
	 */
	public String getContents()
	{
		return contents;
	}

	/**
	 * Ustawia treść wiadomości.
	 *
	 * @param contents treść wiadomości
	 */
	public void setContents(String contents)
	{
		if (contents == null)
			throw new NullPointerException();
		this.contents = contents;
	}
}
