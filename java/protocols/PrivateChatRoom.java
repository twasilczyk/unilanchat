package protocols;

import java.util.*;

/**
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class PrivateChatRoom extends ChatRoom implements Observer
{
	protected final Contact contact;

	public PrivateChatRoom(Contact contact)
	{
		super(getRoomID(contact));
		this.contact = contact;
		super.setTitle(contact.getName());
		contact.addObserver(this);
	}

	public static String getRoomID(Contact contact)
	{
		return "priv:" + contact.getID();
	}

	public Contact getContact()
	{
		return contact;
	}

	@Override public void setTitle(String title)
	{
		throw new UnsupportedOperationException();
	}

	public void update(Observable o, Object arg)
	{
		super.setTitle(contact.getName());
		notifyObservers();
	}
}
