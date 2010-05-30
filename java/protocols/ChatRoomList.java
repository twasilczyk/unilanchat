package protocols;

import java.util.*;

import tools.*;

/**
 * Kolekcja pokoi rozmów, bez określonego porządku. Pokój główny ma pusty
 * identyfikator.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ChatRoomList implements Iterable<ChatRoom>
{
	/**
	 * Opakowywana lista pokoi.
	 */
	protected ListenableVector<ChatRoom> list = new ListenableVector<ChatRoom>();

	/**
	 * Pokój główny.
	 */
	protected PublicChatRoom mainRoom = null;

	/**
	 * Zwraca iterator dla danej listy pokoi. Przy każdej iteracji po pokojach
	 * trzeba ustawić synchronizację na obiekt listy.
	 *
	 * @return iterator po kontaktach
	 */
	public Iterator<ChatRoom> iterator()
	{
		return list.iterator();
	}

	/**
	 * Dodaje nowy pokój do listy.
	 *
	 * @param chatRoom dodawany pokój
	 */
	public void add(ChatRoom chatRoom)
	{
		if (chatRoom instanceof PublicChatRoom)
		{
			synchronized(this)
			{
				if (mainRoom != null && mainRoom != chatRoom)
					throw new IllegalArgumentException("Nie można dodać dwóch pokoi publicznych.");
				mainRoom = (PublicChatRoom)chatRoom;
				list.add(chatRoom);
			}
		}
		else
			list.add(chatRoom);
	}

	/**
	 * Usuwa pokój z listy.
	 *
	 * @param chatRoom usuwany pokój
	 */
	public void remove(ChatRoom chatRoom)
	{
		if (chatRoom instanceof PublicChatRoom)
		{
			synchronized(this)
			{
				if (mainRoom == chatRoom)
					mainRoom = null;
				list.remove(chatRoom);
			}
		}
		else
			list.remove(chatRoom);
	}

	/**
	 * Sprawdza, czy dany pokój istnieje na liście.
	 *
	 * @param chatRoom sprawdzany pokój
	 * @return <code>true</code>, jeżeli pokój jest na liście
	 */
	public boolean contains(ChatRoom chatRoom)
	{
		return list.contains(chatRoom);
	}

	/**
	 * Pobiera główny pokój rozmów.
	 *
	 * @return główny pokój rozmów
	 */
	public PublicChatRoom getMain()
	{
		if (mainRoom != null)
			return mainRoom;

		synchronized(this)
		{
			if (mainRoom != null)
				return mainRoom;
			PublicChatRoom r = new PublicChatRoom();
			add(r);
			return r;
		}
	}

	/**
	 * Zwraca ilość pokoi na liście.
	 *
	 * @return ilość pokoi
	 */
	public int getSize()
	{
		return list.size();
	}

	/**
	 * Dodaje obserwatora zbioru pokoi do listy powiadamianych.
	 *
	 * @param listener obserwator do dodania
	 */
	public void addSetListener(SetListener<ChatRoom> listener)
	{
		list.addSetListener(listener);
	}

	/**
	 * Usuwa obserwatora zbioru pokoi z listy powiadamianych.
	 *
	 * @param listener obserwator do usunięcia
	 */
	public void removeSetListener(SetListener<ChatRoom> listener)
	{
		list.removeSetListener(listener);
	} 
}
