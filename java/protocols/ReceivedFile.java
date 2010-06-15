package protocols;

import java.io.File;

/**
 * Interfejs dla odbieranych plików (załączników).
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public interface ReceivedFile extends TransferredFile
{
	/**
	 * Odbiera plik i zapisuje go do pliku o ścieżce zadanej w parametrze.
	 *
	 * @param path ścieżka, pod którą należy zapisać plik
	 */
	public void receive(File target);
}
