package views.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import protocols.*;
import tools.SetListener;

/**
 * Panel pokoju rozmów, zawierający listę wiadomości oraz pole do ich wysyłania.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ChatRoomPanel extends JPanel implements SetListener<Message>
{
	protected final ChatRoom chatRoom;
	protected final ChatRoomsView chatRoomsView;

	protected final MessagesPanel messagesPanel = new MessagesPanel();
	
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
		this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 0));

		this.add(messagesPanel, BorderLayout.CENTER);
		this.add(inputScrollPane, BorderLayout.SOUTH);

		inputPane.setBackground(Color.WHITE);
		inputPane.addKeyListener(new InputPaneListener());
		inputScrollPane.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		inputScrollPane.setPreferredSize(new Dimension(50, minInputHeight));
		inputScrollPane.setBorder(BorderFactory.createEmptyBorder());

		messagesPanel.addKeyListener(new MessagesPanelListener());
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

	class MessagesPanelListener implements KeyListener
	{

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

	public ChatRoom getChatRoom()
	{
		return chatRoom;
	}

	public void itemAdded(Message item)
	{
		messagesPanel.add(item);
		if (!isFocusedTab())
			setUnread(true);
	}

	public void itemRemoved(Message item)
	{
		throw new UnsupportedOperationException("Nie można usuwać wiadomości");
	}

	public void itemUpdated(Message item)
	{
		messagesPanel.update(item);
	}

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
		chatRoomsView.setUnread(this.chatRoom, unread);
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
