package protocols;

import tools.SimpleObservable;

/**
 * Klasa odpowiadająca koncie korzystającym z danego protokołu. W przypadku
 * części protokołów jest to "wirtualne" konto - nigdzie nie rejestrowane,
 * rozgłaszane przez broadcast.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class Account extends SimpleObservable
{
	/**
	 * Ustawia status dostępności konta.
	 *
	 * @param status nowy status
	 */
	public abstract void setStatus(Contact.UserStatus status);

	/**
	 * Pobiera status dostępności konta.
	 *
	 * @return status użytkownika dla danego konta
	 */
	public abstract Contact.UserStatus getStatus();

	/**
	 * Ustawia status opisowy konta.
	 *
	 * @param textStatus nowy status opisowy
	 */
	public abstract void setTextStatus(String textStatus);

	/**
	 * Stwierdza, czy konto wspiera przypisywanie grup kontaktom. Zazwyczaj
	 * wspólne dla całego protokołu.
	 *
	 * @return <code>true</code>, jeżeli konto wspiera przypisywanie grup
	 * kontaktom
	 */
	public boolean isGroupsSupported()
	{
		return false;
	}

	/**
	 * Wywołanie metody skutkuje próbą wysłania wiadomości.
	 *
	 * Należy przede wszystkim sprawdzić, czy powinna zostać wysłana za pomocą
	 * tego konta (robi to klasa implementująca protokół).
	 *
	 * @param message obiekt wiadomości (zawiera m.in. obiekt pokoju rozmów)
	 * @return <code>true</code>, jeżeli wiadomość powinna zostać wysłana za
	 * pomocą danego konta, NIE oznacza to powodzenia wysyłania wiadomości
	 */
	public abstract boolean postMessage(OutgoingMessage message);
}
