package views.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import protocols.*;
import resources.ResourceManager;
import tools.*;
import tools.html.HTMLUtilities;

/**
 * Zakładki z otwartymi pokojami rozmów.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ChatTabs extends JTabbedPane implements MouseListener, SetListener<ChatRoom>, ChangeListener
{
	protected final ChatTabs thisChatTabs = this;

	protected final ChatRoomsView chatRoomsView;
	protected final ChatRoomList chatRoomList;

	protected final static Icon statusOnline = ResourceManager.getIcon("status/online.png");
	protected final static Icon statusBusy = ResourceManager.getIcon("status/busy.png");
	protected final static Icon statusOffline = ResourceManager.getIcon("status/offline.png");

	public ChatTabs(ChatRoomsView chatRoomsView)
	{
		this.chatRoomsView = chatRoomsView;

		this.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);

		this.addMouseListener(this);
		chatRoomList = this.chatRoomsView.chatController.getChatRoomList();
		chatRoomList.addSetListener(this);

		addChangeListener(this);
	}

	public synchronized void goToRoom(ChatRoom room)
	{
		ChatRoomPanel panel = getRoomPanel(room);
		assert (panel != null);
		setSelectedComponent(panel);
		updateWindowTitle();
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON2)
		{
			ChatRoom remCR;
			synchronized (this)
			{
				int tabno = getUI().tabForCoordinate(this, e.getX(), e.getY());
				if (tabno < 0)
					return;

				Component c = getComponentAt(tabno);

				if (!(c instanceof ChatRoomPanel))
					return;
				remCR = ((ChatRoomPanel)c).getChatRoom();
			}

			chatRoomList.remove(remCR);
		}
	}

	protected synchronized ChatRoomPanel getRoomPanel(ChatRoom item)
	{
		for (int i = 0; i < this.getTabCount(); i++)
		{
			ChatRoomPanel panel = getRoomPanelAt(i);
			if (panel == null)
				continue;
			if (panel.getChatRoom() == item)
				return panel;
		}
		return null;
	}

	//TODO: do wyrzucenia po poprawieniu metod getRoomPanel
	public ChatRoomPanel getRoomPanelAt(int index)
	{
		Object tab = getComponentAt(index);

		if (!(tab instanceof ChatRoomPanel))
			return null;

		return (ChatRoomPanel)tab;
	}

	public ChatRoomPanel getSelectedRoom()
	{
		Component sel = getSelectedComponent();
		if (sel == null || !(sel instanceof ChatRoomPanel))
			return null;
		return (ChatRoomPanel)sel;
	}

	protected synchronized void setTitleAt(ChatRoomPanel panel, String title)
	{
		super.setTitleAt(indexOfComponent(panel), title);
	}

	protected synchronized void setIconAt(ChatRoomPanel panel, Icon icon)
	{
		super.setIconAt(indexOfComponent(panel), icon);
	}

	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }

	public void itemAdded(final ChatRoom item)
	{
		GUIUtilities.swingInvokeAndWait(new Runnable()
		{
			public void run()
			{
				synchronized (thisChatTabs)
				{
					addTab("rozmowa",
						null,
						new ChatRoomPanel(item, chatRoomsView));
				}
				updateRoomTitle(item);
			}
		});
	}

	public void itemRemoved(final ChatRoom item)
	{
		GUIUtilities.swingInvokeAndWait(new Runnable()
		{
			public void run()
			{
				ChatRoomPanel panel = getRoomPanel(item);
				boolean wasUnread = panel.isUnread();
				synchronized (thisChatTabs)
				{
					remove(panel);
					if (getTabCount() == 0)
						chatRoomsView.setVisible(false);
				}
				if (wasUnread)
					chatRoomsView.getAnyUnread();
			}
		});
	}

	public synchronized void itemUpdated(final ChatRoom item)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				updateRoomTitle(item);
			}
		});
	}

	/**
	 * Obsługa zmiany wybranej zakładki.
	 *
	 * @param e zdarzenie opisujące zmianę zakładki
	 */
	public void stateChanged(ChangeEvent e)
	{
		updateWindowTitle();
		Component sel = getSelectedComponent();
		if (sel == null || !(sel instanceof ChatRoomPanel))
			return;
		ChatRoomPanel panel = (ChatRoomPanel)sel;
		panel.setUnread(false);
		panel.focusInput();
	}

	@Override protected void processMouseEvent(MouseEvent e)
	{
		// zakładki powinny się zmieniać tylko poprzez kliknięcie na nie lewym
		// przyciskiem myszy
		if (e.getID() == MouseEvent.MOUSE_PRESSED &&
			e.getButton() != MouseEvent.BUTTON1)
			return;

		super.processMouseEvent(e);
	}

	protected synchronized void updateWindowTitle()
	{
		try
		{
			ChatRoomPanel roomPanel = getSelectedRoom();
			if (roomPanel == null)
				return;
			chatRoomsView.setTitle(roomPanel.getChatRoom().getTitle());
		}
		catch (IndexOutOfBoundsException ex)
		{
			chatRoomsView.setTitle("rozmowa");
		}
	}

	/**
	 * Aktualizuje tytuł i ikonkę okna.
	 *
	 * @param room pokój do aktualizacji
	 */
	protected void updateRoomTitle(final ChatRoom room)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				ChatRoomPanel panel = getRoomPanel(room);

				String title = room.getTitle();
				if (title.isEmpty())
					title = "rozmowa";
				if (getRoomPanel(room).isUnread())
					setTitleAt(panel, "<html><b>" + HTMLUtilities.escape(title) + "</b></html>");
				else
					setTitleAt(panel, HTMLUtilities.escapeForSwing(title));
				updateWindowTitle();
				
				if (room instanceof PrivateChatRoom)
				{
					PrivateChatRoom privRoom = (PrivateChatRoom)room;
					switch (privRoom.getContact().getStatus())
					{
						case ONLINE:
							setIconAt(panel, statusOnline);
							break;
						case BUSY:
							setIconAt(panel, statusBusy);
							break;
						case OFFLINE:
							setIconAt(panel, statusOffline);
							break;
					}
				}
			}
		});
	}
}
