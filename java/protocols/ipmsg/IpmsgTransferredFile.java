package protocols.ipmsg;

import java.io.File;
import java.util.*;

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
	protected Long fileSize = null;

	/**
	 * Stan, w którym aktualnie znajduje się transferowany plik.
	 */
	protected States state = States.WAITING_FOR_CONNECTION;

	/**
	 * Rozmiar przesłanych już danych.
	 */
	protected long transferredDataSize = 0;

	/**
	 * Bieżąca szybkość transferu w bajtach na sekunde
	 */
	protected long transferSpeed = 0;

	/**
	 * Wątek zajmujący się przesyłem.
	 */
	protected Thread thread = null;

	/**
	 * Timer obliczający prędkość transferu.
	 */
	protected Timer notificationTimer = null;

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
	 * Metoda synchronizowana pozwalajaca okreslic rozmiar pliku.
	 *
	 * @return rozmiar pliku, gdy transferowany jest plik, null
	 * gdy transferowany jest katalog i nie był transferowany żaden plik z
	 * katalogu, w przeciwnym wypadku zwraca sumę wszystkich rozmiarów plików
	 * transferowanych do tej pory włączając w to transferowany w danej chwili
	 * plik
	 */
	public synchronized Long getFileSize()
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
	}

	/**
	 * Metoda synchronizowana zwracająca bieżącą szybkość transferu
	 * w bajtach na sekundę.
	 * 
	 * @return szybkość transferu w bajtach na sekundę
	 */
	protected synchronized long getTransferSpeed()
	{
		return transferSpeed;
	}

	/**
	 * Metoda synchronizowana ustawiająca szybkość transferu, parametr powinien
	 * być wyrażony w bajtach na sekundę.
	 *
	 * @param transferSpeed prędkość transferu w bajtach na sekundę
	 */
	protected synchronized void setTransferSpeed(long transferSpeed)
	{
		this.transferSpeed = transferSpeed;
	}

	/**
	 * Ustawia sumę rozmiarów wszytkich plików transferowanych do tej pory.
	 *
	 * @param fileSize rozmiar wszystkich plików transferowanych do tej pory
	 */
	protected synchronized  void setFileSize(Long fileSize)
	{
		if(this.fileSize == null)
			this.fileSize = 0L;
		this.fileSize += fileSize;
	}

	/**
	 * Kolejkuje zadanie powiadamiania o zmianie stanu obiektu do cyklicznego
	 * wywoływania co określoną ilość czasu.
	 */
	protected void startNotifying()
	{
		if(notificationTimer == null)
		{
			notificationTimer = new Timer();
			notificationTimer.schedule(new NotificationTimerTask(getTransferredDataSize()), 0,
					NotificationTimerTask.interval);
		}
	}

	/**
	 * Zatrzymuje zadanie powiadamiania o zmianie stanu obiektu.
	 */
	protected void stopNotifying()
	{
		if(notificationTimer != null)
		{
			notificationTimer.cancel();
			notificationTimer = null;
		}
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

	/**
	 * Klasa zadania timera obliczająca prędkość transferu oraz powiadamiająca
	 * obserwatorow.
	 */
	class NotificationTimerTask extends TimerTask
	{
		protected long lastTransferredDataSize;

		public static final long interval = 500;

		public static final long ratio = 1000 / interval;

		public NotificationTimerTask(long lastTransferredDataSize)
		{
			this.lastTransferredDataSize = lastTransferredDataSize;
		}

		@Override public void run()
		{
			long currentTransferredDataSize = getTransferredDataSize();
			setTransferSpeed(ratio *
					(currentTransferredDataSize - lastTransferredDataSize));
			lastTransferredDataSize = currentTransferredDataSize;
			notifyObservers();
		}
	}
}
