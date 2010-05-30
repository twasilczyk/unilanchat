package protocols;

import tools.*;

/**
 * Kontakt (inny użytkownik) przechowywany na liście kontaktów.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class Contact extends SimpleObservable
{
	/**
	 * Możliwe statusy użytkowników na liście kontaktów.
	 */
	public enum UserStatus
	{
		/**
		 * Użytkownik jest dostępny.
		 */
		ONLINE,

		/**
		 * Użytkownik jest dostępny, ale zajęty.
		 */
		BUSY,

		/**
		 * Użytkownik jest niedostępny.
		 */
		OFFLINE
	}

	/**
	 * Konto, z którym powiązany jest kontakt.
	 */
	protected final Account account;

	protected final Contact thisContact = this;

	/**
	 * Pokój prywatny, do rozmów z kontaktem.
	 */
	protected PrivateChatRoom privateChatRoom;

	public Contact(Account account)
	{
		if (account == null)
			throw new NullPointerException();
		this.account = account;

		getAccount().chatRoomList.addSetListener(new ChatRoomListener());
	}

	/**
	 * Zwraca pełny identyfikator użytkownika,
	 * w formie <code>[unikalny-tekst]@[nazwa-protokołu]</code>.
	 *
	 * @return pełny identyfikator użytkownika
	 */
	public abstract String getID();

	/**
	 * Zwraca nazwę kontaktu (np. do wyświetlania).
	 *
	 * @return nazwa kontaktu
	 */
	public abstract String getName();

	/**
	 * Zwraca grupę, do której jest zapisany użytkownik (kontakt). Metoda działa
	 * tylko, jeżeli protokół obsługuje grupy.
	 *
	 * @return grupa kontaktu
	 */
	public abstract String getGroup();

	/**
	 * Zwraca konto związane z kontaktem.
	 *
	 * @return konto
	 */
	public abstract Account getAccount();

	/**
	 * Zwraca aktualny status kontaktu.
	 *
	 * @return status kontaktu
	 */
	public abstract UserStatus getStatus();

	/**
	 * Zwraca tekstowy opis statusu.
	 *
	 * @return tekstowy opis statusu, lub <code>null</code>, jeżeli nie
	 * ustawiony
	 */
	public abstract String getTextStatus();

	/**
	 * Pobiera prywatny pokój do rozmów z tym kontaktem. Jeżeli chcesz
	 * zagwarantować, że pokój nie zostanie odłączony od tego kontaktu w czasie
	 * korzystania z niego, musisz założyć synchronizację.
	 *
	 * @return pokój prywatny do rozmów z kontaktem
	 */
	public PrivateChatRoom getPrivateChatRoom()
	{
		PrivateChatRoom room = privateChatRoom;
		if (room != null)
			return room;
		synchronized(this)
		{
			privateChatRoom = new PrivateChatRoom(this);
			getAccount().chatRoomList.add(privateChatRoom);
			return privateChatRoom;
		}
	}

	class ChatRoomListener implements SetListener<ChatRoom>
	{
		public void itemAdded(ChatRoom item) {}
		public void itemUpdated(ChatRoom item) { }

		public void itemRemoved(ChatRoom item)
		{
			if (!(item instanceof PrivateChatRoom))
				return;
			PrivateChatRoom privRoom = (PrivateChatRoom)item;
			if (privRoom.getContact() != thisContact)
				return;

			// nasz pokój został usunięty z listy
			synchronized(thisContact)
			{
				if (privateChatRoom != privRoom)
					return;
				assert(!getAccount().chatRoomList.contains(item));
				privateChatRoom = null;
			}
		}
	}

	/**
	 * Zamienia listę kontaktów w tekstową listę ich nazw.
	 *
	 * @param contacts lista kontaktów
	 * @param glue separator łączący nazwy
	 * @return połączona lista
	 */
	public static String join(Iterable<Contact> contacts, String glue)
	{
		StringBuilder joined = new StringBuilder();
		boolean first = true;
		for (Contact c : contacts)
		{
			if (first)
				first = false;
			else
				joined.append(glue);
			joined.append(c.getName());
		}
		return joined.toString();
	}
}
