package views.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import components.swing.JImagePanel;
import controllers.MainController;
import protocols.*;
import resources.ResourceManager;
import views.ContactListModel;

/**
 * Komponent wyświetlający listę kontaktów
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ContactListPanel extends JScrollPane
{
	private final ContactListPanelList contactListPanelList;
	protected final ContactList contactList;
	protected final MainController mainController;
	protected final MainView mainView;

	public ContactListPanel(MainView mainView)
	{
		if (mainView == null)
			throw new NullPointerException();
		this.mainView = mainView;
		this.mainController = mainView.getMainController();
		this.contactList = mainController.getContactList();
		this.contactListPanelList = new ContactListPanelList();

		this.setViewportView(contactListPanelList);
		this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.setBorder(BorderFactory.createEmptyBorder());
	}

	class ContactListPanelList extends JList
	{
		public ContactListPanelList()
		{
			this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.setModel(new ContactListModel(contactList));
			this.setCellRenderer(new ContactListPanelRenderer());
			this.addMouseListener(new ContactListListener());
		}

		protected Contact locationToItem(Point location)
		{
			if (location == null)
				throw new NullPointerException();
			
			int index = locationToIndex(location);
			if (index < 0)
				return null;
			
			Rectangle itemRect = getCellBounds(index, index);
			if (!itemRect.contains(location))
				return null;

			ContactListModel model = (ContactListModel)getModel();
			synchronized (this)
			{
				if (model.getSize() < index)
					return null;
				else
					return model.getElementAt(index);
			}
		}

		@Override public String getToolTipText(MouseEvent event)
		{
			if (event == null)
				throw new NullPointerException();
			Contact item = locationToItem(event.getPoint());
			if (item == null)
				return super.getToolTipText(event);

			String tooltip = "ID: " + item.getID();

			if (item.getAccount().isGroupsSupported() && !item.getGroup().isEmpty())
				tooltip += ", grupa: " + item.getGroup();

			String status = item.getTextStatus();
			if (status != null)
				tooltip += ", opis: \"" + status + "\"";

            return tooltip;
		}
	}

	class ContactListListener implements MouseListener
	{
		public void mouseClicked(MouseEvent e)
		{
			if (e.getButton() != MouseEvent.BUTTON1)
				return;
			if (e.getClickCount() != 2)
				return;
			Contact contact = contactListPanelList.locationToItem(e.getPoint());
			if (contact == null)
				return;
			ChatRoom privRoom = mainController.getChatController().getPrivateChatRoom(contact);
			if (privRoom != null)
				mainView.getChatRoomsView().showRoom(privRoom);
		}

		public void mousePressed(MouseEvent e) { }
		public void mouseReleased(MouseEvent e) { }
		public void mouseEntered(MouseEvent e) { }
		public void mouseExited(MouseEvent e) { }
	}
}

class ContactListPanelRenderer implements ListCellRenderer
{
	protected final JPanel render = new JPanel();

	protected final JPanel userNamePanel = new JPanel();
	protected final JLabel userName = new JLabel();
	protected final JLabel userTextStatus = new JLabel();
	protected final JImagePanel userIcon = new JImagePanel();
	protected final JImagePanel protocolIcon = new JImagePanel();

	protected final Color selectedUserNameColor = new Color(100, 150, 200);

	protected final static Image iconOnline = ResourceManager.getImage("status/online.png");
	protected final static Image iconOffline = ResourceManager.getImage("status/offline.png");
	protected final static Image iconBusy = ResourceManager.getImage("status/busy.png");

	protected final static Image iconIpmsg = ResourceManager.getImage("protocols/ipmsg.png");

	public ContactListPanelRenderer()
	{
		render.setLayout(new BorderLayout(5, 0));
		render.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		render.add(userIcon, BorderLayout.WEST);
		render.add(userNamePanel, BorderLayout.CENTER);
		render.add(protocolIcon, BorderLayout.EAST);

		int prefferedHeight = Math.round((float)1.5 * userName.getFont().getSize());
		if (prefferedHeight < 15)
			prefferedHeight = 15;
		render.setPreferredSize(new Dimension(50, prefferedHeight));

		userTextStatus.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		userTextStatus.setMinimumSize(new Dimension(0, 0));

		userNamePanel.setOpaque(false);
		userNamePanel.setLayout(new BorderLayout(5, 0));
		userNamePanel.setPreferredSize(new Dimension(70, 10));

		userNamePanel.add(userName, BorderLayout.WEST);
		userNamePanel.add(userTextStatus, BorderLayout.CENTER);

		int statusTextSize = userTextStatus.getFont().getSize(); //domyślnie 13 w KDE, w Windowsie mniej
		if (statusTextSize >= 13)
			statusTextSize -= 4;
		else if (statusTextSize >= 10)
			statusTextSize = 9;
		userTextStatus.setFont(new Font(userTextStatus.getFont().getName(),
			userTextStatus.getFont().getStyle(),
			statusTextSize));

		userIcon.setPreferredSize(new Dimension(12, 12));
		protocolIcon.setPreferredSize(new Dimension(12, 12));
		
	}

	public synchronized Component getListCellRendererComponent(JList list,
			Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		if (value == null) //patrz ContactListModel.getElementAt()
			return new JLabel();
		
		assert(value instanceof Contact);
		Contact contact = (Contact)value;

		if (isSelected)
		{
			render.setBackground(selectedUserNameColor);
			userName.setForeground(Color.WHITE);
			userTextStatus.setForeground(Color.LIGHT_GRAY);
		}
		else
		{
			render.setBackground(Color.WHITE);
			userName.setForeground(Color.BLACK);
			userTextStatus.setForeground(Color.GRAY);
		}

		userName.setText(contact.getName());

		String textStatus = contact.getTextStatus();
		if (textStatus == null)
			userTextStatus.setVisible(false);
		else
		{
			userTextStatus.setVisible(true);
			userTextStatus.setText(textStatus);
		}

		switch (contact.getStatus())
		{
			case ONLINE:
				userIcon.image = iconOnline;
				break;
			case OFFLINE:
				userIcon.image = iconOffline;
				break;
			case BUSY:
				userIcon.image = iconBusy;
				break;
		}

		if (contact instanceof protocols.ipmsg.IpmsgContact)
			protocolIcon.image = iconIpmsg;
		else
			assert(false);

		return render;
	}
}
