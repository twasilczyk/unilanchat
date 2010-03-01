package protocols.ipmsg;

import java.util.*;

import protocols.Contact;

/**
 * Wątek sprawdzania dostępności jest inicjowany i kończony w ramach
 * zarządzania wątkiem połączenia. Służy do filtrowania listy z kontaktów,
 * które stały się niedostępne, ale nie poinformowały o tym. Przy okazji
 * znajduje kontakty, których ogłoszenia o dostępności nie dotarły.
 *
 * @see IpmsgAccount.setConnected(boolean)
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
class IpmsgGarbageContactsCollector extends Thread
{
	/**
	 * Ile czasu w ms ma być pomiędzy kolejnymi akcjami czyszczenia listy.
	 */
	protected final static int collectorInterval = 60000;

	/**
	 * Osobne ustawienie czasu oczekiwania dla pierwszego czyszczenia.
	 *
	 * @see #collectorInterval
	 */
	protected final static int collectorFirstInterval = 5000;

	/**
	 * Ile czasu ma kontakt na odpowiedź. Odpowiedź na poprzednie zapytanie
	 * (z tego samego przebiegu akcji czyszczenia) liczy się jak bieżąca
	 * odpowiedź.
	 */
	protected final static int confirmTimeout = 2000;

	/**
	 * Ile razy kontakt jest odpytywany (bez powodzenia) przed skreśleniem
	 * z listy.
	 */
	protected final static int confirmMaxCount = 5;

	/**
	 * Lista nie potwierdzonych (w danej akcji czyszczenia) kontaktów.
	 */
	private final HashMap<String, IpmsgContact> unconfirmedContacts =
			new HashMap<String, IpmsgContact>();

	/**
	 * Konto obsługiwane przez wątek.
	 */
	protected final IpmsgAccount ipmsgAccount;

	/**
	 * Cache, mirror unconfirmedContacts, aby nie blokować metody
	 * potwierdzania kontaktów online (i wątku połączenia).
	 *
	 * @see confirmContact(IpmsgContact)
	 */
	private final Vector<IpmsgContact> notifyPersonally =
			new Vector<IpmsgContact>();

	/**
	 * Główny konstruktor.
	 *
	 * @param ipmsgAccount konto, którego kontakty mają być weryfikowane
	 */
	public IpmsgGarbageContactsCollector(IpmsgAccount ipmsgAccount)
	{
		super("ULC-Ipmsg-GarbageContactsCollector");
		this.ipmsgAccount = ipmsgAccount;
		setDaemon(true);
		start();
	}

	@Override public synchronized void run()
	{
		try
		{
			wait(collectorFirstInterval);

			while (true)
			{
				if (ipmsgAccount.isConnected()) //nie koniecznie musi trwać przez całą pętlę
				{
					// przygotowanie listy kontaktów do sprawdzenia
					synchronized (unconfirmedContacts)
					{
						unconfirmedContacts.clear();
						Vector<IpmsgContact> online = ipmsgAccount.getOnlineContacts();
						for (IpmsgContact contact : online)
							unconfirmedContacts.put(contact.getID(), contact);
					}

					// wysłanie powiadomienia do wszystkich
					ipmsgAccount.statusNotify(IpmsgPacket.COMM_ENTRY);
					wait(confirmTimeout);

					// wysyłanie powiadomień osobno, do opornych
					for (int i = 0; i < confirmMaxCount; i++)
					{
						if (unconfirmedContacts.isEmpty())
							break;
						notifyPersonally.clear();
						synchronized (unconfirmedContacts)
						{
							notifyPersonally.addAll(unconfirmedContacts.values());
						}
						for (IpmsgContact contact : notifyPersonally)
							ipmsgAccount.statusNotify(IpmsgPacket.COMM_ENTRY, contact.getIP());
						wait(confirmTimeout);
					}

					// usuwanie nie potwierdzonych kontaktów
					synchronized (unconfirmedContacts)
					{
						for (IpmsgContact contact : unconfirmedContacts.values())
							contact.setStatus(Contact.UserStatus.OFFLINE);
						unconfirmedContacts.clear();
					}
				}

				wait(collectorInterval);
			}
		}
		catch (InterruptedException e)
		{
			return;
		}
	}

	/**
	 * Potwierdzanie dostępności kontaktu. Wywołana, jeżeli kontakt wysłał
	 * pakiet typu COMM_ANSENTRY.
	 *
	 * @param contact potwierdzany kontakt
	 */
	public void confirmContact(IpmsgContact contact)
	{
		if (unconfirmedContacts.isEmpty() ||
			!unconfirmedContacts.containsKey(contact.getID()))
			return;
		synchronized (unconfirmedContacts)
		{
			unconfirmedContacts.remove(contact.getID());
		}
	}
}