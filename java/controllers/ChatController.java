package controllers;

import protocols.*;

/**
 * Kontroler odpowiedzialny za pokoje rozmów.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ChatController
{
	/**
	 * Główny kontroler związany z bieżącym ChatController.
	 */
	protected final MainController mainController;

	/**
	 * Lista otworzonych pokoi rozmów.
	 */
	protected final ChatRoomList chatRoomList = new ChatRoomList();

	/**
	 * @param mainController Główny kontroler danej instancji aplikacji
	 */
	public ChatController(MainController mainController)
	{
		this.mainController = mainController;
	}

	/**
	 * Zwraca główny kontroler związany z daną instancją aplikacji.
	 *
	 * @return Główny kontroler
	 */
	public MainController getMainController()
	{
		return mainController;
	}

	/**
	 * Zwraca listę otworzonych pokoi rozmów (w danej instancji aplikacji).
	 *
	 * @return Lista otworzonych pokoi rozmów
	 */
	public ChatRoomList getChatRoomList()
	{
		return chatRoomList;
	}

	/**
	 * Zwraca (ewentualnie tworzy) pokój prywatny do rozmowy z danym kontaktem.
	 *
	 * @param contact Kontakt, dla którego chcemy uzyskać pokój
	 * @return Prywatny pokój rozmów
	 */
	public ChatRoom getPrivateChatRoom(Contact contact)
	{
		if (contact == null)
			throw new NullPointerException();
		return chatRoomList.get(contact);
	}

	/**
	 * Zwraca główny pokój rozmów, do komunikacji z wszystkimi kontaktami ze
	 * wszystkich protokołów, na kanale publicznym.
	 *
	 * @return Publiczny pokój rozmów
	 */
	public ChatRoom getMainChatRoom()
	{
		return chatRoomList.getMain();
	}

	/**
	 * Próbuje wysłać wiadomość po kolei za pomocą wszystkich kont.
	 *
	 * @param room Pokój, w którym jest nadawana wiadomość
	 * @param message Treść wiadomości
	 */
	public void sendMessage(ChatRoom room, String message)
	{
		OutgoingMessage msg = new OutgoingMessage(room);
		msg.setContents(message);

		// ryzyko ConcurrentModificationException, w przypadku edycji listy kont
		// w trakcie wysyłania
		
		for (Account acc : mainController.getAccountsVector())
			acc.postMessage(msg);
		room.sentMessage(msg);
	}
}
