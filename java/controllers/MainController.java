package controllers;

import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import protocols.*;
import protocols.ipmsg.IpmsgAccount;
import tools.SimpleObservable;

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
	 * @return Kontroler pokoi rozmów
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

	private StatusControlThread statusControlThread = new StatusControlThread();

	/**
	 * Aktualnie ustawiony status opisowy.
	 */
	protected String currentTextStatus = "";

	/**
	 * Klasa kontroli statusu przyjmuje zlecenia zmiany statusu. Dzięki
	 * wydzieleniu tego do oddzielnego wątku, główny wątek wykonania programu
	 * nie jest zatrzymywany.
	 */
	class StatusControlThread extends Thread
	{
		/**
		 * Jakiego statusu zażądał użytkownik. Po odebraniu żądania ustawiany jest na NULL
		 */
		protected Contact.UserStatus requestedStatus = null;
		protected final Object requestedStatusLock = new Object();

		public StatusControlThread()
		{
			setDaemon(true);
			start();
		}

		@Override public void run()
		{
			Contact.UserStatus newStatus = null;
			while (true)
			{
				synchronized (requestedStatusLock)
				{
					if (requestedStatus == null)
						try
						{
							requestedStatusLock.wait();
						}
						catch (InterruptedException e)
						{
							return;
						}
					if (requestedStatus == null)
						continue;
					newStatus = requestedStatus;
					requestedStatus = null;
				}

				for (Account account : accounts)
					account.setStatus(newStatus);
				notifyObservers("status");
			}
		}

		/**
		 * Ustawia żądanie zmieny statusu (zostanie ono wykonane w osobnym
		 * wątku). Po zmianie statusu główny kontoler powiadamia wszystkich
		 * swoich obserwatowów (wiadomością "status").
		 *
		 * @param status Nowy status
		 */
		public void requestStatus(Contact.UserStatus status)
		{
			if (status == null)
				throw new NullPointerException();
			synchronized (requestedStatusLock)
			{
				requestedStatus = status;
				requestedStatusLock.notifyAll();
			}
		}
	}

	/**
	 * Zwraca aktualnie ustawiony (nie żądany) status użytkownika.
	 *
	 * @return Aktualny status
	 */
	public Contact.UserStatus getStatus()
	{
		return currentStatus;
	}

	/**
	 * Ustawia żądanie zmiany statusu.
	 *
	 * @param status Nowy status
	 * @see StatusControlThread
	 */
	public void setStatus(Contact.UserStatus status)
	{
		statusControlThread.requestStatus(status);
	}

	/**
	 * Wykonywane, jeżeli istnieje podejrzenie, że jedno z kont zmieniło status.
	 *
	 * @param account Podejrzane konto
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
	 * @return Status opisowy
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
	 * @return Obiekt listy kontaktów
	 */
	public ContactList getContactList()
	{
		return contactList;
	}

	/**
	 * Pobiera listę kont. NIE jest to kopia - nie wolno zmieniać jej zawartości.
	 *
	 * @return Lista kont. 
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
		setStatus(Contact.UserStatus.ONLINE);
	}

	public void update(Observable o, Object arg)
	{
		if (o instanceof Account)
			accountStatusProbablyChanged((Account)o);
	}
}
