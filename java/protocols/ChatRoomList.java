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
	protected ListenableMap<String, ChatRoom> list = new ListenableMap<String, ChatRoom>();

	/**
	 * Zwraca iterator dla danej listy pokoi. Przy każdej iteracji po pokojach
	 * trzeba ustawić synchronizację na obiekt listy.
	 *
	 * @return Iterator po kontaktach
	 */
	public Iterator<ChatRoom> iterator()
	{
		return list.values().iterator();
	}

	/**
	 * Dodaje nowy pokój do listy.
	 *
	 * @param chatRoom Dodawany pokój
	 */
	public void add(ChatRoom chatRoom)
	{
		list.put(chatRoom.getID(), chatRoom);
	}

	/**
	 * Usuwa pokój z listy.
	 *
	 * @param chatRoom Usuwany pokój
	 */
	public void remove(ChatRoom chatRoom)
	{
		list.remove(chatRoom.getID());
	}

	/**
	 * Sprawdza, czy dany pokój istnieje na liście.
	 *
	 * @param id Identyfikator sprawdzanego pokoju
	 * @return Czy lista zawiera pokój o danym identyfikatorze
	 */
	public boolean exists(String id)
	{
		return list.containsKey(id);
	}

	/**
	 * Pobiera pokój o danym identyfikatorze z listy. Jeżeli nie istnieje -
	 * tworzy go.
	 *
	 * @param id Identyfikator pokoju do pobrania
	 * @return Wybrany pokój rozmów
	 */
	public synchronized ChatRoom get(String id)
	{
		if (!exists(id))
		{
			ChatRoom newRoom = new ChatRoom(id);
			add(newRoom);
			return newRoom;
		}
		return list.get(id);
	}

	/**
	 * Pobiera z listy pokój prywatny, do rozmowy z danym kontaktem. Jeżeli
	 * pokój nie istnieje - tworzy go.
	 *
	 * @param contact Kontakt, dla którego ma zostać pobrany pokój
	 * @return Pokój prywatny
	 */
	public synchronized PrivateChatRoom get(Contact contact)
	{
		String roomID = PrivateChatRoom.getRoomID(contact);
		if (!exists(roomID))
		{
			PrivateChatRoom newRoom = new PrivateChatRoom(contact);
			add(newRoom);
			return newRoom;
		}
		return (PrivateChatRoom)list.get(roomID); //powinien być właśnie tego typu!
	}

	/**
	 * Pobiera główny pokój rozmów.
	 *
	 * @return Główny pokój rozmów
	 */
	public ChatRoom getMain()
	{
		return get("");
	}

	/**
	 * Zwraca ilość pokoi na liście.
	 *
	 * @return Ilość pokoi na liście
	 */
	public int getSize()
	{
		return list.size();
	}

	/**
	 * Dodaje obserwatora zbioru pokoi do listy powiadamianych
	 *
	 * @param listener Obserwator do dodania
	 */
	public void addSetListener(SetListener<ChatRoom> listener)
	{
		list.addSetListener(listener);
	}

	/**
	 * Usuwa obserwatora zbioru pokoi z listy powiadamianych
	 *
	 * @param listener Obserwator do usunięcia
	 */
	public void removeSetListener(SetListener<ChatRoom> listener)
	{
		list.removeSetListener(listener);
	} 
}
