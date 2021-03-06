package protocols.ipmsg;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.logging.Level;

import main.*;
import protocols.*;
import net.InterfaceInfoProvider;
import tools.ListenableVector;

/**
 * Implementacja protokołu IPMsg.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class IpmsgAccount extends Account
{
	/**
	 * Główny konstruktor.
	 *
	 * @param contactList lista kontaktów, z której ma korzystać konto
	 * @param chatRoomList lista pokoi, z której ma korzystać konto
	 */
	public IpmsgAccount(ContactList contactList, ChatRoomList chatRoomList)
	{
		super(contactList, chatRoomList);

		Configuration.getInstance().addObserver(new ConfigurationObserver());
	}

	@Override public boolean isGroupsSupported()
	{
		return true;
	}

	// <editor-fold defaultstate="collapsed" desc="Transfer plików">
	
	/**
	 * Zbiór plików oczekujących na wysłanie, bądź odebranie.
	 *
	 * @todo jak zrobimy jakiś ładny interfejs, usunąć public.
	 */
	public final ListenableVector<IpmsgTransferredFile> transferredFiles = new ListenableVector<IpmsgTransferredFile>();

	/**
	 * Metoda odnajdująca obiekt pliku w zbiorze oczekujących na wysłanie.
	 *
	 * @param ip ip kontaktu, do którego należy wysłać plik
	 * @param header nagłówek pliku, który należy wysłać
	 * @return obiekt pliku, gdy ten znajduje się w zbiorze,
	 * <code>null</code> w przeciwnym wypadku
	 */
	public IpmsgSentFile getSentFile(String ip, IpmsgFileReceiveRequestHeader header)
	{
		for (IpmsgTransferredFile file: transferredFiles)
		{
			if (file instanceof IpmsgSentFile &&
					((IpmsgSentFile)file).contact.ip.equals(ip) &&
					((IpmsgSentFile)file).packetID == header.packetID &&
					((IpmsgSentFile)file).fileID == header.fileID &&
					file.isFile == (header.offset != null))
				return (IpmsgSentFile)file;
		}
		return null;
	}

	/**
	 * Ustawia status na CANCELLED wszystkim plikom, które
	 * miały zostać wysłane  od tego samego użytkownika i były inicjowane
	 * tym samym pakietem.
	 *
	 * @param ip ip użytkownika, do którego miały być wysłane pliki
	 * @param packetID ID pakietu inicjującego
	 */
	public void cancelFiles(String ip, long packetID)
	{
		for (IpmsgTransferredFile file: transferredFiles)
			if (file.contact.ip.equals(ip) && file.packetID == packetID &&
				file.state != TransferredFile.State.COMPLETED)
				file.setState(TransferredFile.State.CANCELLED);
	}
	
	// </editor-fold>

	class ConfigurationObserver implements Observer
	{
		public void update(Observable o, Object arg)
		{
			assert(o instanceof Configuration);
			statusNotify(IpmsgPacket.COMM_ABSENCE);
		}
	}

	// <editor-fold defaultstate="collapsed" desc="Wątek połączenia">

	private IpmsgConnectionThread connectionThread;

	/**
	 * Wątek zajmujący się transferem plików.
	 */
	protected IpmsgFileTransferThread fileTransferThread;

	/**
	 * Sprawdza, czy połączenie jest nawiązane.
	 *
	 * @return <code>true</code>, jeżeli połączono
	 */
	protected boolean isConnected()
	{
		return (connectionThread != null && connectionThread.isConnected);
	}

	/**
	 * Zakłada lub usuwa połączenie (gniazdo UDP).
	 *
	 * @param setConnected czy ma zostać utworzone gniazdo
	 * @return <code>true</code>, jeżeli się udało
	 */
	protected synchronized boolean setConnected(boolean setConnected)
	{
		if (isConnected() == setConnected)
			return true;
		if (setConnected)
		{
			connectionThread = new IpmsgConnectionThread(this);
			while (!connectionThread.isConnected &&
					!connectionThread.failedConnecting &&
					connectionThread.isAlive())
			{
				try
				{
					Thread.sleep(10);
				}
				catch (InterruptedException e)
				{
					fileTransferThread.interrupt();
					return false;
				}
			}
			if (connectionThread.isConnected)
			{
				try
				{
					fileTransferThread = new IpmsgFileTransferThread(this);
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
				contactsThread.speedupRefresh();
				return true;
			}
			if (fileTransferThread != null &&
				fileTransferThread.isAlive())
				fileTransferThread.interrupt();
			if (connectionThread.failedConnecting)
			{
				Main.userNotifications.add("Problem z siecią IPMsg",
					"Nie udało się dołączyć do sieci IPMsg. Sprawdź, czy na " +
					"pewno wyłączyłeś wszystkie inne programy korzystające z tej " +
					"sieci (np. oryginalny klient IPMsg).");
				return false;
			}
			return false;
		}
		else
		{
			connectionThread.disconnect();
			fileTransferThread.interrupt();
			return true;
		}
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Wysyłanie i odbieranie pakietów IpmsgPacket">

	/**
	 * Wysyła pakiet protokołu IPMsg przez UDP.
	 *
	 * @param packet pakiet do wysłania
	 */
	protected void sendPacket(IpmsgPacket packet) throws ConnectionLostException
	{
		if (packet == null)
			throw new NullPointerException();
		if (!isConnected())
			throw new ConnectionLostException();

		byte[] rawPacketData = packet.getRAWData();

		DatagramPacket udpPacket = new DatagramPacket(
				rawPacketData, rawPacketData.length);
		udpPacket.setPort(IpmsgConnectionThread.port);

		if (packet.ip == null) // wysyłamy na broadcast
		{
			for (Inet4Address bcaddr : InterfaceInfoProvider.getBroadcastAdresses())
			{
				udpPacket.setAddress(bcaddr);
				connectionThread.send(udpPacket);
			}
		}
		else
		{
			try
			{
				udpPacket.setAddress(InterfaceInfoProvider.getIPAddress(packet.ip));
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
	 * Obsługa przychodzących pakietów. Wywoływane z wątku połączenia.
	 *
	 * @param packet odebrany pakiet
	 * @see IpmsgConnectionThread
	 */
	protected void handlePacket(IpmsgPacket packet)
	{
		if (packet == null)
			throw new NullPointerException();
		if (InterfaceInfoProvider.isLocalAdress(packet.ip) ||
			userStatus == Contact.UserStatus.OFFLINE ||
			packet.getCommand() == IpmsgPacket.COMM_NOOP)
			return;
		Main.logger.log(Level.FINER, "Odebrano pakiet: " + packet.toString());

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
			if (dataSplit.length == 1)
				contact.setName(dataSplit[0]);
			else if (dataSplit.length == 2 || dataSplit.length == 3)
			{
				String nick = dataSplit[0].trim();
				int statusStart = nick.lastIndexOf('[');
				if (!nick.isEmpty() &&
					nick.charAt(nick.length() - 1) == ']' &&
					statusStart > 0)
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
			else if (dataSplit.length > 3)
				Main.logger.log(Level.WARNING, "dataSplit.length > 3");
		}

		switch (packet.getCommand())
		{
			case IpmsgPacket.COMM_ANSENTRY:
				contactsThread.confirmContact(contact);
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
			case IpmsgPacket.COMM_RELEASEFILES:
				try
				{
					cancelFiles(packet.ip, Long.parseLong(packet.data));
				}
				catch (NumberFormatException ex) { }
				break;
			default:
				System.out.println("Niezidentyfikowany pakiet: " + packet);
		}
	}

	// </editor-fold>

	// <editor-fold desc="Implementacja protokołu IPMsg">
	
	private final String groupName = "UniLANChat";

		// <editor-fold defaultstate="collapsed" desc="Lista kontaktów">

	private IpmsgContactsThread contactsThread = new IpmsgContactsThread(this);

	/**
	 * Pobiera kontakt powiązany z podanym adresem IP. Jeżeli nie istnieje -
	 * tworzy nowy.
	 *
	 * @param ip adres IP
	 * @return powiązany kontakt
	 */
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
			else
			{
				assert(genContact instanceof IpmsgContact);
				contact = (IpmsgContact)genContact;
			}
			return contact;
		}
	}

	/**
	 * Zwraca listę kontaktów, które mają status inny niż OFFLINE. Zwrócony
	 * wektor można modyfikować.
	 *
	 * @return dostępne kontakty
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

	/**
	 * Przyspieszenie odświeżenia listy kontaktów. Nie powinno się korzystać
	 * z tej metody do innych celów, niż debugowanie.
	 */
	public void speedupContactsRefresh()
	{
		Main.backgroundProcessing.invokeLater(new Runnable()
		{
			public void run()
			{
				contactsThread.speedupRefresh();
			}
		});
	}

		// </editor-fold>

		// <editor-fold defaultstate="collapsed" desc="Ustawianie statusu">

	/**
	 * Maksymalna długość statusu opisowego.
	 */
	protected final static int maxTextStatusLength = 30;

	/**
	 * Bieżący status tego konta.
	 */
	protected Contact.UserStatus userStatus = Contact.UserStatus.OFFLINE;

	/**
	 * Bieżący status opisowy tego konta.
	 */
	protected String textStatus = "";

	/**
	 * Ustawia status konta. Jeżeli zmiana następuje między grupami (OFFLINE),
	 * a (ONLINE, BUSY), połączenie zostaje - odpowiednio - nawiązane lub
	 * przerwane.
	 *
	 * @param status nowy status
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

				synchronized (contactList)
				{
					for (Contact contact : contactList)
						if (contact instanceof IpmsgContact)
							((IpmsgContact)contact).setStatus(IpmsgContact.UserStatus.OFFLINE);
				}
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

	/**
	 * Pobiera status konta.
	 *
	 * @return status
	 */
	public Contact.UserStatus getStatus()
	{
		return userStatus;
	}

	/**
	 * Ustawia status opisowy. Jeżeli nawiązane jest połączenie, powiadamia o
	 * tym wszystkich w sieci.
	 *
	 * @param textStatus nowy status opisowy
	 */
	public synchronized void setTextStatus(String textStatus)
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
	 * Powiadomienie wszystkich o statusie konta.
	 *
	 * @param command rodzaj polecenia powiadamiającego (COMM_ENTRY,
	 * COMM_ABSENCE, COMM_ANSENTRY lub COMM_EXIT)
	 * @see #statusNotify(int, String)
	 */
	protected void statusNotify(int command)
	{
		statusNotify(command, null);
	}

	/**
	 * Powiadamia wybraną osobę o statusie konta.
	 * 
	 * @param command rodzaj polecenia powiadamiającego (COMM_ENTRY,
	 * COMM_ABSENCE, COMM_ANSENTRY lub COMM_EXIT)
	 * @param ip IP osoby powiadamianej (<code>null</code>, jeżeli broadcast)
	 */
	protected void statusNotify(int command, String ip)
	{
		if (command != IpmsgPacket.COMM_ENTRY &&
			command != IpmsgPacket.COMM_ABSENCE &&
			command != IpmsgPacket.COMM_ANSENTRY &&
			command != IpmsgPacket.COMM_EXIT)
			throw new IllegalArgumentException();

		String userName = Configuration.getInstance().getNick();

		IpmsgPacket packet = new IpmsgPacket(userName);
		packet.ip = ip;
		packet.setCommand(command);
		packet.setFlag(IpmsgPacket.FLAG_FILEATTACH, true);

		if (this.userStatus == Contact.UserStatus.ONLINE)
			packet.setFlag(IpmsgPacket.FLAG_ABSENCE, false);
		else // IContact.UserStatus.BUSY
			packet.setFlag(IpmsgPacket.FLAG_ABSENCE, true);

		if (textStatus.isEmpty())
			packet.data = userName + '\0' + groupName;
		else
			packet.data = userName + '[' + textStatus + ']' + '\0' + groupName;
		try
		{
			sendPacket(packet);
		}
		catch (ConnectionLostException e)
		{
		}
	}

		// </editor-fold>

		// <editor-fold defaultstate="collapsed" desc="Odbieranie wiadomośći">

	/**
	 * Potwierdzone pakiety - kluczem jest <code>[id pakietu]/[id usera]</code>,
	 * wartością czas potwierdzenia (unix timestamp). Jeżeli minął
	 * confirmedMessagesTimeout, potwierdzenie jest nieaktualne (powinno być
	 * zignorowane, może być usunięte).
	 *
	 * @todo czyszczenie, co jakiś czas
	 */
	protected final HashMap<String, Long> confirmedMessages =
			new HashMap<String, Long>();

	/**
	 * Czas, po którym potwierdzenia pakietów tracą ważność. Po tym czasie
	 * pakiet o tym samym identyfikatorze zostanie znowu przyjęty.
	 */
	protected static final int confirmedMessagesTimeout = 300000; //5 minut

	/**
	 * Obsługa odbieranych wiadomości.
	 *
	 * @param packet odebrany pakiet z wiadomością
	 */
	private void gotMessage(IpmsgPacket packet)
	{
		boolean sendCheck = packet.getFlag(IpmsgPacket.FLAG_SENDCHECK);
		boolean broadcast =
				packet.getFlag(IpmsgPacket.FLAG_MULTICAST) ||
				packet.getFlag(IpmsgPacket.FLAG_MULTICAST_NEW) ||
				packet.getFlag(IpmsgPacket.FLAG_BROADCAST);
		String userName = Configuration.getInstance().getNick();

		if (sendCheck)
		{
			// potwierdzamy wszystkie, bo może jakiś nie doszedł
			IpmsgPacket confirmPacket = new IpmsgPacket(userName);
			confirmPacket.ip = packet.ip;
			confirmPacket.setCommand(IpmsgPacket.COMM_RECVMSG);
			confirmPacket.data = Long.toString(packet.packetNo);
			try
			{
				sendPacket(confirmPacket);
			}
			catch (ConnectionLostException e)
			{
			}

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

		//ignorujemy wiadomość odsyłaną automatycznie
		if (Configuration.getInstance().getIgnoreAutoResponses() &&
			packet.getFlag(IpmsgPacket.FLAG_AUTORET))
			return;

		IpmsgContact contact = (IpmsgContact)contactList.get(packet.ip + "@ipmsg");
		String authorName;
		if (contact == null)
			authorName = "Nieznajomy (" + packet.ip + ")";
		else
			authorName = contact.getName();

		ChatRoom chatRoom;
		if (broadcast)
			chatRoom = chatRoomList.getMain();
		else
			chatRoom = contact.getPrivateChatRoom();

		IncomingMessage message = new IncomingMessage(chatRoom, contact);

		String messageRAWContents, filesAdditionalSection = null;
		if (packet.data.indexOf('\0') < 0)
			messageRAWContents = packet.data;
		else
		{
			String[] splittedData = packet.data.split("\0");

			messageRAWContents = splittedData[0];
			if(splittedData.length > 1) // Byc moze sa kolejne sekcje
				filesAdditionalSection = splittedData[1];
		}

		if (filesAdditionalSection != null)
		{
			IpmsgFileListSendRequestHeader headerList = IpmsgFileListSendRequestHeader.fromRawData(filesAdditionalSection);
			for (IpmsgFileSendRequestHeader header: headerList.headerList)
			{
				IpmsgReceivedFile file = new IpmsgReceivedFile(header, contact, packet.packetNo);
				transferredFiles.add(file);
				message.addAttachment(file);
			}
		}

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
		message.setContents(messageContents.toString().trim());
		message.setRawContents(messageRAWContents);

		chatRoom.gotMessage(message);
	}

		// </editor-fold>

		// <editor-fold defaultstate="collapsed" desc="Wysyłanie wiadomości">

	private IpmsgPostMessageThread postMessageThread =
		new IpmsgPostMessageThread(this);

	/**
	 * Kolejkuje wiadomość do wysłania.
	 *
	 * @param message wiadomość do wysłania
	 * @return <code>true</code>, jeżeli wiadomość może być przesłana tym
	 * protokołem
	 */
	public boolean postMessage(OutgoingMessage message)
	{
		ChatRoom room = message.getChatRoom();

		// aktualnie nie obsługujemy pokoi konferencyjnych w tym protokole
		if (!(room instanceof PublicChatRoom) &&
			!(room instanceof PrivateChatRoom))
			return false;

		// użytkownik korzysta z innego protokołu
		if (room instanceof PrivateChatRoom &&
			!(((PrivateChatRoom)room).getContact() instanceof IpmsgContact))
			return false;

		postMessageThread.enqueueMessagePacket(new IpmsgMessagePacket(this, message));
		return true;
	}

		// </editor-fold>

	// </editor-fold>
}
