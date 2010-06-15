package tools;

import java.util.Observer;

/**
 * Klasa może implementować interfejs ObservableInterface, aby być obserwowalną
 * przez obiekty typu Observer. Interfejs służy przede wszystkim jako baza dla
 * innych interfejsów, które potrzebują funkcjonalności Observable.
 *
 * @see java.util.Observer
 * @see java.util.Observable
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public interface ObservableInterface
{
	/**
	 * Dodaje obserwatora do zbioru informowanych przez ten obiekt.
	 *
	 * @param o obserwator do dodania
	 */
	abstract public void addObserver(Observer o);

	/**
	 * Usuwa obserwatora ze zbioru informowanych przez ten obiekt.
	 *
	 * @param o obserwator do usunięcia
	 */
	abstract public void deleteObserver(Observer o);
}
