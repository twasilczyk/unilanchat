package protocols;

import java.util.*;

/**
 * Wiadomość przychodząca, czyli odebrana przez jedno z kont, do przeczytania
 * przez użytkownika.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class IncomingMessage extends Message
{
	/**
	 * Nazwa autora wiadomości.
	 */
	protected final Contact author;

	/**
	 * Oryginalna (przed ewentualnymi automatycznymi modyfikacjami) treść
	 * wiadomości.
	 */
	protected String rawContents;

	protected final Vector<ReceivedFile> attachments = new Vector<ReceivedFile>();
	private final AttachmentObserver attachmentObserver = new AttachmentObserver();

	/**
	 * Nowa wiadomość przychodząca.
	 *
	 * @param room pokój, w ramach którego odebrano wiadomość
	 * @param author autor wiadomości
	 */
	public IncomingMessage(ChatRoom room, Contact author)
	{
		super(room);
		if(author == null)
			throw new NullPointerException();
		this.author = author;
	}

	/**
	 * Zwraca nazwę autora wiadomości.
	 *
	 * @return nazwa autora
	 */
	public String getAuthorName()
	{
		return author.getName();
	}

	/**
	 * Zwraca autora wiadomości.
	 *
	 * @return autor wiadomości
	 */
	public Contact getAuthor()
	{
		return author;
	}

	/**
	 * Zapisuje surową wersję wiadomości, jeżeli jest różna od wyświetlanej.
	 *
	 * @param contents surowa wiadomość
	 */
	public void setRawContents(String contents)
	{
		if (contents == null)
			throw new NullPointerException();
		rawContents = contents;
	}

	/**
	 * Zwraca surową wersję wiadomości (lub wyświetlaną, jeżeli surowa nie
	 * istnieje).
	 *
	 * @return surowa wiadomość
	 */
	public String getRawContents()
	{
		if (rawContents == null)
			return getContents();
		return rawContents;
	}

	public void addAttachment(ReceivedFile attachment)
	{
		if (attachment == null)
			throw new NullPointerException();
		synchronized (attachments)
		{
			attachments.add(attachment);
			attachment.addObserver(attachmentObserver);
		}
	}

	public ReceivedFile[] getAttachments()
	{
		if (attachments.size() == 0)
			return null;
		synchronized (attachments)
		{
			ReceivedFile[] ret = new ReceivedFile[attachments.size()];
			return attachments.toArray(ret);
		}
	}

	/**
	 * Sprawdza, czy wersja surowa jest istotnie (pomijając białe znaki na
	 * końcach) różna od wyświetlanej.
	 *
	 * @return <code>true</code>, jeżeli wersja surowa jest różna od oryginalnej
	 */
	public boolean isRawContentsDifferent()
	{
		if (rawContents == null)
			return false;
		return !rawContents.trim().equals(this.contents.trim());
	}

	class AttachmentObserver implements Observer
	{
		public void update(Observable o, Object arg)
		{
			ReceivedFile rFile = (ReceivedFile)o;
			if (rFile.getState() == TransferredFile.State.COMPLETED)
				synchronized (attachments)
				{
					attachments.remove(rFile);
				}
			notifyObservers();
		}
	}
}
