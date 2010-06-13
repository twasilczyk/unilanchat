package views.swing;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import controllers.ChatController;
import protocols.*;
import resources.ResourceManager;
import tools.*;

/**
 * Widok jest w istocie kolekcją paneli rozmów, pogrupowanych w zakładkach.
 *
 * @see ChatRoomPanel
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ChatRoomsView extends JFrame
{
	protected final static String defaultTitle = "rozmowa";

	public final ChatController chatController;

	private final ChatTabs chatTabs;

	public ChatRoomsView(ChatController chatController)
	{
		super(defaultTitle);

		this.setVisible(false);

		this.chatController = chatController;
		this.chatTabs = new ChatTabs(this);

		this.setMinimumSize(new Dimension(200, 200));
		this.setPreferredSize(new Dimension(500, 450));
		this.setLayout(new BorderLayout());
		this.setIconImage(ResourceManager.getIcon("icons/32.png").getImage());

		this.addWindowFocusListener(new ChatRoomsViewFocusListener());

		this.add(chatTabs, BorderLayout.CENTER);

		this.setJMenuBar(new ChatRoomsViewMenu());

		this.pack();
		setLocationRelativeTo(null); //wyśrodkowanie okna
	}

	public void showView()
	{
		if (chatTabs.getTabCount() == 0)
			return;
		this.setVisible(true);
		this.requestFocus();
	}

	public void showRoom(ChatRoom room)
	{
		if (chatTabs.getTabCount() == 0)
			return;
		showView();
		chatTabs.goToRoom(room);
	}

	// <editor-fold defaultstate="collapsed" desc="Oznaczanie jako przeczytane / nie przeczytane">

	protected boolean isAnyUnread = false;

	public void setUnread(final ChatRoom room, boolean unread)
	{
		chatTabs.updateRoomTitle(room);

		if (this.isAnyUnread == unread)
			return;

		if (unread)
		{ // mamy pierwszy nie przeczytany pokój
			// mrugamy ikonką w trayu
			this.isAnyUnread = true;
			notifyObservers();
		}
		else
		{ // zobaczmy, czy zostały jeszcze jakieś nie przeczytane
			if (getAnyUnread() != null)
				return;

			// już nie mrugamy ikonką w trayu
			notifyObservers();
		}
	}

	protected ChatRoomPanel getAnyUnread()
	{
		synchronized (chatTabs)
		{
			for (int i = 0; i < chatTabs.getTabCount(); i++)
			{
				ChatRoomPanel chatRoomPanel =
						(ChatRoomPanel)chatTabs.getComponentAt(i);
				if (chatRoomPanel.isUnread())
				{
					this.isAnyUnread = true;
					return chatRoomPanel;
				}
			}
			this.isAnyUnread = false;
			return null;
		}
	}

	public boolean isAnyUnread()
	{
		return isAnyUnread;
	}

	public void showAnyUnread()
	{
		boolean selectedPanelWasUnread =
				((ChatRoomPanel)chatTabs.getSelectedComponent()).isUnread();
		
		GUIUtilities.bringWindowToFront(this);
		if (selectedPanelWasUnread)
			return;

		synchronized (chatTabs)
		{
			ChatRoomPanel unread = getAnyUnread();
			if (unread != null)
				chatTabs.goToRoom(unread.getChatRoom());
		}
	}

	class ChatRoomsViewFocusListener implements WindowFocusListener
	{
		public void windowGainedFocus(WindowEvent e)
		{
			ChatRoomPanel sel = chatTabs.getSelectedRoom();
			if (sel != null)
			{
				sel.setUnread(false);
				sel.focusInput();
			}
		}

		public void windowLostFocus(WindowEvent e) { }
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Obsługa obserwatorów">

	protected final SimpleObservable observableHost = new SimpleObservable();

	public void addObserver(Observer o)
	{
		observableHost.addObserver(o);
	}

	public void deleteObserver(Observer o)
	{
		observableHost.deleteObserver(o);
	}

	protected void notifyObservers()
	{
		observableHost.notifyObservers();
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Menu okna rozmów">

	class ChatRoomsViewMenu extends JMenuBar
	{
		public ChatRoomsViewMenu()
		{
			ChatRoomsViewListener listener = new ChatRoomsViewListener();

			JMenu menuConversation = new JMenu("Rozmowa");
			menuConversation.addActionListener(listener);
			this.add(menuConversation);

			JMenuItem itemAttachFile = new JMenuItem("Wyślij plik");
			itemAttachFile.setActionCommand("conversation.attachFile");
			itemAttachFile.addActionListener(listener);
			menuConversation.add(itemAttachFile);

			JMenuItem itemClose = new JMenuItem("Zamknij rozmowę");
			itemClose.setActionCommand("conversation.close");
			itemClose.addActionListener(listener);
			menuConversation.add(itemClose);
		}
	}

	class ChatRoomsViewListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			String cmd = e.getActionCommand();
			if (cmd.equals("conversation.close"))
				chatTabs.chatRoomList.remove(chatTabs.getSelectedRoom().getChatRoom());
			else if (cmd.equals("conversation.attachFile"))
			chatTabs.getSelectedRoom().showFileAttachDialog();
		}
	}

	// </editor-fold>
}
