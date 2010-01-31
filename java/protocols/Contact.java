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
	public enum UserStatus { ONLINE, BUSY, OFFLINE }

	/**
	 * Zwraca pełny identyfikator użytkownika,
	 * w formie [unikalny-tekst]@[nazwa-protokołu]
	 *
	 * @return Pełny identyfikator użytkownika
	 */
	public abstract String getID();

	/**
	 * Zwraca nazwę kontaktu (np. do wyświetlania)
	 *
	 * @return Nazwa kontaktu
	 */
	public abstract String getName();

	/**
	 * Zwraca grupę, do której jest zapisany użytkownik (kontakt). Metoda działa
	 * tylko, jeżeli protokół obsługuje grupy
	 *
	 * @return Grupa kontaktu
	 */
	public abstract String getGroup();

	/**
	 * Zwraca konto związane z kontaktem.
	 *
	 * @return Konto związane z kontaktem
	 */
	public abstract Account getAccount();

	/**
	 * Zwraca aktualny status kontaktu.
	 *
	 * @return Status kontaktu
	 */
	public abstract UserStatus getStatus();

	/**
	 * Zwraca tekstowy opis statusu
	 *
	 * @return Tekstowy opis statusu, lub null, jeżeli nie ustawiony
	 */
	public abstract String getTextStatus();
}
