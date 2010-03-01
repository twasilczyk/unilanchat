package protocols;

import java.util.*;

import tools.*;

/**
 * Kolekcja kontaktów, bez określonego porządku.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ContactList implements Iterable<Contact>
{
	/**
	 * Opakowywana lista kontaktów.
	 *
	 * Klucz: unikalny identyfikator kontaktu
	 * Wartość: obiekt kontaktu
	 */
	protected ListenableMap<String, Contact> list = new ListenableMap<String, Contact>();

	/**
	 * Pobiera iterator. Przy każdej iteracji po kontaktach trzeba ustawić
	 * synchronizację na obiekt listy.
	 *
	 * @return iterator po kontaktach
	 */
	public Iterator<Contact> iterator()
	{
		return list.values().iterator();
	}

	/**
	 * Dodaje nowy kontakt do listy.
	 *
	 * @param contact kontakt
	 */
	public void add(Contact contact)
	{
		list.put(contact.getID(), contact);
	}

	/**
	 * Usuwa kontakt z listy.
	 *
	 * @param contact kontakt
	 */
	public void remove(Contact contact)
	{
		list.remove(contact.getID());
	}

	/**
	 * Sprawdza, czy kontakt o podanym identyfikatorze jest na liście.
	 *
	 * @param id identyfikator do sprawdzenia
	 * @return <code>true</code>, jeżeli jest na liście
	 */
	public boolean exists(String id)
	{
		return list.containsKey(id);
	}

	/**
	 * Pobiera z listy kontakt o podanym identyfikatorze.
	 *
	 * @param id identyfikator
	 * @return kontakt
	 */
	public synchronized Contact get(String id)
	{
		return list.get(id);
	}

	/**
	 * Pobiera ilość przechowywanych kontaktów.
	 *
	 * @return ilość kontaktów
	 */
	public int getSize()
	{
		return list.size();
	}

	/**
	 * Dodaje słuchacza zbioru kontaktów.
	 *
	 * @param listener słuchacz
	 */
	public void addSetListener(SetListener<Contact> listener)
	{
		list.addSetListener(listener);
	}

	/**
	 * Usuwa słuchacza zbioru kontaktów.
	 *
	 * @param listener słuchacz
	 */
	public void removeSetListener(SetListener<Contact> listener)
	{
		list.removeSetListener(listener);
	} 
}
