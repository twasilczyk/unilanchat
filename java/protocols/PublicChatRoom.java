package protocols;

/**
 * Prywatny pokój rozmów - do komunikacji ze wszystkimi.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class PublicChatRoom extends ChatRoom
{
	/**
	 * Główny konstruktor.
	 */
	public PublicChatRoom()
	{
		super();
		super.setTitle("Pokój główny");
	}

	/**
	 * Ustawienie tytułu pokoju - nie obsługiwane w przypadku pokoju
	 * publicznego.
	 *
	 * @param title nowy tytuł
	 */
	@Override public void setTitle(String title)
	{
		throw new UnsupportedOperationException();
	}
}
