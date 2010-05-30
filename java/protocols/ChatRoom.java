package protocols;

import tools.*;

/**
 * Pokój rozmowy - może być to zarówno pokój prywatny (prywatna rozmowa między
 * dwoma osobami), pokój wieloosobowy, lub pokój ogólny.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class ChatRoom extends SimpleObservable
{
	/**
	 * Tytuł pokoju. Może służyć np. do wyświetlania go w nagłówku rozmowy.
	 */
	protected String title = "";

	/**
	 * Lista wiadomości wysłanych w ramach pokoju.
	 */
	protected ListenableVector<Message> messages = new ListenableVector<Message>();

	/**
	 * Główny konstruktor.
	 */
	public ChatRoom()
	{
	}

	/**
	 * Zwraca tytuł pokoju.
	 *
	 * @see #title
	 * @return tytuł pokoju
	 */
	public String getTitle()
	{
		assert(!title.isEmpty());
		return title;
	}

	/**
	 * Ustawia nowy tytuł pokoju.
	 *
	 * @see #getTitle()
	 * @param title nowy tytuł pokoju
	 */
	public void setTitle(String title)
	{
		if (title == null)
			throw new NullPointerException();
		title = title.trim();
		if (this.title.equals(title))
			return;
		this.title = title;
		notifyObservers();
	}

	/**
	 * Zwraca wektor wiadomości w pokoju.
	 *
	 * @return wiadomości w pokoju
	 */
	public ListenableVector<Message> getMessagesVector()
	{
		return messages;
	}

	/**
	 * Powiadamia pokój, że wiadomość (przychodząca) została odebrana.
	 *
	 * @param message wiadomość przychodząca
	 */
	public void gotMessage(IncomingMessage message)
	{
		if (message == null)
			throw new NullPointerException();
		assert(message.getChatRoom() == this);
		messages.add(message);
	}

	/**
	 * Powiadamia pokój, że rozpoczęto wysyłanie wiadomości.
	 *
	 * @param message wiadomość wychodząca
	 */
	public void sentMessage(OutgoingMessage message)
	{
		if (message == null)
			throw new NullPointerException();
		assert(message.getChatRoom() == this);
		messages.add(message);
	}
}
