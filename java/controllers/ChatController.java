package controllers;

import main.Main;
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
	 * Główny konstruktor.
	 *
	 * @param mainController główny kontroler danej instancji aplikacji
	 */
	public ChatController(MainController mainController)
	{
		this.mainController = mainController;
	}

	/**
	 * Zwraca główny kontroler związany z daną instancją aplikacji.
	 *
	 * @return główny kontroler
	 */
	public MainController getMainController()
	{
		return mainController;
	}

	/**
	 * Zwraca listę otworzonych pokoi rozmów (w danej instancji aplikacji).
	 *
	 * @return lista otworzonych pokoi rozmów
	 */
	public ChatRoomList getChatRoomList()
	{
		return chatRoomList;
	}

	/**
	 * Zwraca (ewentualnie tworzy) pokój prywatny do rozmowy z danym kontaktem.
	 *
	 * @param contact kontakt, dla którego chcemy uzyskać pokój
	 * @return prywatny pokój rozmów
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
	 * @return publiczny pokój rozmów
	 */
	public ChatRoom getMainChatRoom()
	{
		return chatRoomList.getMain();
	}

	/**
	 * Próbuje wysłać wiadomość po kolei za pomocą wszystkich kont.
	 *
	 * @param room pokój, w którym jest nadawana wiadomość
	 * @param message treść wiadomości
	 */
	public void sendMessage(ChatRoom room, String message)
	{
		final OutgoingMessage msg = new OutgoingMessage(room);
		msg.setContents(message);

		room.sentMessage(msg);

		Main.backgroundProcessing.invokeLater(new Runnable()
		{
			public void run()
			{
				// ryzyko ConcurrentModificationException, w przypadku edycji listy kont
				// w trakcie wysyłania
				for (Account acc : mainController.getAccountsVector())
					acc.postMessage(msg);
			}
		});
	}
}
