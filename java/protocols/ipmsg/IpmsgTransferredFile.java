package protocols.ipmsg;

import java.io.File;
import tools.SimpleObservable;

/**
 * Klasa abstrakcyjna reprezentująca transferowany plik.
 *
 * @author Piotr Gajowiak
 */
public abstract class IpmsgTransferredFile extends SimpleObservable
{
	/**
	 * Flaga oznaczająca, że plik jest plikiem właściwym.
	 */
	public static final long FLAG_FILE_REGULAR =	0x00000001;

	/**
	 * Flaga oznaczająca, że plik jest katalogiem.
	 */
	public static final long FLAG_FILE_DIR =		0x00000002;

	/**
	 * Flaga oznaczająca powrót do katalogu nadrzędnego.
	 * Potrzebne do wysyłania całych katalogów.
	 */
	public static final long FLAG_FILE_RETPARENT =	0x00000003;
	
	public static final long FLAG_FILE_SYMLINK =	0x00000004;
	public static final long FLAG_FILE_CDEV =		0x00000005;
	public static final long FLAG_FILE_BDEV =		0x00000006;
	public static final long FLAG_FILE_FIFO =		0x00000007;
	public static final long FLAG_FILE_RESFORK =	0x00000010;

	/**
	 * Stany w jakich może znależć się transferowany plik.
	 */
	public enum States { WAITING_FOR_CONNECTION, TRANSFERRING, ERROR, COMPLETED };

	/**
	 * Rozmiar bufora używany podczas wysyłania, bądź pobierania.
	 */
	protected final static int bufferSize = 8192;

	/**
	 * Plik, który należy wysłać bądź odebrać.
	 */
	protected File file;

	/**
	 * <code>True</code>, gdy plik jest plikiem właściwym.
	 */
	protected boolean isFile;

	/**
	 * Kontakt, do/od którego jest przesyłany plik. Potrzebny do identyfikacji
	 * pliku, który należy wysłać.
	 */
	protected IpmsgContact contact;

	/**
	 * Identyfikator pakietu, w którym zawarto żądanie wysłania pliku.
	 */
	protected long packetID;

	/**
	 * Identyfikator pliku nadawany podczas tworzenia żądania wysłania.
	 */
	protected long fileID;

	/**
	 * Rozmiar pliku
	 */
	protected long fileSize;

	/**
	 * Stan, w którym aktualnie znajduje się transferowany plik.
	 */
	protected States state = States.WAITING_FOR_CONNECTION;

	/**
	 * Rozmiar przesłanych już danych.
	 */
	protected long transferredDataSize = 0;

	/**
	 * Wątek zajmujący się przesyłem.
	 */
	protected Thread thread = null;

	/**
	 * Pozwala określić czy plik jest plikiem właściwym.
	 *
	 * @return <code>true</code> gdy plik jest plikiem właściwym
	 */
	public boolean isFile()
	{
		return isFile;
	}

	/**
	 * Pozwala określić rozmiar pliku.
	 *
	 * @return rozmiar pliku
	 */
	public long getFileSize()
	{
		return fileSize;
	}

	/**
	 * Metoda synchronizowana, zwraca stan, w którym obecnie znajduje się plik.
	 *
	 * @return stan, w którym obecnie znajduje się plik
	 */
	public synchronized States getState()
	{
		return state;
	}

	/**
	 * Metoda synchronizowana, ustawia stan, w ktorym ma się znajdować plik.
	 * Powiadamia o zmianie stanu obiektu wszystkich obserwatorów.
	 * 
	 * @param state stan, w ktorym ma się znajdować plik
	 */
	protected synchronized void setState(States state)
	{
		this.state = state;
		notifyObservers();
	}

	/**
	 * Metoda synchronizowana, zwracająca ilość wysłanych już danych.
	 *
	 * @return ilość wysłanych już danych.
	 */
	public synchronized long getTransferredDataSize()
	{
		return transferredDataSize;
	}

	/**
	 * Metoda synchronizowana, ustawiająca ilość przesłanych już danych.
	 * Powiadamia o zmianie stanu obiektu wszystkich obserwatorów.
	 * 
	 * @param transferredDataSize ilość przesłanych już danych
	 */
	protected synchronized void setTransferredDataSize(long transferredDataSize)
	{
		this.transferredDataSize = transferredDataSize;
		notifyObservers();
	}

	@Override
	public String toString()
	{
		return "IpmsgTransferredFile[\"" +
			file.getName() + "\"" + (isFile?"(plik)":"(nie plik)") + ", " +
			"od/do \"" + contact.name + "\", " +
			"przesłano " + transferredDataSize + "/" + fileSize + ", " +
			state.toString() + "]";
	}
}
