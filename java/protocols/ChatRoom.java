package protocols;

import tools.*;

/**
 * Pokój rozmowy - może być to zarówno pokój prywatny (prywatna rozmowa między
 * dwoma osobami), pokój wieloosobowy, lub pokój ogólny.
 *
 * @todo Przemyśleć trochę strukturę pokoi, przede wszystkim metodę zwracającą
 * wszystkich uczestników rozmowy. Wziąć pod uwagę wieloprotokołowość.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ChatRoom extends SimpleObservable
{
	/**
	 * Identyfikator pokoju.
	 *
	 * Jest on unikalny, w postaci [identyfikator wewnętrzny]@[protokół].
	 * Pokój główny (np. do wysyłania na broadcast) dla wszystkich protokołów
	 * posiada pusty ID.
	 */
	public final String id;

	/**
	 * Tytuł pokoju. Może służyć np. do wyświetlania go w nagłówku rozmowy.
	 */
	protected String title = "";

	/**
	 * Lista wiadomości wysłanych w ramach pokoju.
	 */
	protected ListenableVector<Message> messages = new ListenableVector<Message>();

	/**
	 * @see #getID()
	 * @param id Identyfikator pokoju
	 */
	public ChatRoom(String id)
	{
		this.id = id;
	}

	/**
	 * Zwraca identyfikator pokoju.
	 *
	 * @see #id
	 * @return Identyfikator pokoju
	 */
	public String getID()
	{
		return id;
	}

	/**
	 * Zwraca tytuł pokoju.
	 *
	 * @see #title
	 * @return Tytuł pokoju
	 */
	public String getTitle()
	{
		if (title.isEmpty() && id.isEmpty())
			return "Pokój główny";
		return title;
	}

	/**
	 * Ustawia nowy tytuł pokoju.
	 *
	 * @see #getTitle()
	 * @param title Nowy tytuł pokoju
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
	 * @return Wiadomości w pokoju
	 */
	public ListenableVector<Message> getMessagesVector()
	{
		return messages;
	}

	/**
	 * Powiadamia pokój, że wiadomość (przychodząca) została odebrana.
	 *
	 * @param message Wiadomość przychodząca
	 */
	public void gotMessage(IncomingMessage message)
	{
		if (message.getChatRoom() != this)
			throw new IllegalArgumentException("Wiadomość nie z tego pokoju");
		messages.add(message);
	}

	/**
	 * Powiadamia pokój, że rozpoczęto wysyłanie wiadomości.
	 *
	 * @param message Wiadomość wychodząca
	 */
	public void sentMessage(OutgoingMessage message)
	{
		if (message.getChatRoom() != this)
			throw new IllegalArgumentException("Wiadomość nie do tego pokoju");
		messages.add(message);
	}
}
