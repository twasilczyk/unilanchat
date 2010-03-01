package tools;

import java.util.Observable;

/**
 * Klasa prostego obiektu obserwowanego. Wysyła powiadomienia zawsze,
 * niezależnie od stanu hasChanged.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class SimpleObservable extends Observable
{
	/**
	 * Powiadamia wszystkich obserwujących o zmianie stanu obiektu.
	 */
	@Override public void notifyObservers()
	{
		this.setChanged();
		super.notifyObservers();
	}

	/**
	 * Powiadamia wszystkich obserwujących o zmianie stanu obiektu, dołączając
	 * przy tym parametr.
	 *
	 * @param arg parametr do przekazania w powiadomieniu
	 */
	@Override public void notifyObservers(Object arg)
	{
		this.setChanged();
		super.notifyObservers(arg);
	}
}
