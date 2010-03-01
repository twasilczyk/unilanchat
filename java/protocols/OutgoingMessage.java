package protocols;

import java.util.Vector;

/**
 * Wiadomość wychodząca, czyli napisana przez użytkownika.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class OutgoingMessage extends Message
{
	/**
	 * Główny konstruktor.
	 *
	 * @param room pokój, w ramach którego nadano wiadomość
	 */
	public OutgoingMessage(ChatRoom room)
	{
		super(room);
	}

	/**
	 * Ciąg reprezentujący autora - użytkownika korzystającego z programu.
	 */
	protected final String selfName = "Ja";

	/**
	 * Autor wiadomości - zawsze wartość selfName
	 *
	 * @return autor wiadomości - {@link #selfName}
	 */
	public String getAuthor()
	{
		return "Ja";
	}

	// <editor-fold defaultstate="collapsed" desc="Listy odbiorców">

	/**
	 * Lista użytkowników, którzy oczekują na dostarczenie wiadomości.
	 */
	protected final Vector<Contact> receiversPending = new Vector<Contact>();

	/**
	 * Lista użytkowników, do których udało się dostarczyć wiadomość.
	 */
	protected final Vector<Contact> receiversGot = new Vector<Contact>();

	/**
	 * Lista użytkowników, do których nie udało się dostarczyć wiadomości.
	 */
	protected final Vector<Contact> receiversFailed = new Vector<Contact>();

	/**
	 * Dodanie użytkowników do listy odbiorców oczekujących na dostarczenie.
	 *
	 * @param contacts użytkownicy oczekujący
	 */
	public void notifyReceiversAdded(Vector<? extends Contact> contacts)
	{
		receiversPending.addAll(contacts);
		notifyObservers();
	}

	/**
	 * Oznaczenie użytkownika, który odebrał wiadomość.
	 *
	 * @param contact użytkownik
	 */
	public void notifyReceiverGot(Contact contact)
	{
		receiversPending.remove(contact);
		receiversGot.add(contact);
		notifyObservers();
	}

	/**
	 * Oznaczenie użytkowników, którzy nie odebrali wiadomości.
	 *
	 * @param contacts użytkownicy
	 */
	public void notifyReceiversFailed(Vector<? extends Contact> contacts)
	{
		receiversPending.removeAll(contacts);
		receiversFailed.addAll(contacts);
		notifyObservers();
	}

	/**
	 * Pobiera ilość użytkownników, oczekujących na dostarczenie wiadomości.
	 *
	 * @return ilość użytkowników
	 */
	public int getReceiversPendingCount()
	{
		return receiversPending.size();
	}

	/**
	 * Pobiera ilość użytkownników, którzy odebrali wiadomość.
	 *
	 * @return ilość użytkowników
	 */
	public int getReceiversGotCount()
	{
		return receiversGot.size();
	}

	/**
	 * Pobiera ilość użytkownników, do których nie udało się dostarczyć
	 * wiadomości.
	 *
	 * @return ilość użytkowników
	 */
	public int getReceiversFailedCount()
	{
		return receiversFailed.size();
	}

	/**
	 * Pobiera listę użytkownników, oczekujących na dostarczenie wiadomości.
	 *
	 * @return lista użytkowników
	 */
	@SuppressWarnings("unchecked")
	public Vector<Contact> getReceiversPending()
	{
		return (Vector<Contact>)receiversPending.clone(); //TODO: ladniej?
	}

	/**
	 * Pobiera listę użytkownników, którzy odebrali wiadomość.
	 *
	 * @return lista użytkowników
	 */
	@SuppressWarnings("unchecked")
	public Vector<Contact> getReceiversGot()
	{
		return (Vector<Contact>)receiversGot.clone();
	}

	/**
	 * Pobiera listę użytkownników, do których nie udało się dostarczyć
	 * wiadomości.
	 *
	 * @return lista użytkowników
	 */
	@SuppressWarnings("unchecked")
	public Vector<Contact> getReceiversFailed()
	{
		return (Vector<Contact>)receiversFailed.clone();
	}

	// </editor-fold>
}
