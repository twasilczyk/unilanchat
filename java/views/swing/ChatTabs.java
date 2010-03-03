package views.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import protocols.*;
import resources.ResourceManager;
import tools.*;

/**
 * Zakładki z otwartymi pokojami rozmów.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ChatTabs extends JTabbedPane implements MouseListener, SetListener<ChatRoom>, ChangeListener
{
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

	public synchronized boolean goToRoom(ChatRoom room)
	{
		int panelIndex = getRoomIndex(room);
		if (panelIndex < 0)
			return false;
		goToRoom(panelIndex);
		return true;
	}

	public void goToRoom(int index)
	{
		setSelectedIndex(index);
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
			if (!(getComponentAt(i) instanceof ChatRoomPanel))
				continue;
			ChatRoomPanel panel = (ChatRoomPanel)getComponentAt(i);
			if (panel.getChatRoom() == item)
				return panel;
		}
		return null;
	}

	protected synchronized int getRoomIndex(ChatRoom item)
	{
		for (int i = 0; i < this.getTabCount(); i++)
		{
			if (!(getComponentAt(i) instanceof ChatRoomPanel))
				continue;
			ChatRoomPanel panel = (ChatRoomPanel)getComponentAt(i);
			if (panel.getChatRoom() == item)
				return i;
		}
		return -1;
	}

	public void mousePressed(MouseEvent e) { } //może przenoszenie tabów?
	public void mouseReleased(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }

	public void itemAdded(final ChatRoom item)
	{
		GUIUtilities.swingInvokeAndWait(new Runnable()
		{
			public void run()
			{
				addTab("rozmowa",
					null,
					new ChatRoomPanel(item, chatRoomsView));
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
				remove(panel);
				synchronized (this)
				{
					if (getTabCount() == 0)
						chatRoomsView.setVisible(false);
				}
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
			chatRoomsView.setTitle(getTitleAt(getSelectedIndex()));
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
		final int tabno = getRoomIndex(room);
		if (tabno < 0)
			return;
		GUIUtilities.swingInvokeAndWait(new Runnable()
		{
			public void run()
			{
				String title = room.getTitle();
				if (title.isEmpty())
					title = "rozmowa";

				if (getRoomPanel(room).isUnread())
					setTitleAt(tabno, "<html><b>" + title + "</b></html>");
				else
					setTitleAt(tabno, title);
				updateWindowTitle();
				if (room instanceof PrivateChatRoom)
				{
					PrivateChatRoom privRoom = (PrivateChatRoom)room;
					switch (privRoom.getContact().getStatus())
					{
						case ONLINE:
							setIconAt(tabno, statusOnline);
							break;
						case BUSY:
							setIconAt(tabno, statusBusy);
							break;
						case OFFLINE:
							setIconAt(tabno, statusOffline);
							break;
					}
				}
			}
		});
	}
}
