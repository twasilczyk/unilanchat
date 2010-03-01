package main;

import java.util.Observable;

/**
 * Klasa odpowiadająca za przechowywanie informacji o konfiguracji. Klasy
 * korzystające z konfiguracji tu zawartej, powinny nasłuchiwać zmian w niej.
 *
 * Po wykonaniu serii wywołań setterów, należy wywołać notifyObservers().
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class Configuration extends Observable
{
	/**
	 * Egzemplarz klasy przechowującej konfigurację. Powinien być jeden na całą
	 * aplikację.
	 *
	 * @todo zamiast "new Configuration()", zrobić deserializację
	 * (i serializację).
	 */
	protected static Configuration instance = new Configuration();

	private Configuration()
	{
	}

	/**
	 * Pobiera instancję klasy.
	 *
	 * @return klasa konfiguracji
	 */
	public static Configuration getInstance()
	{
		if (instance == null)
			throw new NullPointerException();
		return instance;
	}

	// <editor-fold defaultstate="collapsed" desc="Ustawienia ogólne">

	private String nick = null;

	/**
	 * Pobiera nick użytkownika, wykorzystywany jako identyfikator w rozmowach.
	 *
	 * @return nick
	 */
	public String getNick()
	{
		if (nick == null)
			return System.getProperty("user.name", "anonim");
		return nick;
	}

	/**
	 * Ustala nowy nick użytkownika.
	 *
	 * @see #getNick()
	 * @param nick nowy nick
	 */
	public void setNick(String nick)
	{
		if (!((this.nick == null && nick == null) ||
			(nick != null && nick.equals(this.nick))))
			setChanged();
		this.nick = nick;
	}

	private boolean ignoreAutoResponses = true;

	/**
	 * Czy automatyczne odpowiedzi (w protokołach umożliwiających ich
	 * zidentyfikowanie) powinny być ignorowane.
	 *
	 * Niektóre implementacje klientów różnych protokołów pozwalają na ustalenie
	 * automatycznej odpowiedzi, jeżeli użytkownik jest nieobecny. Takie
	 * wiadomości zazwyczaj tylko zaśmiecają okno rozmów.
	 *
	 * @return <code>true</code>, jeżeli automatyczne odpowiedzi powinny być
	 * ignorowane
	 */
	public boolean getIgnoreAutoResponses()
	{
		return ignoreAutoResponses;
	}

	/**
	 * Ustala, czy automatyczne odpowiedzi powinny być ignorowane.
	 *
	 * @param set czy ignorować automatyczne odpowiedzi
	 * @see #getIgnoreAutoResponses()
	 */
	public void setIgnoreAutoResponses(boolean set)
	{
		if (set != ignoreAutoResponses)
			setChanged();
		ignoreAutoResponses = set;
	}

	// </editor-fold>

}
