package protocols;

import java.util.*;

/**
 * Prywatny pokój rozmów - do komunikacji z tylko jedym kontaktem.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class PrivateChatRoom extends ChatRoom
{
	/**
	 * Kontakt, z którym prowadzona jest rozmowa.
	 */
	private final Contact contact;

	private final ContactObserver contactObserver = new ContactObserver();

	/**
	 * Główny konstruktor.
	 *
	 * @param contact kontakt, z którym prowadzona jest rozmowa
	 */
	public PrivateChatRoom(Contact contact)
	{
		super(getRoomID(contact));
		this.contact = contact;
		super.setTitle(contact.getName());
		contact.addObserver(contactObserver);
	}

	/**
	 * Pobiera identyfikator pokoju związanego z kontaktem.
	 *
	 * @param contact kontakt
	 * @return identyfikator pokoju
	 */
	public static String getRoomID(Contact contact)
	{
		return "priv:" + contact.getID();
	}

	/**
	 * Pobiera kontakt, z którym prowadzona jest rozmowa.
	 *
	 * @return kontakt
	 */
	public Contact getContact()
	{
		return contact;
	}

	/**
	 * Ustawienie tytułu pokoju - nie obsługiwane w przypadku pokojów
	 * prywatnych.
	 * 
	 * @param title nowy tytuł
	 */
	@Override public void setTitle(String title)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Ustawia nową nazwę kontaktu, powiązanego z pokojem.
	 *
	 * @param contactName nazwa kontaktu
	 */
	private void setContactName(String contactName)
	{
		super.setTitle(contactName);
	}

	class ContactObserver implements Observer
	{
		public void update(Observable o, Object arg)
		{
			setContactName(contact.getName());
			notifyObservers();
		}
	}
}
