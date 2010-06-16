package protocols;

import tools.ObservableInterface;

/**
 * Interfejs dla przesyłanych (odbieranych, lub wysyłanych) plików (załączników).
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public interface TransferredFile extends ObservableInterface
{
	/**
	 * Stany w jakich może znależć się przesyłany plik.
	 */
	public enum State { PREPARING, WAITING_FOR_CONNECTION, TRANSFERRING, CANCELLED, ERROR, COMPLETED };

	/**
	 * Zwraca stan, w którym obecnie znajduje się plik.
	 *
	 * @return stan, w którym obecnie znajduje się plik
	 */
	abstract public State getState();

	/**
	 * Pozwala określić czy przesyłany jest plik, czy katalog.
	 *
	 * @return <code>true</code> gdy przesyłany jest plik (w przeciwnym wypadku,
	 * jest to katalog)
	 */
	abstract public boolean isFile();

	/**
	 * Zwraca rozmiar przesyłanego pliku.
	 *
	 * W przypadku niektórych implementacji, nie da się określić dokładnego
	 * rozmiaru pliku (najczęściej katalogu). W tym przypadku, jeżeli nie
	 * wiadomo nic o tym rozmiarze, zostanie zwrócony null. Jeżeli można
	 * częściowo go oszacować - zwracane jest oszacowanie.
	 *
	 * @return rozmiar pliku (lub null, jeżeli nieznany)
	 */
	abstract public Long getFileSize();

	/**
	 * Zwraca ilość już przesłanych danych.
	 *
	 * @return ilość przesłanych danych
	 */
	abstract public long getTransferredDataSize();

	/**
	 * Zwraca bieżącą szybkość transferu w bajtach na sekundę.
	 *
	 * @return szybkość transferu w bajtach na sekundę
	 */
	abstract public long getTransferSpeed();

	/**
	 * Zwraca nazwę pliku, zaproponowaną przez nadawcę.
	 *
	 * @return zaproponowana nazwa pliku
	 */
	abstract public String getFileName();
}
