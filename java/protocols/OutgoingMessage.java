package protocols;

import java.util.Vector;

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

	public String getAuthor()
	{
		return "Ja";
	}

	// <editor-fold defaultstate="collapsed" desc="Listy odbiorców">

	protected final Vector<Contact> receiversPending = new Vector<Contact>();
	protected final Vector<Contact> receiversGot = new Vector<Contact>();
	protected final Vector<Contact> receiversFailed = new Vector<Contact>();

	public void notifyReceiversAdded(Vector<? extends Contact> contacts)
	{
		receiversPending.addAll(contacts);
		notifyObservers();
	}

	public void notifyReceiverGot(Contact contact)
	{
		receiversPending.remove(contact);
		receiversGot.add(contact);
		notifyObservers();
	}

	public void notifyReceiversFailed(Vector<? extends Contact> contacts)
	{
		receiversPending.removeAll(contacts);
		receiversFailed.addAll(contacts);
		notifyObservers();
	}

	public int getReceiversPendingCount()
	{
		return receiversPending.size();
	}

	public int getReceiversGotCount()
	{
		return receiversGot.size();
	}

	public int getReceiversFailedCount()
	{
		return receiversFailed.size();
	}

	@SuppressWarnings("unchecked")
	public Vector<Contact> getReceiversPending()
	{
		return (Vector<Contact>)receiversPending.clone(); //TODO: ladniej?
	}

	@SuppressWarnings("unchecked")
	public Vector<Contact> getReceiversGot()
	{
		return (Vector<Contact>)receiversGot.clone();
	}

	@SuppressWarnings("unchecked")
	public Vector<Contact> getReceiversFailed()
	{
		return (Vector<Contact>)receiversFailed.clone();
	}

	// </editor-fold>
}
