package protocols;

import java.util.*;

import tools.*;

/**
 * Kolekcja kontaktów, bez określonego porządku
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ContactList implements Iterable<Contact>
{
	protected ListenableMap<String, Contact> list = new ListenableMap<String, Contact>();

	/**
	 * Przy każdej iteracji po kontaktach trzeba ustawić synchronizację na obiekt listy
	 *
	 * @return Iterator po kontaktach
	 */
	public Iterator<Contact> iterator()
	{
		return list.values().iterator();
	}

	public void add(Contact contact)
	{
		list.put(contact.getID(), contact);
	}

	public void remove(Contact contact)
	{
		list.remove(contact.getID());
	}

	public boolean exists(String id)
	{
		return list.containsKey(id);
	}

	public synchronized Contact get(String id)
	{
		return list.get(id);
	}

	public int getSize()
	{
		return list.size();
	}

	public void addSetListener(SetListener<Contact> listener)
	{
		list.addSetListener(listener);
	}

	public void removeSetListener(SetListener<Contact> listener)
	{
		list.removeSetListener(listener);
	} 
}
