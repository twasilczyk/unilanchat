package protocols.ipmsg;

import java.io.IOException;
import java.net.*;
import java.util.*;

import main.Main;
import protocols.*;
import tools.IP4Utilities;

/**
 * Implementacja protokołu IPMsg
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class IpmsgAccount extends Account
{
	protected final ContactList contactList;
	public final ChatRoomList chatRoomList;

	public IpmsgAccount(ContactList contactList, ChatRoomList chatRoomList)
	{
		this.contactList = contactList;
		this.chatRoomList = chatRoomList;
	}

	@Override public boolean isGroupsSupported()
	{
		return true;
	}

	// <editor-fold defaultstate="collapsed" desc="Wątek połączenia">

	protected final static int port = 2425;
	protected final static int socketTimeout = 1000; //ms

	protected IpmsgConnectionThread connectionThread;

	/**
	 * Wątek połączenia IPMsg. Dokładniej mówiąc, nie jest to połączenie, tylko
	 * otwarcie portu UDP, do odbioru i wysyłania pakietów protokołu IPMsg.
	 */
	protected class IpmsgConnectionThread extends Thread
	{
		protected DatagramSocket sock;
		protected final Object sockSendLocker = new Object();
		public boolean isConnected = false, failedConnecting = false;
		protected final byte[] readBuff = new byte[1024];

		public IpmsgConnectionThread()
		{
			this.setDaemon(true);
			this.start();
		}

		@Override public void run()
		{
			try
			{
				sock = new DatagramSocket(port);
				sock.setSoTimeout(socketTimeout);
			}
			catch (SocketException e)
			{
				failedConnecting = true;
				return;
			}

			isConnected = true;

			while (true)
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException e)
				{
					break;
				}
				
				DatagramPacket packet = new DatagramPacket(readBuff, readBuff.length);
				try
				{
					sock.receive(packet);

					IpmsgPacket ipmsgPacket;

					try
					{
						ipmsgPacket = IpmsgPacket.fromRAWData(
								packet.getData(), packet.getLength());
						ipmsgPacket.ip = packet.getAddress().getHostAddress();
					}
					catch (IllegalArgumentException e)
					{
						System.err.println(e.getMessage());
						System.err.println(
							"Pakiet od " + packet.getAddress().getHostAddress() + ", " +
							"Dane: [" + new String(packet.getData(), 0, packet.getLength()) + "]"
							);
						continue;
					}

					handlePacket(ipmsgPacket);
				}
				catch (SocketTimeoutException e)
				{
				}
				catch (IOException e)
				{
					break;
				}
			}

			sock.close();
			isConnected = false;
