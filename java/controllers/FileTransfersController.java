package controllers;

/**
 * Kontroler odpowiedzialny za listę (okno) przesyłanych (wysyłanych
 * i odbieranych) plików.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class FileTransfersController
{
	/**
	 * Główny kontroler związany z bieżącym FileTransfersController.
	 */
	protected final MainController mainController;

	/**
	 * Główny konstruktor.
	 *
	 * @param mainController główny kontroler danej instancji aplikacji
	 */
	public FileTransfersController(MainController mainController)
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

	// tu będą metody odpowiedzialne za manipulację transferami (usuwanie itp)
}
