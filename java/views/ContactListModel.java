package views;

import java.text.Collator;
import java.util.*;
import javax.swing.*;

import tools.SetListener;
import protocols.*;

/**
 * Model przedstawiający listę kontaktów w wybranym porządku
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ContactListModel extends AbstractListModel implements SetListener<Contact>
{
	protected final ContactList contactList;
	protected final ArrayList<Contact> orderedContactList = new ArrayList<Contact>();

	protected final static Comparator<Contact> orderByName = new ContactOrderByName();
	protected final static Comparator<Contact> orderByStatus = new ContactOrderByStatus();

	protected Comparator<Contact> orderComparator = orderByStatus;

	public ContactListModel(ContactList contactList)
	{
		if (contactList == null)
			throw new NullPointerException();
		this.contactList = contactList;
		contactList.addSetListener(this);
	}

	public int getSize()
	{
		return orderedContactList.size();
	}

	public Contact getElementAt(int index)
	{
		try
		{
			return orderedContactList.get(index);
		}
		catch (IndexOutOfBoundsException e) // występuje chyba tylko na Windowsie
		{
			refreshList(); // da się tego uniknąć?
			return null;
		}
	}

	public void itemAdded(Contact contact)
	{
		if (contact == null)
			throw new NullPointerException();
		synchronized (orderedContactList)
		{
			orderedContactList.add(contact);
			Collections.sort(orderedContactList, orderComparator);
		}
		refreshList();
	}

	public void itemRemoved(Contact contact)
	{
		if (contact == null)
			throw new NullPointerException();
		synchronized (orderedContactList)
		{
			orderedContactList.remove(contact);
		}
		refreshList();
	}

	public void itemUpdated(Contact contact)
	{
		if (contact == null)
			throw new NullPointerException();
		synchronized (orderedContactList)
		{
			Collections.sort(orderedContactList, orderComparator);
		}
		refreshList();
	}

	protected void refreshList()
	{
		final ContactListModel model = this;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				fireContentsChanged(model, 0, Integer.MAX_VALUE);
			}
		});
	}
}

/**
 * Porządek listy kontaktów według nazwy
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
class ContactOrderByName implements Comparator<Contact>
{
	public int compare(Contact o1, Contact o2)
	{
		return Collator.getInstance().compare(o1.getName(), o2.getName());
	}
}

/**
 * Porządek listy kontaktów według statusu, następnie według nazwy
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
class ContactOrderByStatus implements Comparator<Contact>
{
	public int compare(Contact o1, Contact o2)
	{
		if (o1.getStatus().equals(o2.getStatus()))
			return Collator.getInstance().compare(o1.getName(), o2.getName());
		return o1.getStatus().compareTo(o2.getStatus());
	}
}