//			System.err.println("disconnected");
		}

		/**
		 * Zamyka port i kończy wątek połączenia
		 */
		public void disconnect()
		{
			isConnected = false;
			this.interrupt();
		}

		/**
		 * Wysyła pakiet za pomocą otwartego portu UDP
		 *
		 * @param packet Pakiet do wysłania
		 */
		public void send(DatagramPacket packet)
		{
			if (packet == null)
				throw new NullPointerException();
			if (!isConnected)
				return;
			synchronized(sockSendLocker)
			{
				if (!isConnected)
					return;
				try
				{
					sock.send(packet);
				}
				catch (SocketTimeoutException e)
				{
				}
				catch (IOException e)
				{
					disconnect();
				}
			}
		}
	}

	/**
	 * @return Czy połączenie jest nawiązane
	 */
	protected boolean isConnected()
	{
		return (connectionThread != null && connectionThread.isConnected);
	}

	/**
	 * Zakłada lub usuwa połączenie (gniazdo UDP)
	 *
	 * @param setConnected Czy ma zostać utworzone gniazdo
	 * @return Czy się udało
	 */
	protected synchronized boolean setConnected(boolean setConnected)
	{
		if (isConnected() == setConnected)
			return true;
		if (setConnected)
		{
			connectionThread = new IpmsgConnectionThread();
			while (!connectionThread.isConnected &&
					!connectionThread.failedConnecting)
			{
				try
				{
					Thread.sleep(10);
				}
				catch (InterruptedException e)
				{
					return false;
				}
			}
			if (connectionThread.isConnected)
			{
				postMessageThread = new PostMessageThread();
				garbageContactsCollector = new GarbageContactsCollector();
				return true;
			}
			return false;
		}
		else
		{
			connectionThread.disconnect();
			postMessageThread.interrupt();
			garbageContactsCollector.interrupt();
			return true;
		}
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Wysyłanie i odbieranie pakietów IpmsgPacket">

	/**
	 * Wysyła pakiet protokołu IPMsg przez UDP
	 *
	 * @param packet Pakiet do wysłania
	 */
	protected void sendPacket(IpmsgPacket packet)
	{
		if (packet == null)
			throw new NullPointerException();
		if (!isConnected())
			throw new RuntimeException("Próba wysłania pakietu bez połączenia");

		byte[] rawPacketData = packet.getRAWData();

		DatagramPacket udpPacket = new DatagramPacket(
				rawPacketData, rawPacketData.length);
		udpPacket.setPort(port);

		if (packet.ip == null) // wysyłamy na broadcast
		{
			for (Inet4Address bcaddr : IP4Utilities.getBroadcastAdresses())
			{
				udpPacket.setAddress(bcaddr);
				connectionThread.send(udpPacket); // TODO: czy na pewno dalej mamy połączenie?
			}
		}
		else
		{
			try
			{
				udpPacket.setAddress(IP4Utilities.getIPAddress(packet.ip));
			}
			catch (UnknownHostException e)
			{
				System.err.println("Nieznany adres: " + e.getMessage());
				return;
			}

			connectionThread.send(udpPacket);
		}
	}

	/**
	 * Obsługa przychodzących pakietów. Wywoływane z wątku połączenia
	 *
	 * @param packet Odebrany pakiet
	 * @see IpmsgConnectionThread
	 */
	private void handlePacket(IpmsgPacket packet)
	{
		if (packet == null)
			throw new NullPointerException();
		if (IP4Utilities.isLocalAdress(packet.ip) ||
			userStatus == Contact.UserStatus.OFFLINE ||
			packet.getCommand() == IpmsgPacket.COMM_NOOP)
			return;

		Contact genContact = contactList.get(IpmsgContact.getID(packet.ip));
		IpmsgContact contact = null;
		if (genContact != null)
			contact = (IpmsgContact)genContact;

		if (packet.getCommand() == IpmsgPacket.COMM_EXIT)
		{
			if (contact != null)
				contact.setStatus(Contact.UserStatus.OFFLINE);
			return;
		}

		if (contact == null)
			contact = getOrCreateContact(packet.ip);

		if (contact.getName().isEmpty())
			contact.setName(packet.userName);

		if (packet.getCommand() == IpmsgPacket.COMM_ENTRY ||
			packet.getCommand() == IpmsgPacket.COMM_ABSENCE ||
			packet.getCommand() == IpmsgPacket.COMM_ANSENTRY)
		{
			if (packet.getFlag(IpmsgPacket.FLAG_ABSENCE))
				contact.setStatus(Contact.UserStatus.BUSY);
			else
				contact.setStatus(Contact.UserStatus.ONLINE);
			String[] dataSplit = packet.data.split("\0", 10);
			if (dataSplit.length == 2)
			{
				String nick = dataSplit[0].trim();
				int statusStart = nick.lastIndexOf('[');
				if (nick.charAt(nick.length() - 1) == ']' && statusStart >= 0)
				{
					contact.setTextStatus(nick.substring(statusStart + 1, nick.length() - 1));
					nick = nick.substring(0, statusStart).trim();
				}
				else
					contact.setTextStatus("");
				if (!nick.isEmpty())
					contact.setName(nick);

				contact.setGroup(dataSplit[1]);
			}
			//else if (dataSplit.length == 3) // TODO: wtf? protokół o tym nie wspomina (?)
		}

//		System.out.println("Odebrano pakiet: " + packet);

		switch (packet.getCommand())
		{
			case IpmsgPacket.COMM_ANSENTRY:
				garbageContactsCollector.confirmContact(contact);
				break;
			case IpmsgPacket.COMM_ABSENCE:
				break;
			case IpmsgPacket.COMM_READMSG:
			case IpmsgPacket.COMM_ANSREADMSG:
				// pakiety ignorowane
				break;
			case IpmsgPacket.COMM_ENTRY:
				statusNotify(IpmsgPacket.COMM_ANSENTRY, packet.ip);
				break;
			case IpmsgPacket.COMM_SENDMSG:
				gotMessage(packet);
				break;
			case IpmsgPacket.COMM_RECVMSG:
				try
				{
					postMessageThread.confirmMessage(contact, Long.parseLong(packet.data));
				}
				catch (NumberFormatException e) { }
				break;
			default:
				System.out.println("Niezidentyfikowany pakiet: " + packet);
		}
	}

	// </editor-fold>

	// <editor-fold desc="Implementacja protokołu IPMsg">

	protected String userName = Main.tmpNick;
	protected String groupName = "UniLANChat";

	// <editor-fold defaultstate="collapsed" desc="Lista kontaktów">

	private GarbageContactsCollector garbageContactsCollector;

	/**
	 * Wątek sprawdzania dostępności jest inicjowany i kończony w ramach
	 * zarządzania wątkiem połączenia. Służy do filtrowania listy z kontaktów,
	 * które stały się niedostępne, ale nie poinformowały o tym. Przy okazji
	 * znajduje kontakty, których ogłoszenia o dostępności nie dotarły.
	 *
	 * @see #setConnected(boolean)
	 */
	class GarbageContactsCollector extends Thread
	{
		/**
		 * Ile czasu w ms ma być pomiędzy kolejnymi akcjami czyszczenia listy
		 */
		protected final static int collectorInterval = 60000;

		/**
		 * Osobne ustawienie czasu oczekiwania dla pierwszego czyszczenia
		 *
		 * @see GarbageContactsCollector.collectorInterval
		 */
		protected final static int collectorFirstInterval = 5000;

		/**
		 * Ile czasu ma kontakt na odpowiedź. Odpowiedź na poprzednie zapytanie
		 * (z tego samego przebiegu akcji czyszczenia) liczy się jak bieżąca
		 * odpowiedź
		 */
		protected final static int confirmTimeout = 2000;

		/**
		 * Ile razy kontakt jest odpytywany (bez powodzenia) przed skreśleniem
		 * z listy
		 */
		protected final static int confirmMaxCount = 5;

		/**
		 * Lista nie potwierdzonych (w danej akcji czyszczenia) kontaktów
		 */
		private final HashMap<String, IpmsgContact> unconfirmedContacts =
				new HashMap<String, IpmsgContact>();

		/**
		 * Cache, mirror unconfirmedContacts, aby nie blokować metody
		 * potwierdzania kontaktów online (i wątku połączenia).
		 *
		 * @see confirmContact()
		 */
		private final Vector<IpmsgContact> notifyPersonally =
				new Vector<IpmsgContact>();

		public GarbageContactsCollector()
		{
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
					if (isConnected()) //nie koniecznie musi trwać przez całą pętlę
					{
						// przygotowanie listy kontaktów do sprawdzenia
						synchronized (unconfirmedContacts)
						{
							unconfirmedContacts.clear();
							Vector<IpmsgContact> online = getOnlineContacts();
							for (IpmsgContact contact : online)
								unconfirmedContacts.put(contact.getID(), contact);
						}

						// wysłanie powiadomienia do wszystkich
						statusNotify(IpmsgPacket.COMM_ENTRY);
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
								statusNotify(IpmsgPacket.COMM_ENTRY, contact.getIP());
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
		 * pakiet typu COMM_ANSENTRY
		 *
		 * @param contact Potwierdzany kontakt
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

	protected IpmsgContact getOrCreateContact(String ip)
	{
		synchronized (contactList)
		{
			Contact genContact = contactList.get(IpmsgContact.getID(ip));
			IpmsgContact contact;
			if (genContact == null)
			{
				contact = new IpmsgContact(this, ip);
				contactList.add(contact);
			}
			else if (genContact instanceof IpmsgContact)
				contact = (IpmsgContact)genContact;
			else
				throw new RuntimeException("Konflikt oznaczeń ID, kontakt nie jest z protokołu ipmsg: " + genContact.getClass().getName());
			return contact;
		}
	}

	/**
	 * Zwraca listę kontaktów, które mają status inny niż OFFLINE. Zwrócony
	 * wektor można modyfikować
	 *
	 * @return Dostępne kontakty
	 */
	protected Vector<IpmsgContact> getOnlineContacts()
	{
		Vector<IpmsgContact> online = new Vector<IpmsgContact>();
		synchronized (contactList)
		{
			for (Contact contact : contactList)
			{
				if (!(contact instanceof IpmsgContact))
					continue;
				if (contact.getStatus() == Contact.UserStatus.OFFLINE)
					continue;
				online.add((IpmsgContact)contact);
			}
		}
		return online;
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Ustawianie statusu">

	protected final static int maxTextStatusLength = 30;

	protected Contact.UserStatus userStatus = Contact.UserStatus.OFFLINE;
	protected String textStatus = "";

	/**
	 * Ustawia status konta. Jeżeli zmiana następuje między grupami (OFFLINE),
	 * a (ONLINE, BUSY), połączenie zostaje -- odpowiednio -- nawiązane lub
	 * przerwane
	 *
	 * @param status Nowy status
	 */
	public synchronized void setStatus(Contact.UserStatus status)
	{
		if (status == null)
			throw new NullPointerException();
		Contact.UserStatus oldStatus = this.userStatus;
		this.userStatus = status;

		if (status == Contact.UserStatus.OFFLINE)
		{
			if (isConnected())
			{
				statusNotify(IpmsgPacket.COMM_EXIT);
				setConnected(false);

				Vector<Contact> removeContacts = new Vector<Contact>();
				synchronized (contactList)
				{
					for (Contact contact : contactList)
						if (contact instanceof IpmsgContact)
							removeContacts.add(contact);
				}
				for (Contact contact : removeContacts)
					contactList.remove(contact);
			}

			notifyObservers();
			return;
		}
		
		setConnected(true);

		if (oldStatus == Contact.UserStatus.OFFLINE)
		{
			if (isConnected())
				statusNotify(IpmsgPacket.COMM_ENTRY);
			else
				this.userStatus = Contact.UserStatus.OFFLINE;
		}
		else
			statusNotify(IpmsgPacket.COMM_ABSENCE);

		notifyObservers();
	}

	public Contact.UserStatus getStatus()
	{
		return userStatus;
	}

	/**
	 * Ustawia status opisowy. Jeżeli nawiązane jest połączenie, powiadamia o
	 * tym wszystkich w sieci.
	 *
	 * @param textStatus Nowy status opisowy
	 */
	@Override public synchronized void setTextStatus(String textStatus)
	{
		if (textStatus == null)
			throw new NullPointerException();
		textStatus = textStatus.replace(']', ' ').replace('[', ' ').trim();
		if (textStatus.length() > maxTextStatusLength)
			textStatus = textStatus.substring(0, maxTextStatusLength);
		if (textStatus.equals(this.textStatus))
			return;
		this.textStatus = textStatus;
		if (isConnected())
			statusNotify(IpmsgPacket.COMM_ABSENCE);
	}

	/**
	 * Powiadomienie wszystkich o statusie konta
	 *
	 * @param command Rodzaj polecenia powiadamiającego (COMM_ENTRY, COMM_ABSENCE, COMM_ANSENTRY lub COMM_EXIT)
	 * @see #statusNotify(int, String)
	 */
	protected void statusNotify(int command)
	{
		statusNotify(command, null);
	}

	/**
	 * Powiadamia wybraną osobę o statusie konta.
	 * 
	 * @param command Rodzaj polecenia powiadamiającego (COMM_ENTRY, COMM_ABSENCE, COMM_ANSENTRY lub COMM_EXIT)
	 * @param ip IP osoby powiadamianej
	 */
	protected void statusNotify(int command, String ip)
	{
		if (command != IpmsgPacket.COMM_ENTRY &&
			command != IpmsgPacket.COMM_ABSENCE &&
			command != IpmsgPacket.COMM_ANSENTRY &&
			command != IpmsgPacket.COMM_EXIT)
			throw new IllegalArgumentException();

		IpmsgPacket packet = new IpmsgPacket(userName);
		packet.ip = ip;
		packet.setCommand(command);

		if (this.userStatus == Contact.UserStatus.ONLINE)
			packet.setFlag(IpmsgPacket.FLAG_ABSENCE, false);
		else // IContact.UserStatus.BUSY
			packet.setFlag(IpmsgPacket.FLAG_ABSENCE, true);

		if (textStatus.isEmpty())
			packet.data = userName + '\0' + groupName;
		else
			packet.data = userName + '[' + textStatus + ']' + '\0' + groupName;
		sendPacket(packet);
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Odbieranie wiadomośći">

	/**
	 * Potwierdzone pakiety -- kluczem jest "[id pakietu]/[id usera]", wartością
	 * czas potwierdzenia (unix timestamp). Jeżeli minął confirmedMessagesTimeout,
	 * potwierdzenie jest nieaktualne (powinno być zignorowane, może być usunięte)
	 */
	//TODO: czyszczenie, co jakiś czas
	protected final HashMap<String, Long> confirmedMessages =
			new HashMap<String, Long>();
	protected static final int confirmedMessagesTimeout = 300000; //5 minut

	protected void gotMessage(IpmsgPacket packet)
	{
		boolean sendCheck = packet.getFlag(IpmsgPacket.FLAG_SENDCHECK);
		boolean broadcast =
				packet.getFlag(IpmsgPacket.FLAG_MULTICAST) ||
				packet.getFlag(IpmsgPacket.FLAG_BROADCAST);

		if (sendCheck)
		{
			// potwierdzamy wszystkie, bo może jakiś nie doszedł
			IpmsgPacket confirmPacket = new IpmsgPacket(userName);
			confirmPacket.ip = packet.ip;
			confirmPacket.setCommand(IpmsgPacket.COMM_RECVMSG);
			confirmPacket.data = Long.toString(packet.packetNo);
			sendPacket(confirmPacket);

			synchronized (confirmedMessages)
			{
				String messageID = packet.packetNo + "/" + IpmsgContact.getID(packet.ip);
				long now = (new Date()).getTime();
				
				// już odebrano
				if (confirmedMessages.containsKey(messageID) &&
					confirmedMessages.get(messageID) + confirmedMessagesTimeout >= now)
					return;
				
				confirmedMessages.put(messageID, now);
			}
		}

		Contact contact = contactList.get(packet.ip + "@ipmsg");
		String authorName;
		if (contact == null)
			authorName = "Nieznajomy (" + packet.ip + ")";
		else
			authorName = contact.getName();

		ChatRoom chatRoom;
		if (broadcast)
			chatRoom = chatRoomList.getMain();
		else
			chatRoom = chatRoomList.get(contact);

		// wiadomości z załącznikami -- obcinanie informacji o załączniku
		String messageRAWContents;
		if (packet.data.indexOf('\0') < 0)
			messageRAWContents = packet.data;
		else
			messageRAWContents = packet.data.substring(0, packet.data.indexOf("\0"));
		StringBuilder messageContents = new StringBuilder();

		//TODO: dodać możliwość wyłączenia kasowania linii cytatu (automatyczne wykrywanie, czy gadamy z ipmsg, czy UniLANChat)
		String[] messageLines = messageRAWContents.split("\n");
		for (int i = 0; i < messageLines.length; i++)
		{
			if (messageLines[i].length() > 0 && messageLines[i].charAt(0) == '>')
				continue;
			messageContents.append(messageLines[i]);
			if (i < messageLines.length - 1)
				messageContents.append("\n");
		}

		IncomingMessage message = new IncomingMessage(chatRoom, authorName);
		message.setContents(messageContents.toString().trim());
		chatRoom.gotMessage(message);
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Wysyłanie wiadomości">

	private PostMessageThread postMessageThread;

	/**
	 * Wątek wysyłania wiadomości jest inicjowany i kończony w ramach
	 * zarządzania wątkiem połączenia
	 *
	 * @see #setConnected(boolean)
	 */
	class PostMessageThread extends Thread
	{
		/**
		 * Lista wiadomości do wysłania
		 * Klucz: id pakietu
		 * Wartość: obiekt wiadomości (zawiera także wspomniany klucz)
		 */
		protected final HashMap<Long, IpmsgMessagePacket> messages =
				new HashMap<Long, IpmsgMessagePacket>();

		public PostMessageThread()
		{
			setDaemon(true);
			start();
		}

		@Override public synchronized void run()
		{
			Vector<IpmsgMessagePacket> removeMessages = new Vector<IpmsgMessagePacket>();
			
			while (true) //TODO: wychodzić, jak konto nie istnieje? a moze przez interrupt?
			{
				if (isConnected())
				{
					removeMessages.clear();
					for (IpmsgMessagePacket messagePacket : messages.values())
						if (!messagePacket.isNextTryAvailable())
						{
							removeMessages.add(messagePacket);
							//TODO: może powiadamianie wiadomości o tym, że nie doszła?
							
							StringBuilder failMsg = new StringBuilder("Nie doszło do: ");
							boolean fst = true;
							for (IpmsgContact receiver : messagePacket.receivers)
							{
								receiver.setStatus(Contact.UserStatus.OFFLINE);
								if (fst)
									fst = false;
								else
									failMsg.append(", ");
								failMsg.append(receiver.getIP());
							}
							System.err.println(failMsg);
						}
					for (IpmsgMessagePacket messagePacket : removeMessages)
						messages.remove(messagePacket.getID());
					for (IpmsgMessagePacket messagePacket : messages.values())
						for (IpmsgContact receiver : messagePacket.receivers)
						{
							messagePacket.packet.ip = receiver.getIP();
							
							// TODO: obsłużyć błędy, jeżeli nie połączony
							// (isConnected() nie koniecznie musi trwać przez
							// całą pętlę)
							sendPacket(messagePacket.packet);
						}
				}

				try
				{
					if (messages.isEmpty())
						wait();
					else
						wait(1000);
				}
				catch (InterruptedException e)
				{
					return;
				}
			}
		}

		/**
		 * Dodaje pakiet do listy wysyłanych. Po dodaniu pakiet (szczególnie
		 * lista odbiorców) nie powinna być modyfikowana.
		 *
		 * @param messagePacket Wiadomość do wysłania
		 */
		public synchronized void enqueueMessagePacket(IpmsgMessagePacket messagePacket)
		{
			messages.put(messagePacket.getID(), messagePacket);
			if (getState().equals(Thread.State.WAITING)) // WAITING - bez ustawionego timeoutu
				notifyAll();
		}

		/**
		 * Potwierdza odebranie wiadomości. Wywołana, jeżeli kontakt wysłał COMM_RECVMSG
		 *
		 * @param contact Kontakt, który potwierdza odebranie wiadomości
		 * @param id ID potwierdzanej wiadomości
		 */
		public synchronized void confirmMessage(IpmsgContact contact, long id)
		{
			IpmsgMessagePacket message = messages.get(id);
			if (message == null)
				return;
			message.receivers.remove(contact); // true, jeżeli wcześniej nie potwierdzone
			if (message.receivers.isEmpty())
				messages.remove(id);
		}
	}

	/**
	 * Klasa pomocnicza przechowująca wiadomość do wysłania, listę odbiorców
	 * oraz licznik prób
	 */
	class IpmsgMessagePacket
	{
		protected final static int maxSendTries = 5;

		public final IpmsgPacket packet;
		public final Vector<IpmsgContact> receivers = new Vector<IpmsgContact>();
		protected int sendTries = 0;

		public IpmsgMessagePacket(OutgoingMessage message)
		{
			ChatRoom room = message.getChatRoom();
			packet = new IpmsgPacket(userName);

			if (room.id.isEmpty())
			{
				receivers.addAll(getOnlineContacts()); //TODO: contactList ma być pobierana z chatRoom jako członkowie rozmowy
				packet.setFlag(IpmsgPacket.FLAG_MULTICAST, true); // FLAG_BROADCAST ignoruje flagę FLAG_SENDCHECK
			}
			else if (!room.id.endsWith("@ipmsg"))
				throw new UnsupportedOperationException("Nie można wysyłać wiadomości z innych protokołów");
			else if (room instanceof PrivateChatRoom)
				receivers.add((IpmsgContact)((PrivateChatRoom)room).getContact());
			else
				throw new UnsupportedOperationException("Aktualnie nie są obsługiwane pokoje multicast");

			packet.setCommand(IpmsgPacket.COMM_SENDMSG);
			packet.setFlag(IpmsgPacket.FLAG_SENDCHECK, true);

			packet.data = message.getContents();
		}

		/**
		 * @return Identyfikator wiadomości (czyli pakietu wychodzącego)
		 */
		public long getID()
		{
			return packet.packetNo;
		}

		/**
		 * Sprawdza, czy można jeszcze raz spróbować wysłać wiadomość -- jeżeli
		 * tak, zaznacza kolejną próbę jako wykonaną
		 *
		 * @return Można wykonać próbę
		 */
		public synchronized boolean isNextTryAvailable()
		{
			if (sendTries >= maxSendTries)
				return false;
			sendTries++;
			return true;
		}
	}

	/**
	 * Kolejkuje wiadomość do wysłania
	 *
	 * @param message Wiadomość do wysłania
	 * @return Czy wiadomość może być przesłana tym protokołem
	 */
	@Override public boolean postMessage(OutgoingMessage message)
	{
		ChatRoom room = message.getChatRoom();
		if (!room.id.isEmpty() && !room.id.endsWith("@ipmsg"))
			return false;

		postMessageThread.enqueueMessagePacket(new IpmsgMessagePacket(message));
		return true;
	}

	// </editor-fold>

	// </editor-fold>
}
