package views.swing;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;

import components.swing.*;
import protocols.*;
import resources.ResourceManager;
import tools.HeavyObjectLoader;
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
	protected final ChatRoomPanel chatRoomPanel;
	
	protected final JEditorPane messagesPane = new JEditorPane("text/html",
		"<div id=\"messages\"></div>");

	private final HashMap<Integer, DisplayedMessage> displayedMessages =
		new HashMap<Integer, DisplayedMessage>();
	private DisplayedMessage lastDisplayedMessage;

	protected final SimpleDateFormat messagesDateFormat = new SimpleDateFormat("HH:mm:ss");

	protected final HTMLDocument messagesDoc;
	protected final Element messagesElement;

	protected static final URL statusUnknown = ResourceManager.get("msgControls/status-unknown.png");
	protected static final URL statusPending = ResourceManager.get("msgControls/status-pending.png");
	protected static final URL statusFailed = ResourceManager.get("msgControls/status-failed.png");
	protected static final URL statusDelivered = ResourceManager.get("msgControls/status-delivered.png");
	protected static final URL switchRawMessage = ResourceManager.get("msgControls/switch-rawMessage.png");
	protected static final URL switchAttachments = ResourceManager.get("msgControls/switch-attachments.png");

	protected static final HeavyObjectLoader<JSaveFileChooser> attachmentSaveFileChooser =
		new HeavyObjectLoader<JSaveFileChooser>();

	static
	{
		attachmentSaveFileChooser.load(
			new HeavyObjectLoader.SwingInitializer<JSaveFileChooser>()
		{
			@Override
			public JSaveFileChooser buildSwing()
			{
				JSaveFileChooser chooser = new JSaveFileChooser();
				chooser.setDialogTitle("Zapisz załącznik jako...");
				return chooser;
			}
		});
	}

	public MessagesPanel(ChatRoomPanel chatRoomPanel)
	{
		this.setViewportView(messagesPane);
		this.setHorizontalScrollBarPolicy(
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.setBorder(BorderFactory.createEmptyBorder());
		this.chatRoomPanel = chatRoomPanel;

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
	protected final static String messageServiceContents = //data, autor, informacja
		"<p>(%s) <strong>%s</strong> <em>%s</em></p>";
	protected final static String messageAuthorLink = //id, autor
		"<a class=\"messageAuthorLink\" href=\"action:openPrivateRoom/%d\">%s</a>";

	protected final static String messageReceiversCell = //id, ikona (html)
		"<td id=\"message-receivers-%d\">%s</td>";
	protected final static String messageReceiversIcon =  //id, ikona (url), got, pending, failed
		"<p><a href=\"action:messageReceiversSwitch/%d\">" +
		"<img src=\"%s\" border=\"0\" width=\"10\" height=\"10\" "+
		"alt=\"Dostarczono: %d, wysyłanie: %d, nie dostarczono: %d\" /></a></p>";
	
	protected final static String messageSwitchRawCell = //id, ikona (switchRawMessage)
		"<td class=\"message-iconCell\"><p><a href=\"action:switchRawContents/%d\">" +
		"<img width=\"10\" height=\"10\" src=\"%s\" " +
		"alt=\"Pokaż/ukryj oryginalną wiadomość\" border=\"0\" /></a></p></td>";
	protected final static String messageSwitchAttachmentsCell = //id, ikona (switchAttachments)
		"<td class=\"message-iconCell\"><p><a href=\"action:switchAttachments/%d\">" +
		"<img width=\"10\" height=\"10\" src=\"%s\" " +
		"alt=\"Pokaż/ukryj załączniki\" border=\"0\" /></a></p></td>";

	protected final static String messageServiceCell = //id
		"<div class=\"message-serviceCell\" id=\"message-serviceCell-%d\"><p></p></div>";

	/**
	 * Element HTML listy załączników.
	 *
	 * Parametry (kolejno): fileName, messageID, fileNo, dodatkowe linki.
	 */
	protected final static String messageAttachment =
		"<p>%s (<a href=\"action:downloadAttachment/%d/%d\">pobierz</a>" +
		"%s)</p>";

	/**
	 * Element HTML linku otworzenia załącznika.
	 *
	 * Parametry (kolejno): messageID, fileNo.
	 */
	protected final static String messageAttachmentOpenAction =
		", <a href=\"action:openAttachment/%d/%d\">otwórz</a>";

	protected String formatMessageContents(Message message, boolean rawContents)
	{
		String contents;
		if (rawContents)
		{
			if (!(message instanceof IncomingMessage))
				throw new RuntimeException("Wiadomości nie przychodzące nie mogą posiadać wersji RAW");

			contents = escapeMessageContents(
				((IncomingMessage)message).getRawContents(), false);
		}
		else
			contents = escapeMessageContents(message.getContents(), true);

		String nick;
		if (chatRoomPanel.chatRoom instanceof PrivateChatRoom ||
			message instanceof OutgoingMessage)
			nick = HTMLUtilities.escape(message.getAuthorName());
		else
			nick = String.format(messageAuthorLink, message.id,
				HTMLUtilities.escape(message.getAuthorName()));

		if (contents.isEmpty())
		{
			String komunikat;

			if (((message instanceof IncomingMessage) && ((IncomingMessage)message).getAttachments() != null) ||
				((message instanceof OutgoingMessage) && ((OutgoingMessage)message).getAttachedFiles().size() > 0))
				komunikat = "przesyła plik";
			else
				komunikat = "wysłał pustą wiadomość";

			return String.format(messageServiceContents,
				messagesDateFormat.format(message.date),
				nick, komunikat
				);
		}
		else
		{
			return String.format(messageContents,
				messagesDateFormat.format(message.date),
				nick, contents
				);
		}
	}

	public void add(Message message)
	{
		DisplayedMessage dMesg = new DisplayedMessage(message);

		StringBuilder messageIcons = new StringBuilder();

		if (message instanceof OutgoingMessage)
		{
			OutgoingMessage out = (OutgoingMessage)message;

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

			if (out.getAttachedFiles().size() > 0)
			{
				messageIcons.append(
					String.format(messageSwitchAttachmentsCell,
						message.id,
						switchAttachments)
					);
			}
		}

		if (message instanceof IncomingMessage)
		{
			IncomingMessage inc = (IncomingMessage)message;
			if (inc.isRawContentsDifferent())
			{
				messageIcons.append(
					String.format(messageSwitchRawCell,
						message.id,
						switchRawMessage)
					);
			}

			if (inc.getAttachments() != null)
			{
				messageIcons.append(
					String.format(messageSwitchAttachmentsCell,
						message.id,
						switchAttachments)
					);
			}
		}

		String messageHTML = String.format(messageRow,
			message.id, message.id,
			formatMessageContents(message, false),
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

		StringBuilder serviceMsg = new StringBuilder();

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
					if (pending == 0 && got == 0 && failed == 0)
						serviceMsg.append("<p><b>Nieznany status wiadomości, lub brak odbiorców.</b></p>");
					else if (pending > 0)
						serviceMsg.append("<p><b>Wiadomość w trakcie wysyłania.</b></p>");
					else if (failed > 0)
						serviceMsg.append("<p><b>Wiadomość nie dostarczona do wszystkich odbiorców.</b></p>");
					else
						serviceMsg.append("<p><b>Wiadomość dostarczona.</b></p>");

					if (got > 0)
					{
						serviceMsg.append("<p>Dostarczono: ");
						serviceMsg.append(Contact.join(oMessage.getReceiversGot(), ", "));
						serviceMsg.append(".</p>");
					}
					if (pending > 0)
					{
						serviceMsg.append("<p>Wysyłanie: ");
						serviceMsg.append(Contact.join(oMessage.getReceiversPending(), ", "));
						serviceMsg.append(".</p>");
					}
					if (failed > 0)
					{
						serviceMsg.append("<p>Nie dostarczono: ");
						serviceMsg.append(Contact.join(oMessage.getReceiversFailed(), ", "));
						serviceMsg.append(".</p>");
					}

					HTMLUtilities.setInnerHTML(dMesg.getServiceCell(),
						serviceMsg.toString(), false);
					break;
				case ATTACHMENTS:
					for (File f : oMessage.getAttachedFiles())
					{
						serviceMsg.append("<p>");
						serviceMsg.append(f.getName());
						serviceMsg.append("</p>");
					}

					if (serviceMsg.length() == 0)
						throw new RuntimeException("Powinien być jakiś załącznik");

					HTMLUtilities.setInnerHTML(dMesg.getServiceCell(),
						serviceMsg.toString(), false);
					break;
			}
		}
		else if (message instanceof IncomingMessage)
		{
			IncomingMessage iMessage = (IncomingMessage)message;

			switch (dMesg.getServiceCellUsage())
			{
				case ATTACHMENTS:
					ReceivedFile[] receivedFiles = iMessage.getAttachments();

					if (receivedFiles == null)
					{
						HTMLUtilities.setInnerHTML(dMesg.getServiceCell(),
							"<p>Wszystkie załączniki zostały już pobrane.</p>", false);
						break;
					}
					assert(receivedFiles.length > 0);

					if (dMesg.renderedAttachmentsCount == receivedFiles.length)
						break;
					dMesg.renderedAttachmentsCount = receivedFiles.length;

					for (int i = 0; i < receivedFiles.length; i++)
					{
						String openLink = "";
						if (receivedFiles[i].isFile())
							openLink = String.format(messageAttachmentOpenAction,
								message.id, i);
						serviceMsg.append(String.format(
								messageAttachment, receivedFiles[i].getFileName(),
								message.id, i, openLink
							));
					}

					HTMLUtilities.setInnerHTML(dMesg.getServiceCell(),
						serviceMsg.toString(), false);
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
			else if (cmd[0].equals("switchAttachments"))
			{
				int id = Integer.parseInt(cmd[1]);
				DisplayedMessage dMesg = displayedMessages.get(id);
				assert(dMesg != null);

				dMesg.switchServiceCell(ServiceCellUsage.ATTACHMENTS);
			}
			else if (cmd[0].equals("downloadAttachment") || cmd[0].equals("openAttachment"))
			{
				int messageId = Integer.parseInt(cmd[1]);
				int fileNo = Integer.parseInt(cmd[2]);

				DisplayedMessage dMesg = displayedMessages.get(messageId);
				assert(dMesg != null);
				IncomingMessage iMesg = (IncomingMessage)dMesg.message;

				ReceivedFile[] attachments = iMesg.getAttachments();
				assert(attachments.length > fileNo);

				ReceivedFile attachment = attachments[fileNo];

				if (attachment.getState() != TransferredFile.State.READY)
				{
					String message;
					if (attachment.getState() == TransferredFile.State.TRANSFERRING ||
						attachment.getState() == TransferredFile.State.WAITING_FOR_CONNECTION)
						message = "Już pobierasz ten plik...";
					else
						message = "Już wcześniej pobrałeś ten plik...";
					JOptionPane.showMessageDialog(messagesPanel,
						message, "Pobieranie pliku",
						JOptionPane.INFORMATION_MESSAGE);
				}
				else if (cmd[0].equals("downloadAttachment"))
					downloadAttachment(attachment);
				else // cmd[0].equals("openAttachment")
					openAttachment(attachment);
			}
			else if (cmd[0].equals("openPrivateRoom"))
			{
				int id = Integer.parseInt(cmd[1]);
				
				DisplayedMessage dMesg = displayedMessages.get(id);
				assert(dMesg != null);
				IncomingMessage iMesg = (IncomingMessage)dMesg.message;
				chatRoomPanel.chatRoomsView.showRoom(iMesg.getAuthor().getPrivateChatRoom());
			}
			else
				throw new RuntimeException("nieznane polecenie: " + cmdStr);
		}
	}

	/**
	 * To jest mały memleak, aby otwarte pliki nie były usuwane zbyt szybko.
	 * Niektóre aplikacje oddają sterowanie po otworzeniu pliku.
	 * Np. w przypadku archiwów, może ono zostać otwarte w zewnętrznym
	 * programie, sterowanie zostanie przekazane z powrotem do naszego programu,
	 * wykonywany wątek się zakończy, a garbage collector (zależnie od
	 * implementacji) usunie obiekt pliku wraz z nim. Ten kontener zapobiega
	 * oznaczeniu pliku jako obiektu bez powiązań.
	 */
	private static Vector<File> openedFiles = new Vector<File>();

	private void openAttachment(final ReceivedFile attachment)
	{
		// wykrywanie rozszerzenia (bez niego nie będzie się dało określić, jak
		// otworzyć plik
		String ext = null;
		String[] fileEl = attachment.getFileName().split("\\.");
		if (fileEl.length > 0)
		{
			ext = fileEl[fileEl.length - 1];
			if (!ext.matches("[a-z0-9A-Z]{1,10}"))
				ext = null;
		}
		if (ext == null)
		{
			JOptionPane.showMessageDialog(messagesPanel,
				"Nie można określić typu pliku. Spróbuj go zapisać i ręcznie uruchomić.", "Nieznany typ pliku",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		// przygotowanie miejsca na plik
		final File tmp;
		try
		{
			tmp = File.createTempFile("UniLANChat-viewAttachment-", "." + ext);
			tmp.deleteOnExit();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		//pobranie pliku
		attachment.addObserver(new Observer()
		{
			public void update(Observable o, Object arg)
			{
				if (!tmp.exists())
					throw new RuntimeException("Pobrany plik nie istnieje");

				TransferredFile.State attState = attachment.getState();

				if (attState == TransferredFile.State.ERROR)
					attachment.deleteObserver(this);
				else if (attState != TransferredFile.State.COMPLETED)
					return;
				attachment.deleteObserver(this);

				// plik został pobrany - próbujemy go otworzyć
				if (SystemProcesses.openFile(tmp))
				{
					openedFiles.add(tmp);
					tmp.deleteOnExit();
					return;
				}

				// nie udało się otworzyć - przenosimy tam, gdzie user sobie zażyczy
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						JOptionPane.showMessageDialog(messagesPanel,
							"Nie udało się wyświetlić pliku \"" +
							attachment.getFileName() +
							"\". Spróbuj go zapisać i ręcznie uruchomić.", "Nieznany typ pliku",
							JOptionPane.WARNING_MESSAGE);

						JFileChooser fileChooser = attachmentSaveFileChooser.get();
						File saveTo;
						synchronized(attachmentSaveFileChooser)
						{
							fileChooser.setSelectedFile(new File(attachment.getFileName()));
							if (fileChooser.showSaveDialog(messagesPanel) != JFileChooser.APPROVE_OPTION)
								return;
							saveTo = fileChooser.getSelectedFile();
						}
						if (saveTo.exists())
							saveTo.delete();

						try
						{
							SystemProcesses.copyFile(tmp, saveTo);
						}
						catch (IOException e)
						{
							JOptionPane.showMessageDialog(messagesPanel,
								"Nie udało się przenieść pliku.", "Nieznany typ pliku",
								JOptionPane.ERROR_MESSAGE);
						}

						tmp.delete();
					}
				});
			}
		});
		attachment.receive(tmp);
	}

	private void downloadAttachment(final ReceivedFile attachment)
	{
		attachmentSaveFileChooser.invokeSwingWhenReady(new Runnable()
		{
			public void run()
			{
				JFileChooser fileChooser = attachmentSaveFileChooser.getIfReady();
				File saveTo = null;
				synchronized(attachmentSaveFileChooser)
				{
					fileChooser.setSelectedFile(new File(attachment.getFileName()));
					if (fileChooser.showSaveDialog(messagesPanel) == JFileChooser.APPROVE_OPTION)
						saveTo = fileChooser.getSelectedFile();
				}
				if (saveTo != null)
				{
					if (saveTo.exists())
					{
						if (!saveTo.canWrite())
							throw new RuntimeException("Plik istnieje i nie można go nadpisać");
						if (!saveTo.delete())
							throw new RuntimeException("Nie można usunąć pliku");
						if (saveTo.exists())
							throw new RuntimeException("Plik się nie usunął");
					}

					attachment.receive(saveTo);
					chatRoomPanel.chatRoomsView.mainView.mainMenu.
						showFileTransfersView();
				}
			}
		});
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
		RECEIVERS,

		/**
		 * W komórce serwisowej wyświetlane są załączniki.
		 */
		ATTACHMENTS
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

		/**
		 * Pozwala na nie przerysowywanie załączników, jeżeli ich lista się nie
		 * zmieniła. Załączniki można tylko dodawać (na samym początku), albo
		 * usuwać (po jednym), więc przechowywanie ich liczby może się sprawdzić.
		 */
		public int renderedAttachmentsCount = 0;

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
			renderedAttachmentsCount = 0;
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

			HTMLUtilities.setInnerHTML(messageContentCell,
				formatMessageContents(message, isRAWContentsShown),
				false);
		}
	}
}
