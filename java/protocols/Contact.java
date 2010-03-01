package protocols;

import tools.SimpleObservable;

/**
 * Kontakt (inny użytkownik) przechowywany na liście kontaktów.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class Contact extends SimpleObservable
{
	/**
	 * Możliwe statusy użytkowników na liście kontaktów.
	 */
	public enum UserStatus
	{
		/**
		 * Użytkownik jest dostępny.
		 */
		ONLINE,

		/**
		 * Użytkownik jest dostępny, ale zajęty.
		 */
		BUSY,

		/**
		 * Użytkownik jest niedostępny.
		 */
		OFFLINE
	}

	/**
	 * Zwraca pełny identyfikator użytkownika,
	 * w formie <code>[unikalny-tekst]@[nazwa-protokołu]</code>.
	 *
	 * @return pełny identyfikator użytkownika
	 */
	public abstract String getID();

	/**
	 * Zwraca nazwę kontaktu (np. do wyświetlania).
	 *
	 * @return nazwa kontaktu
	 */
	public abstract String getName();

	/**
	 * Zwraca grupę, do której jest zapisany użytkownik (kontakt). Metoda działa
	 * tylko, jeżeli protokół obsługuje grupy.
	 *
	 * @return grupa kontaktu
	 */
	public abstract String getGroup();

	/**
	 * Zwraca konto związane z kontaktem.
	 *
	 * @return konto
	 */
	public abstract Account getAccount();

	/**
	 * Zwraca aktualny status kontaktu.
	 *
	 * @return status kontaktu
	 */
	public abstract UserStatus getStatus();

	/**
	 * Zwraca tekstowy opis statusu.
	 *
	 * @return tekstowy opis statusu, lub <code>null</code>, jeżeli nie
	 * ustawiony
	 */
	public abstract String getTextStatus();

	/**
	 * Zamienia listę kontaktów w tekstową listę ich nazw.
	 *
	 * @param contacts lista kontaktów
	 * @param glue separator łączący nazwy
	 * @return połączona lista
	 */
	public static String join(Iterable<Contact> contacts, String glue)
	{
		StringBuilder joined = new StringBuilder();
		boolean first = true;
		for (Contact c : contacts)
		{
			if (first)
				first = false;
			else
				joined.append(glue);
			joined.append(c.getName());
		}
		return joined.toString();
	}
}
