package tools;

import java.util.*;

/**
 * Zbiór elementów o określonej kolejności, z możliwością dodania słuchaczy typu
 * {@link SetListener}.
 * Wartości muszą dziedziczyć po klasie {@link Observable} - przekazują tak
 * informację o zmianie swojej zawartości (która jest przekazywana w metodzie
 * {@link SetListener#itemUpdated(Object)}.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ListenableVector<E extends Observable> extends Vector<E>
		implements Observer
{
	@Override public synchronized boolean add(E e)
	{
		super.add(e);
		notifyAdded(e);
		return true;
	}

	@Override public void add(int index, E element)
	{
		super.add(index, element);
		notifyAdded(element);
	}

	@Override public synchronized boolean addAll(Collection<? extends E> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override public synchronized boolean addAll(int index, Collection<? extends E> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override public synchronized void addElement(E obj)
	{
		add(obj);
	}

	@Override public synchronized void clear()
	{
		for (E e : this)
			remove(e);
	}

	@Override public synchronized void insertElementAt(E obj, int index)
	{
		add(index, obj);
	}

	@Override public synchronized boolean remove(Object o)
	{
		if (o == null)
			throw new NullPointerException();
		@SuppressWarnings("unchecked") E observ = (E)o;
		boolean contains = super.remove(observ);
		if (contains)
			notifyRemoved(observ);
		return contains;
	}

	@Override public synchronized E remove(int index)
	{
		E elem = elementAt(index);
		remove(elem);
		return elem;
	}

	@Override public synchronized boolean removeAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override public synchronized void removeAllElements()
	{
		this.clear();
	}

	@Override public synchronized boolean removeElement(Object obj)
	{
		return super.removeElement(obj);
	}

	@Override public synchronized void removeElementAt(int index)
	{
		super.removeElementAt(index);
	}

	@Override protected synchronized void removeRange(int fromIndex, int toIndex)
	{
		throw new UnsupportedOperationException();
	}

	@Override public synchronized boolean retainAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	// <editor-fold defaultstate="collapsed" desc="Obsługa komunikatów od obiektów">

	public void update(Observable o, Object arg)
	{
		if (o == null)
			throw new NullPointerException();
		@SuppressWarnings("unchecked") E observ = (E)o;
		for (SetListener<E> listener : setListeners)
			listener.itemUpdated(observ);
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Obsługa SetListener">

	/**
	 * Zbiór obserwatorów kolekcji.
	 */
	protected final Vector<SetListener<E>> setListeners =
			new Vector<SetListener<E>>();

	/**
	 * Dodaje nowego obserwatora kolekcji do listy powiadamianych.
	 *
	 * Nie powinno się dodawać słuchaczy, jak już zaczynają być odbierane
	 * komunikaty - możliwość wystąpienia ConcurrentModificationException.
	 *
	 * @param listener słuchacz do dodania
	 */
	public void addSetListener(SetListener<E> listener)
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
	public void removeSetListener(SetListener<E> listener)
	{
		if (listener == null)
			throw new NullPointerException();
		setListeners.remove(listener);
	}

	/**
	 * Powiadamia słuchaczy o dodaniu nowego elementu, oraz sam dodaje się do
	 * listy jego słuchaczy.
	 *
	 * @param elem dodany element
	 */
	protected void notifyAdded(E elem)
	{
		if (elem == null)
			return;
		elem.addObserver(this);
		for (SetListener<E> listener : setListeners)
			listener.itemAdded(elem);
	}

	/**
	 * Powiadamia słuchaczy o usunięciu danego elementu, oraz sam usuwa się
	 * z listy jego słuchaczy.
	 *
	 * @param elem usunięty element
	 */
	protected void notifyRemoved(E elem)
	{
		if (elem == null)
			return;
		elem.deleteObserver(this);
		for (SetListener<E> listener : setListeners)
			listener.itemRemoved(elem);
	}

	// </editor-fold>
}
