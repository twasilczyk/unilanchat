package views.swing;

import tools.html.HTMLUtilities;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.event.*;

import javax.swing.text.html.HTMLDocument;
import protocols.*;
import resources.ResourceManager;
import tools.*;
import tools.html.HyperlinkHighlighter;

/**
 * Panel pokoju rozmów, zawierający listę wiadomości oraz pole do ich wysyłania
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ChatRoomPanel extends JPanel implements SetListener<Message>
{
	protected final ChatRoom chatRoom;
	protected final ChatRoomsView chatRoomsView;

	protected final JEditorPane messagesPane = new JEditorPane("text/html", "");
	protected final JScrollPane messagesScrollPane = new JScrollPane(messagesPane);
	protected final StringBuilder messagesText = new StringBuilder();
	protected final SimpleDateFormat messagesDateFormat = new SimpleDateFormat("HH:mm:ss");

	protected final JTextPane inputPane = new JTextPane();
	protected final JScrollPane inputScrollPane = new JScrollPane(inputPane);

	protected final static int minInputHeight = 25;
	protected final static int maxInputHeight = 100;

	protected boolean unread = false;

	public ChatRoomPanel(ChatRoom chatRoom, ChatRoomsView chatRoomsView)
	{
		this.chatRoomsView = chatRoomsView;
		this.chatRoom = chatRoom;
		chatRoom.getMessagesVector().addSetListener(this);

		this.setLayout(new BorderLayout(0, 4));
		this.add(messagesScrollPane, BorderLayout.CENTER);
		this.add(inputScrollPane, BorderLayout.SOUTH);

		messagesPane.setBackground(Color.WHITE);
		messagesPane.setEditable(false);
		messagesScrollPane.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		InputPaneListener inputPaneListener = new InputPaneListener();
		inputPane.setBackground(Color.WHITE);
		inputPane.addKeyListener(inputPaneListener);
		inputScrollPane.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		inputScrollPane.setPreferredSize(new Dimension(50, minInputHeight));

		HTMLUtilities.loadCSSRules(((HTMLDocument)messagesPane.getDocument()).getStyleSheet(), ResourceManager.get("chatRoom.css"));

		MessagesPaneListener messagesPaneListener = new MessagesPaneListener();
		messagesPane.addHyperlinkListener(messagesPaneListener);
		messagesPane.addKeyListener(messagesPaneListener);
		new HyperlinkHighlighter(messagesPane);
	}

	class MessagesPaneListener implements HyperlinkListener, KeyListener
	{
		public void hyperlinkUpdate(HyperlinkEvent e)
		{
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
				HTMLUtilities.openURL(e.getURL().toString());
		}

		public void keyTyped(KeyEvent e)
		{
			if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
			{
				chatRoomsView.setVisible(false);
				return;
			}
			if ((e.getKeyChar() >= 32 || e.getKeyChar() == 10) && !inputPane.hasFocus())
			{
				inputPane.requestFocus();
				if (e.getKeyChar() >= 32)
					inputPane.replaceSelection(String.valueOf(e.getKeyChar()));
			}
		}

		public void keyPressed(KeyEvent e) { }
		public void keyReleased(KeyEvent e) { }
	}

	class InputPaneListener implements KeyListener
	{
		protected final Dimension scrollPaneDimension = new Dimension();
		protected int currHeight = minInputHeight;

		protected void setScrollPaneHeight(int height)
		{
			currHeight = height;
			scrollPaneDimension.width = inputScrollPane.getWidth();
			scrollPaneDimension.height = height;
			inputScrollPane.setPreferredSize(scrollPaneDimension);
			chatRoomsView.validate();
		}

		public void keyPressed(KeyEvent e)
		{
			if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
			{
				chatRoomsView.setVisible(false);
				return;
			}
			if (e.getKeyChar() == KeyEvent.VK_ENTER)
			{
				if (e.isShiftDown())
					inputPane.replaceSelection("\n");
				else
				{
					String sendText;
					synchronized (inputPane)
					{
						sendText = inputPane.getText().trim();
						inputPane.setText("");
					}

					if (!sendText.isEmpty())
						chatRoomsView.chatController.sendMessage(chatRoom, sendText);
					setScrollPaneHeight(minInputHeight);
				}
			}
		}

		public void keyTyped(KeyEvent e)
		{
			// należy ponownie wyczyścić pole edycji, ponieważ dodał się do
			// niego enter, naciskany przy wysyłaniu
			if (e.getKeyChar() == KeyEvent.VK_ENTER && !e.isShiftDown())
				inputPane.setText("");
		}

		public void keyReleased(KeyEvent e)
		{
			inputPane.validate();
			if (currHeight < inputPane.getHeight() - 5)
			{
				int newHeight = inputPane.getHeight() + 5;
				if (newHeight < minInputHeight)
					newHeight = minInputHeight;
				else if (newHeight > maxInputHeight)
					newHeight = maxInputHeight;
				setScrollPaneHeight(newHeight);
			}
		}
	}

	public ChatRoom getChatRoom()
	{
		return chatRoom;
	}

	protected static String escapeMessageContents(String plaintext)
	{
		return HTMLUtilities.nl2br(HTMLUtilities.tagURLs(HTMLUtilities.escape(plaintext)));
	}

	public void itemAdded(Message item)
	{
		GUIUtilities.swingInvokeAndWait(new ParametrizedRunnable<Message>(item)
		{
			public void run()
			{
				JScrollBar scroll = messagesScrollPane.getVerticalScrollBar();
				int prevPos = scroll.getValue();
				boolean wasAtMax = prevPos >= scroll.getMaximum() - scroll.getVisibleAmount();
				Date now = new Date();
				messagesText.append("<p>(" +
						messagesDateFormat.format(new Date()) + ") <strong>" +
						
						HTMLUtilities.escape(parameter.getAuthor()) + ":</strong> " +
						escapeMessageContents(parameter.getContents()) + "</p>");

				messagesPane.setText(messagesText.toString());
				messagesPane.validate();

				if (wasAtMax)
					scroll.setValue(scroll.getMaximum());
				else
					scroll.setValue(prevPos);

				if (!isFocusedTab())
					setUnread(true);
			}
		});
	}

	public void itemRemoved(Message item)
	{
		throw new UnsupportedOperationException("Nie można usuwać wiadomości");
	}

	public void itemUpdated(Message item) { }

	public boolean isFocusedTab()
	{
		if (!this.chatRoomsView.isFocused())
			return false;
		return (this == this.chatRoomsView.getSelectedRoom());
	}

	public synchronized void setUnread(boolean unread)
	{
		if (this.unread == unread)
			return;
		this.unread = unread;
		chatRoomsView.setUnread(this, unread);
	}

	public boolean isUnread()
	{
		return unread;
	}

	public void focusInput()
	{
		inputPane.requestFocusInWindow();
	}
}
