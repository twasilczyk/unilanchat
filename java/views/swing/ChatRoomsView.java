package views.swing;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

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
		this.setPreferredSize(new Dimension(500, 500));
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
		chatTabs.repaint(); //TODO: swing thread safe?

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

class ChatTabs extends JTabbedPane implements MouseListener, SetListener<ChatRoom>, ChangeListener
{
	protected final ChatRoomsView chatRoomsView;
	protected final ChatRoomList chatRoomList;

	protected final static Icon statusOnline = ResourceManager.getIcon("status/online.png");
	protected final static Icon statusBusy = ResourceManager.getIcon("status/busy.png");
	protected final static Icon statusOffline = ResourceManager.getIcon("status/offline.png");

	public ChatTabs(ChatRoomsView chatRoomsView)
	{
		this.chatRoomsView = chatRoomsView;

		this.setUI(new ChatTabsUI(this));

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

	public void itemAdded(ChatRoom item)
	{
		GUIUtilities.swingInvokeAndWait(new ParametrizedRunnable<ChatRoom>(item)
		{
			public void run()
			{
				addTab("rozmowa",
					null,
					new ChatRoomPanel(parameter, chatRoomsView));
				updateRoomTitle(parameter);
			}
		});
	}

	public void itemRemoved(ChatRoom item)
	{
		GUIUtilities.swingInvokeAndWait(new ParametrizedRunnable<ChatRoom>(item)
		{
			public void run()
			{
				ChatRoomPanel panel = getRoomPanel(parameter);
				remove(panel);
				synchronized (this)
				{
					if (getTabCount() == 0)
						chatRoomsView.setVisible(false);
				}
			}
		});
	}

	public synchronized void itemUpdated(ChatRoom item)
	{
		SwingUtilities.invokeLater(new ParametrizedRunnable<ChatRoom>(item)
		{
			public void run()
			{
				updateRoomTitle(parameter);
			}
		});
	}

	// zmiana taba
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
	 * Aktualizuje tytuł i ikonkę okna. Może być wywoływany TYLKO w wątku AWT.
	 *
	 * @param room Pokój do aktualizacji
	 */
	protected void updateRoomTitle(ChatRoom room)
	{
		int tabno = getRoomIndex(room);
		if (tabno >= 0)
		{
			String title = room.getTitle();
			if (title.isEmpty())
				title = "rozmowa";
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
	}
}

class ChatTabsUI extends javax.swing.plaf.basic.BasicTabbedPaneUI
{
	protected final ChatTabs chatTabs;
	protected final Font boldFont;
	protected FontMetrics boldFontMetrics;

	public ChatTabsUI(ChatTabs chatTabs)
	{
		this.chatTabs = chatTabs;
		boldFont = new Font(chatTabs.getFont().getFontName(), Font.BOLD,
				chatTabs.getFont().getSize());
	}

	@Override public void installUI(JComponent c)
	{
		super.installUI(c);
		boldFontMetrics = c.getFontMetrics(this.boldFont);
	}

	protected boolean isUnread(int tabIndex)
	{
		Component c = chatTabs.getComponentAt(tabIndex);
		if (!(c instanceof ChatRoomPanel))
			return false;
		return ((ChatRoomPanel)c).isUnread();
	}

	@Override protected void paintText(Graphics g, int tabPlacement, Font font,
			FontMetrics metrics, int tabIndex, String title, Rectangle textRect,
			boolean isSelected)
	{
		if (isUnread(tabIndex))
			super.paintText(g, tabPlacement, boldFont, boldFontMetrics, tabIndex, title, textRect, isSelected);
		else
			super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
	}

	@Override protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics)
	{
		if (isUnread(tabIndex))
			return super.calculateTabWidth(tabPlacement, tabIndex, boldFontMetrics) + 5;
		else
			return super.calculateTabWidth(tabPlacement, tabIndex, metrics);
	}

}
