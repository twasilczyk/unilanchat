package tools;

/**
 * Interfejs dla klas, które potrzebują informacji o zmianach w kolekcji.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public interface SetListener<E>
{
	/**
	 * Powiadomienie o dodaniu obiektu do kolekcji.
	 *
	 * @param item dodany obiekt
	 */
	public void itemAdded(E item);

	/**
	 * Powiadomienie o usunięciu obiektu z kolekcji.
	 *
	 * @param item usunięty obiekt
	 */
	public void itemRemoved(E item);

	/**
	 * Powiadomienie o uaktualnieniu obiektu znajdującego się w kolekcji.
	 *
	 * @param item uaktualniony obiekt
	 */
	public void itemUpdated(E item);
}
