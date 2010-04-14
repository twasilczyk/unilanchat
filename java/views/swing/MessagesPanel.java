package views.swing;

import java.awt.Color;
import java.awt.event.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;

import components.swing.JStickyScrollPane;
import protocols.*;
import resources.ResourceManager;
import tools.html.*;
import tools.systemintegration.SystemProcesses;

/**
 * Panel z listą wiadomości.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class MessagesPanel extends JStickyScrollPane
{
	protected final MessagesPanel messagesPanel = this;
	protected final JEditorPane messagesPane = new JEditorPane("text/html",
		"<div id=\"messages\"></div>");

	private final HashMap<Integer, DisplayedMessage> displayedMessages =
		new HashMap<Integer, DisplayedMessage>();
	private DisplayedMessage lastDisplayedMessage;

	protected final SimpleDateFormat messagesDateFormat = new SimpleDateFormat("HH:mm:ss");

	protected final HTMLDocument messagesDoc;
	protected final Element messagesElement;

	protected static final URL statusUnknown = ResourceManager.get("msgStatus-unknown.png");
	protected static final URL statusPending = ResourceManager.get("msgStatus-pending.png");
	protected static final URL statusFailed = ResourceManager.get("msgStatus-failed.png");
	protected static final URL statusDelivered = ResourceManager.get("msgStatus-delivered.png");
	protected static final URL switchRawMessage = ResourceManager.get("switchRawMessage.png");

	public MessagesPanel()
	{
		this.setViewportView(messagesPane);
		this.setHorizontalScrollBarPolicy(
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.setBorder(BorderFactory.createEmptyBorder());

		// przygotowanie odnośników DOM
		messagesDoc = (HTMLDocument)messagesPane.getDocument();
		messagesElement = messagesDoc.getElement("messages");
		assert(messagesElement != null);

		// ustawienie wyglądu pola rozmowy
		messagesPane.setBackground(Color.WHITE);
		messagesPane.setEditable(false);
		HTMLUtilities.loadCSSRules(messagesDoc.getStyleSheet(),
			ResourceManager.get("chatRoom.css"));

		// ustawienie słuchaczy pola rozmowy
		MessagesPaneListener messagesPaneListener = new MessagesPaneListener();
		messagesPane.addHyperlinkListener(messagesPaneListener);
		messagesPane.addKeyListener(messagesPaneListener);
		new HyperlinkHighlighter(messagesPane);

		// Pole nie jest edytowalne przez użytkownika, a przesuwanie kursora
		// przy edycji powodowało "skakanie" pasków przewijania.
		((DefaultCaret)messagesPane.getCaret()).setUpdatePolicy(
				DefaultCaret.NEVER_UPDATE);
	}

	@Override public synchronized void addKeyListener(KeyListener l)
	{
		super.addKeyListener(l);
		messagesPane.addKeyListener(l);
	}

	protected final static String messageRow = //id, id, zawartość, ikonki
		"<table id=\"messageRow-%d\" valign=\"top\" class=\"messageRow\">" +
		"<tr><td class=\"messageContent\" id=\"messageContent-%d\">%s</td>" +
		"%s</tr></table>";
	protected final static String messageContents = //data, autor, treść
		"<p>(%s) <strong>%s:</strong> %s</p>";
	protected final static String messageReceiversCell = //id, ikona (html)
		"<td id=\"message-receivers-%d\">%s</td>";
	protected final static String messageReceiversIcon =  //id, ikona (url), got, pending, failed
		"<p><a href=\"action:messageReceiversSwitch/%d\">" +
		"<img src=\"%s\" border=\"0\" width=\"10\" height=\"10\" "+
		"alt=\"Dostarczono: %d, wysyłanie: %d, nie dostarczono: %d\" /></a></p>";
	protected final static String messageSwitchRawCell = //id
		"<td><p><a href=\"action:switchRawContents/%d\">" +
		"<img width=\"10\" height=\"10\" src=\"" + switchRawMessage + 
		"\" alt=\"Pokaż/ukryj oryginalną wiadomość\" border=\"0\" /></a></p></td>";
	protected final static String messageServiceCell = //id
		"<div class=\"message-serviceCell\" id=\"message-serviceCell-%d\"><p></p></div>";

	public void add(Message message)
	{
		DisplayedMessage dMesg = new DisplayedMessage(message);

		StringBuilder messageIcons = new StringBuilder();

		if (message instanceof OutgoingMessage)
		{
			messageIcons.append(
				String.format(messageReceiversCell,
					message.id,
					String.format(messageReceiversIcon,
						message.id,
						statusUnknown,
						0, 0, 0
						)
					)
				);
		}

		if (message instanceof IncomingMessage)
		{
			if (((IncomingMessage)message).isRawContentsDifferent())
			{
				messageIcons.append(
					String.format(messageSwitchRawCell, message.id)
					);
			}
		}

		String messageHTML = String.format(messageRow,
			message.id, message.id,
		String.format(messageContents,
				messagesDateFormat.format(message.date),
				HTMLUtilities.escape(message.getAuthor()),
				escapeMessageContents(message.getContents(), true)
				),
			messageIcons
			);

		// if dla before/afterEnd to obejście dla buga dublowania tabelek
		if (lastDisplayedMessage == null)
			HTMLUtilities.insertBeforeEnd(messagesElement, messageHTML);
		else if (lastDisplayedMessage.serviceCell != null)
			HTMLUtilities.insertAfterEnd(lastDisplayedMessage.serviceCell, messageHTML);
		else
			HTMLUtilities.insertAfterEnd(lastDisplayedMessage.messageRow, messageHTML);

		dMesg.messageRow = messagesDoc.getElement("messageRow-" + message.id);
		assert(dMesg.messageRow != null);

		dMesg.messageContentCell = messagesDoc.getElement("messageContent-" + message.id);
		assert(dMesg.messageContentCell != null);

		if (message instanceof OutgoingMessage)
		{
			dMesg.messageReceiversIconCell = messagesDoc.getElement(
				"message-receivers-" + message.id);
			assert(dMesg.messageReceiversIconCell != null);
		}

		// tutaj można by było wywoływać update, ale w ChatController najpierw
		// dodajemy wiadomość do pokoju, więc update i tak się wykona

		displayedMessages.put(message.id, dMesg);
		lastDisplayedMessage = dMesg;
	}

	public void update(Message message)
	{
		DisplayedMessage dMesg = displayedMessages.get(message.id);
		assert(dMesg != null);

		if (message instanceof OutgoingMessage)
		{
			OutgoingMessage oMessage = (OutgoingMessage)message;

			//ikonka statusu dostarczenia wiadomości
			URL statusIcon;
			int pending = oMessage.getReceiversPendingCount();
			int got = oMessage.getReceiversGotCount();
			int failed = oMessage.getReceiversFailedCount();
			if (pending == 0 && got == 0 && failed == 0)
				statusIcon = statusUnknown;
			else if (pending > 0)
				statusIcon = statusPending;
			else if (failed > 0)
				statusIcon = statusFailed;
			else
				statusIcon = statusDelivered;

			HTMLUtilities.setInnerHTML(dMesg.messageReceiversIconCell,
				String.format(messageReceiversIcon,
					message.id,
					statusIcon,
					got, pending, failed),
				false);

			switch (dMesg.getServiceCellUsage())
			{
				case RECEIVERS:
					String serviceMsg = "";
					if (pending == 0 && got == 0 && failed == 0)
						serviceMsg = "<p><b>Nieznany status wiadomości, lub brak odbiorców.</b></p>";
					else if (pending > 0)
						serviceMsg = "<p><b>Wiadomość w trakcie wysyłania.</b></p>";
					else if (failed > 0)
						serviceMsg = "<p><b>Wiadomość nie dostarczona do wszystkich odbiorców.</b></p>";
					else
						serviceMsg = "<p><b>Wiadomość dostarczona.</b></p>";

					if (got > 0)
						serviceMsg += "<p>Dostarczono: " +
							Contact.join(oMessage.getReceiversGot(), ", ") + ".</p>";
					if (pending > 0)
						serviceMsg += "<p>Wysyłanie: " +
							Contact.join(oMessage.getReceiversPending(), ", ") + ".</p>";
					if (failed > 0)
						serviceMsg += "<p>Nie dostarczono: " +
							Contact.join(oMessage.getReceiversFailed(), ", ") + ".</p>";

					HTMLUtilities.setInnerHTML(dMesg.getServiceCell(),
						serviceMsg, false);
					break;
			}
		}
	}

	protected static String escapeMessageContents(String plaintext, boolean tagURLs)
	{
		if (tagURLs)
			return HTMLUtilities.nl2br(HTMLUtilities.tagURLs(HTMLUtilities.escape(plaintext)));
		else
			return HTMLUtilities.nl2br(HTMLUtilities.escape(plaintext));
	}

	class MessagesPaneListener implements HyperlinkListener, KeyListener
	{
		public void hyperlinkUpdate(HyperlinkEvent e)
		{
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
			{
				String url = e.getDescription();
				if (url.startsWith("action:"))
					actionPerformed(url.substring(7));
				else
					SystemProcesses.openURL(url);
			}
		}

		public void keyTyped(KeyEvent e)
		{
			//TODO: obsługa scrolla strzałkami
		}

		public void keyPressed(KeyEvent e) { }
		public void keyReleased(KeyEvent e) { }

		public void actionPerformed(String cmdStr)
		{
			String[] cmd = cmdStr.split("/");
			if (cmd[0].equals("messageReceiversSwitch"))
			{
				int id = Integer.parseInt(cmd[1]);
				DisplayedMessage dMesg = displayedMessages.get(id);
				assert(dMesg != null);

				dMesg.switchServiceCell(ServiceCellUsage.RECEIVERS);
			}
			else if (cmd[0].equals("switchRawContents"))
			{
				int id = Integer.parseInt(cmd[1]);
				DisplayedMessage dMesg = displayedMessages.get(id);
				assert(dMesg != null);

				dMesg.switchRAWContents();
			}
			else
				throw new RuntimeException("nieznane polecenie: " + cmdStr);
		}
	}

	/**
	 * Co jest wyświetlane w komórce serwisowej danej wiadomości.
	 */
	public enum ServiceCellUsage
	{
		/**
		 * Komórka serwisowa niewidoczna.
		 */
		UNUSED,
		
		/**
		 * W komórce serwisowej wyświetlana jest lista odbiorców.
		 */
		RECEIVERS
	};

	class DisplayedMessage
	{
		public final Message message;

		/**
		 * Jeżeli wiadomość wychodząca i protokół obsługuje śledzenie dostarczania
		 * wiadomości - element ikony statusu wiadomości.
		 */
		public Element messageReceiversIconCell;

		public Element messageRow;

		public Element messageContentCell;

		protected Element serviceCell;
		protected ServiceCellUsage serviceCellUsage = ServiceCellUsage.UNUSED;

		public DisplayedMessage(Message message)
		{
			this.message = message;
		}

		public synchronized Element getServiceCell()
		{
			if (serviceCell != null)
				return serviceCell;

			if (messageRow == null)
				throw new NullPointerException();

			HTMLUtilities.insertAfterEnd(messageRow,
				String.format(messageServiceCell, message.id));

			serviceCell = messagesDoc.getElement("message-serviceCell-" + message.id);

			return serviceCell;
		}

		public synchronized void removeServiceCell()
		{
			assert(serviceCellUsage.equals(ServiceCellUsage.UNUSED) ==
				(serviceCell == null));

			if (serviceCell == null)
				return;

			HTMLUtilities.remove(serviceCell);

			serviceCell = null;
			serviceCellUsage = ServiceCellUsage.UNUSED;
		}

		public void showInServiceCell(ServiceCellUsage what)
		{
			if (what.equals(serviceCellUsage))
				return;
			serviceCellUsage = what;
			update(message);
		}

		public void switchServiceCell(ServiceCellUsage what)
		{
			if (!getServiceCellUsage().equals(what))
				showInServiceCell(what);
			else
				removeServiceCell();
		}

		public ServiceCellUsage getServiceCellUsage()
		{
			return serviceCellUsage;
		}

		boolean isRAWContentsShown = false;

		public void switchRAWContents()
		{
			if (messageContentCell == null)
				throw new NullPointerException();

			if (!(message instanceof IncomingMessage))
				throw new RuntimeException("Wiadomości nie przychodzące nie mogą posiadać wersji RAW");

			isRAWContentsShown = !isRAWContentsShown;

			String contents;
			if (isRAWContentsShown)
				contents = escapeMessageContents(
					((IncomingMessage)message).getRawContents(), false);
			else
				contents = escapeMessageContents(message.getContents(), true);

			HTMLUtilities.setInnerHTML(messageContentCell,
				String.format(messageContents,
					messagesDateFormat.format(message.date),
					HTMLUtilities.escape(message.getAuthor()),
					contents
					),
				false);
		}
	}
}
