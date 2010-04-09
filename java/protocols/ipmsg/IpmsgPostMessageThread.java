package protocols.ipmsg;

import java.util.*;
import protocols.ConnectionLostException;

import protocols.Contact;

/**
 * Wątek wysyłania wiadomości jest inicjowany i kończony w ramach
 * zarządzania wątkiem połączenia.
 *
 * @see IpmsgAccount.setConnected(boolean)
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
class IpmsgPostMessageThread extends Thread
{
	/**
	 * Konto, w ramach którego jest uruchomiony wątek.
	 */
	protected final IpmsgAccount ipmsgAccount;

	/**
	 * Lista wiadomości do wysłania.
	 * 
	 * Klucz: id pakietu
	 * Wartość: obiekt wiadomości (zawiera także wspomniany klucz)
	 */
	protected final HashMap<Long, IpmsgMessagePacket> messages =
			new HashMap<Long, IpmsgMessagePacket>();

	/**
	 * Główny konstruktor.
	 * 
	 * @param ipmsgAccount konto, w ramach którego ma być uruchomiony wątek
	 */
	public IpmsgPostMessageThread(IpmsgAccount ipmsgAccount)
	{
		super("ULC-Ipmsg-PostMessageThread");
		this.ipmsgAccount = ipmsgAccount;
		setDaemon(true);
		start();
	}

	@Override public synchronized void run()
	{
		Vector<IpmsgMessagePacket> removeMessages = new Vector<IpmsgMessagePacket>();

		while (true) //TODO: wychodzić, jak konto nie istnieje? a moze przez interrupt?
		{
			if (ipmsgAccount.isConnected())
			{
				removeMessages.clear();
				for (IpmsgMessagePacket messagePacket : messages.values())
					if (!messagePacket.isNextTryAvailable())
					{
						removeMessages.add(messagePacket);

						for (IpmsgContact receiver : messagePacket.receivers)
							receiver.setStatus(Contact.UserStatus.OFFLINE);
						messagePacket.message.notifyReceiversFailed(messagePacket.receivers);
					}
				for (IpmsgMessagePacket messagePacket : removeMessages)
					messages.remove(messagePacket.getID());
				for (IpmsgMessagePacket messagePacket : messages.values())
					for (IpmsgContact receiver : messagePacket.receivers)
					{
						messagePacket.packet.ip = receiver.getIP();

						try
						{
							ipmsgAccount.sendPacket(messagePacket.packet);
						}
						catch (ConnectionLostException e)
						{
							break;
						}
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
	 * @param messagePacket wiadomość do wysłania
	 */
	public synchronized void enqueueMessagePacket(IpmsgMessagePacket messagePacket)
	{
		messages.put(messagePacket.getID(), messagePacket);
		if (getState().equals(Thread.State.WAITING))
			// WAITING - bez ustawionego timeoutu
			notifyAll();
	}

	/**
	 * Potwierdza odebranie wiadomości. Wywołana, jeżeli kontakt wysłał
	 * COMM_RECVMSG.
	 *
	 * @param contact kontakt, który potwierdza odebranie wiadomości
	 * @param id ID potwierdzanej wiadomości
	 */
	public synchronized void confirmMessage(IpmsgContact contact, long id)
	{
		IpmsgMessagePacket messagePacket = messages.get(id);
		if (messagePacket == null)
			return;
		if (messagePacket.receivers.remove(contact))
			// true, jeżeli wcześniej nie potwierdzone
			messagePacket.message.notifyReceiverGot(contact);
		if (messagePacket.receivers.isEmpty())
			messages.remove(id);
	}
}
