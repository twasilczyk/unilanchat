package tools;

import java.util.*;

/**
 * Zbiór par klucz - wartość, z możliwością dodania słuchaczy typu SetListener.
 * Wartości muszą dziedziczyć po klasie Observable - przekazują tak informację
 * o zmianie swojej zawartości (która jest przekazywana w metodzie
 * SetListener.itemUpdated()).
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 * @see SetListener
 */
public class ListenableMap<K, V extends Observable> extends Hashtable<K, V>
		implements Observer
{
	/**
	 * Dodanie elementu do kolekcji.
	 *
	 * @param key klucz elementu
	 * @param value wartość do dodania
	 * @return poprzednia wartość zapisana pod podanym kluczem, lub null, jeżeli
	 * nie było
	 */
	@Override public V put(K key, V value)
	{
		V r;
		synchronized (this)
		{
			r = super.put(key, value);
			value.addObserver(this);
		}

		for (SetListener<V> listener : setListeners)
			listener.itemAdded(value);
		
		return r;
	}

	/**
	 * Dodanie wielu elementów do kolekcji.
	 *
	 * @param m kolekcja z elementami do dodania
	 */
	@Override public void putAll(Map<? extends K, ? extends V> m)
	{
		for (K elem : m.keySet())
			put(elem, m.get(elem));
	}

	/**
	 * Usuwa element o podanym kluczu z kolekcji.
	 *
	 * @param key klucz do usunięcia
	 * @return element, który znajdował się pod podanym kluczem, lub null,
	 * jeżeli nie istniał
	 */
	@Override public V remove(Object key)
	{
		@SuppressWarnings("unchecked") K k = (K)key;
		V r;
		synchronized (this)
		{
			r = super.remove(k);
			if (r != null)
				r.deleteObserver(this);
		}

		if (r != null)
			for (SetListener<V> listener : setListeners)
				listener.itemRemoved(r);
		return r;
	}

	/**
	 * Usuwa wszystkie elementy z kolekcji.
	 */
	@Override public synchronized void clear()
	{
		for (K key : keySet())
			remove(key);
	}

	// <editor-fold defaultstate="collapsed" desc="Obsługa komunikatów od obiektów">

	/**
	 * Obsługa komunikatów od elementów znajdujących się w kolekcji. Metoda nie
	 * powinna być wywoływana spoza kolekcji.
	 *
	 * @param o obiekt z kolekcji, którego dotyczy powiadomienie
	 * @param arg parametry (ignorowane)
	 */
	public void update(Observable o, Object arg)
	{
		if (o == null)
			throw new NullPointerException();
		@SuppressWarnings("unchecked") V observ = (V)o;
		for (SetListener<V> listener : setListeners)
			listener.itemUpdated(observ);
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Obsługa SetListener">

	/**
	 * Zbiór obserwatorów kolekcji.
	 */
	protected final Vector<SetListener<V>> setListeners =
			new Vector<SetListener<V>>();

	/**
	 * Dodaje nowego obserwatora kolekcji do listy powiadamianych.
	 *
	 * Nie powinno się dodawać słuchaczy, jak już zaczynają być odbierane
	 * komunikaty - możliwość wystąpienia ConcurrentModificationException.
	 *
	 * @param listener słuchacz do dodania
	 */
	public void addSetListener(SetListener<V> listener)
	{
		if (listener == null)
			throw new NullPointerException();
		synchronized (setListeners)
		{
			if (!setListeners.contains(listener))
				setListeners.add(listener);
		}
	}

	/**
	 * Usuwa obserwatora kolekcji z listy powiadamianych.
	 *
	 * Nie powinno się usuwać słuchaczy, gdy są odbierane komunikaty
	 * - możliwość wystąpienia ConcurrentModificationException.
	 *
	 * @param listener słuchacz do usunięcia
	 */
	public void removeSetListener(SetListener<V> listener)
	{
		if (listener == null)
			throw new NullPointerException();
		setListeners.remove(listener);
	}

	// </editor-fold>

}
