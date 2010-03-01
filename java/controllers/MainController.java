package controllers;

import java.util.*;

import protocols.*;
import protocols.ipmsg.IpmsgAccount;
import tools.*;
import main.Main;

/**
 * Główny kontroler aplikacji.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class MainController extends SimpleObservable implements Observer
{
	/**
	 * Lista kont posiadanych przez użytkownika (być może wirtualne).
	 */
	protected final Vector<Account> accounts = new Vector<Account>();

	/**
	 * Kontroler odpowiedzialny za pokoje rozmów, związany z daną instancją
	 * aplikacji.
	 */
	protected final ChatController chatController = new ChatController(this);

	/**
	 * Lista kontaktów użytkownika.
	 */
	protected final ContactList contactList = new ContactList();

	public MainController()
	{
		Account ipmacc = new IpmsgAccount(contactList, getChatController().getChatRoomList());
		accounts.add(ipmacc);
		ipmacc.addObserver(this);
	}

	/**
	 * Zwraca kontroler pokoi rozmów, związany z daną instancją aplikacji.
	 *
	 * @return kontroler pokoi rozmów
	 */
	public ChatController getChatController()
	{
		return chatController;
	}

	// <editor-fold defaultstate="collapsed" desc="Zarządzanie statusem">

	/**
	 * Aktualnie ustawiony status.
	 */
	protected Contact.UserStatus currentStatus = Contact.UserStatus.OFFLINE;

	/**
	 * Aktualnie ustawiony status opisowy.
	 */
	protected String currentTextStatus = "";

	/**
	 * Zwraca aktualnie ustawiony (nie żądany) status użytkownika.
	 *
	 * @return aktualny status
	 */
	public Contact.UserStatus getStatus()
	{
		return currentStatus;
	}

	/**
	 * Ustawia nowy status (w tle).
	 *
	 * @param status nowy status
	 */
	public void setStatus(final Contact.UserStatus status)
	{
		Main.backgroundProcessing.invokeLater(new Runnable()
		{
			public void run()
			{
				for (Account account : accounts)
				{
					if (account.getStatus().equals(status))
						continue;
					account.setStatus(status);
				}
				notifyObservers("status");
			}
		});
	}

	/**
	 * Wykonywane, jeżeli istnieje podejrzenie, że jedno z kont zmieniło status.
	 *
	 * @param account podejrzane konto
	 */
	protected void accountStatusProbablyChanged(Account account)
	{
		synchronized (account)
		{
			if (currentStatus != account.getStatus())
			{ //TODO: jak będzie więcej niż jedno konto, rozwiązać to inaczej
				currentStatus = account.getStatus();
				this.notifyObservers("status");
			}
		}
	}

	/**
	 * Pobiera status opisowy, ustawiony przez użytkownika.
	 *
	 * @return status opisowy
	 */
	public String getTextStatus()
	{
		return currentTextStatus;
	}

	/**
	 * Ustawia nowy status opisowy.
	 *
	 * @param newStatus nowy status opisowy
	 */
	public void setTextStatus(String newStatus)
	{
		if (newStatus == null)
			throw new NullPointerException();
		newStatus = newStatus.trim();
		if (newStatus.equals(currentTextStatus))
			return;
		currentTextStatus = newStatus;
		for (Account account : accounts)
			account.setTextStatus(newStatus);
	}

	// </editor-fold>

	/**
	 * Pobiera listę kontaktów (jako referencję, nie kopię).
	 *
	 * @return obiekt listy kontaktów
	 */
	public ContactList getContactList()
	{
		return contactList;
	}

	/**
	 * Pobiera listę kont. NIE jest to kopia - nie wolno zmieniać jej zawartości.
	 *
	 * @return lista kont.
	 * @todo rozwiązać to ładniej
	 */
	public Vector<Account> getAccountsVector()
	{
		return accounts;
	}

	/**
	 * Ustawia status OFFLINE i zamyka aplikację.
	 */
	public void applicationClose()
	{
		setStatus(Contact.UserStatus.OFFLINE);
		System.exit(0);
	}

	/**
	 * Metoda wywoływana po załadowaniu się aplikacji.
	 */
	public void onAppLoad()
	{
		if (X11StartupNotification.isSupported)
			X11StartupNotification.notifyStartupComplete();
		setStatus(Contact.UserStatus.ONLINE);
	}

	public void update(Observable o, Object arg)
	{
		if (o instanceof Account)
			accountStatusProbablyChanged((Account)o);
	}
}
