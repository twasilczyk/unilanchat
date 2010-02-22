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
 * Widok jest w istocie kolekcją paneli rozmów, pogrupowanych w zakładkach
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
		this.setIconImage(ResourceManager.getIcon("icon.png").getImage());

		this.addWindowFocusListener(new ChatRoomsViewFocusListener());

		this.add(chatTabs, BorderLayout.CENTER);

		this.pack();
		GUIUtilities.centerWindow(this);
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

	public ChatRoomPanel getSelectedRoom()
	{
		Component sel = chatTabs.getSelectedComponent();
		if (sel == null || !(sel instanceof ChatRoomPanel))
			return null;
		return (ChatRoomPanel)sel;
	}

	// <editor-fold defaultstate="collapsed" desc="Oznaczanie jako przeczytane / nie przeczytane">

	protected boolean isAnyUnread = false;

	public void setUnread(ChatRoomPanel panel, boolean unread)
	{
		GUIUtilities.swingInvokeAndWait(new Runnable()
		{
			public void run()
			{
				chatTabs.repaint();
			}
		});

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
			if (getAnyUnreadIndex() >= 0)
				return;

			// już nie mrugamy ikonką w trayu
			notifyObservers();
		}
	}

	protected int getAnyUnreadIndex()
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
					return i;
				}
			}
			this.isAnyUnread = false;
			return -1;
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
		
		if (isVisible())
		{
			if (getState() == Frame.ICONIFIED)
			{
				setState(Frame.NORMAL);
				setVisible(true);
			}
			else if (!isActive())
			{
				setVisible(false);
				setVisible(true);
			}
		}
		else
			setVisible(true);
		toFront();
		requestFocus();

		if (selectedPanelWasUnread)
			return;

		synchronized (chatTabs)
		{
			int unreadIndex = getAnyUnreadIndex();
			if (unreadIndex >= 0)
				chatTabs.goToRoom(unreadIndex);
		}
	}

	class ChatRoomsViewFocusListener implements WindowFocusListener
	{
		public void windowGainedFocus(WindowEvent e)
		{
			ChatRoomPanel sel = getSelectedRoom();
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

}
