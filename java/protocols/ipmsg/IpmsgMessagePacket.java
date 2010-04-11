package protocols.ipmsg;

import java.util.Vector;

import main.Configuration;
import protocols.*;

/**
 * Klasa pomocnicza przechowująca wiadomość do wysłania, listę odbiorców
 * oraz licznik prób.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
class IpmsgMessagePacket
{
	/**
	 * Maksymalna ilość prób, w ilu jest wysyłana wiadomość.
	 */
	protected final static int maxSendTries = 5;

	/**
	 * Pakiet ipmsg, którym jest wysyłana wiadomość.
	 */
	public final IpmsgPacket packet =
		new IpmsgPacket(Configuration.getInstance().getNick());

	/**
	 * Lista odbiorców.
	 */
	public final Vector<IpmsgContact> receivers = new Vector<IpmsgContact>();

	/**
	 * Wysyłana wiadomość wychodząca.
	 */
	public final OutgoingMessage message;

	/**
	 * Konto, w ramach którego jest wysyłana wiadomość.
	 */
	public final IpmsgAccount ipmsgAccount;

	/**
	 * Aktualna ilość prób wysyłania.
	 */
	protected int sendTries = 0;

	/**
	 * Główny konstruktor.
	 *
	 * @param ipmsgAccount konto, w ramach którego nadawana jest wiadomość
	 * @param message wiadomość do nadania
	 */
	public IpmsgMessagePacket(IpmsgAccount ipmsgAccount, OutgoingMessage message)
	{
		this.message = message;
		this.ipmsgAccount = ipmsgAccount;
		ChatRoom room = message.getChatRoom();

		if (room.id.isEmpty())
		{
			receivers.addAll(ipmsgAccount.getOnlineContacts()); //TODO: contactList ma być pobierana z chatRoom jako członkowie rozmowy
			packet.setFlag(IpmsgPacket.FLAG_MULTICAST, true); // FLAG_BROADCAST ignoruje flagę FLAG_SENDCHECK
		}
		else
		{
			assert(room.id.endsWith("@ipmsg"));
			if (room instanceof PrivateChatRoom)
				receivers.add((IpmsgContact)((PrivateChatRoom)room).getContact());
			else
				throw new UnsupportedOperationException("Aktualnie nie są obsługiwane pokoje multicast");
		}
		message.notifyReceiversAdded(receivers);

		packet.setCommand(IpmsgPacket.COMM_SENDMSG);
		packet.setFlag(IpmsgPacket.FLAG_SENDCHECK, true);

		packet.data = message.getContents();
	}

	/**
	 * Pobiera identyfikator wiadomości (czyli pakietu wychodzącego).
	 *
	 * @return identyfikator
	 */
	public long getID()
	{
		return packet.packetNo;
	}

	/**
	 * Sprawdza, czy można jeszcze raz spróbować wysłać wiadomość - jeżeli
	 * tak, zaznacza kolejną próbę jako wykonaną.
	 *
	 * @return <code>true</code>, jeżeli wykonać próbę
	 */
	public synchronized boolean isNextTryAvailable()
	{
		if (sendTries >= maxSendTries)
			return false;
		sendTries++;
		return true;
	}
}