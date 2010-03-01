package views.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import controllers.MainController;
import resources.ResourceManager;
import tools.GUIUtilities;

/**
 * Widok odpowiadający za główne okno programu.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class MainView extends JFrame
{
	protected final MainController mainController;
	protected final MainView mainView = this;

	protected final ChatRoomsView chatRoomsView;

	protected TrayIcon trayIcon = null;
	protected boolean trayModeShowUnread = false;

	private final MainViewListener mainViewListener =
		new MainViewListener();

	/**
	 * Główny konstruktor. Aby utworzyć nową instancję widoku, należy skorzystać
	 * z metody {@link #init(MainController)}.
	 *
	 * @see #init(MainController)
	 * @param mainController główny kontroler, z którym powiązany jest widok
	 */
	protected MainView(MainController mainController)
	{
		super("Lista kontaktów");

		this.mainController = mainController;
		chatRoomsView = new ChatRoomsView(mainController.getChatController());
		chatRoomsView.addObserver(new ChatRoomsViewObserver());

		this.setMinimumSize(new Dimension(100, 200));
		this.setPreferredSize(new Dimension(200, 450));
		this.setLayout(new BorderLayout(0, 3));
		this.setIconImage(ResourceManager.getIcon("icon.png").getImage());

		this.setJMenuBar(new MainMenu(this));

		JButton mainRoomButton = new JButton("Pokój główny");
		mainRoomButton.setActionCommand("openMainRoom");
		mainRoomButton.addActionListener(mainViewListener);

		this.add(mainRoomButton, BorderLayout.NORTH);
		this.add(new ContactListPanel(this), BorderLayout.CENTER);
		this.add(new StatusPanel(mainController), BorderLayout.SOUTH);

		this.pack();
		GUIUtilities.centerWindow(this);

		new TrayInitThread();
	}

	class MainViewListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			String cmd = e.getActionCommand();
			if (cmd.equals("openMainRoom"))
				getChatRoomsView().showRoom(
					mainView.getMainController().getChatController().getMainChatRoom());
			else
				throw new RuntimeException("Nieznane polecenie: " + cmd);
		}
	}

	/**
	 * Tworzenie nowej instancji głównego widoku musi odbywać się przez tą
	 * metodę. W przypadku niepowodzenia (np. po próbie uruchomienia aplikacji
	 * Swing w środowisku bez okien) zwraca fałsz.
	 *
	 * @param mainController obiekt głównego kontrolera
	 * @return <code>true</code>, jeżeli zakończono powodzeniem
	 */
	public static boolean init(MainController mainController)
	{
		MainViewInitializator init = new MainViewInitializator(mainController);
		if (!GUIUtilities.swingInvokeAndWait(init))
			return false;
		return init.wasSuccessful();
	}

	public ChatRoomsView getChatRoomsView()
	{
		return chatRoomsView;
	}

	public MainController getMainController()
	{
		return mainController;
	}

	/**
	 * Klasa odpowiada za obserwowanie, czy nie pojawiły się (lub zniknęły)
	 * nie przeczytane wiadomości - jeżeli tak, zmieniamy tryb ikonki w trayu.
	 */
	class ChatRoomsViewObserver implements Observer
	{
		public void update(Observable o, Object arg)
		{
			if (trayIcon == null)
				return;

			trayModeShowUnread = chatRoomsView.isAnyUnread();

			if (trayModeShowUnread)
				trayIcon.setImage(ResourceManager.getImage("iconUnread.png"));
			else
				trayIcon.setImage(ResourceManager.getImage("icon.png"));
		}
	}

	// <editor-fold defaultstate="collapsed" desc="Obsługa tacki systemowej">

	class TrayMenuListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			String cmd = event.getActionCommand();
			if (cmd.equals("application.close"))
				mainController.applicationClose();
			else
				throw new IllegalArgumentException("Nieprawidłowe polecenie");
		}
	}

	class TrayMouseListener implements MouseListener
	{
		public void mouseEntered(MouseEvent e) { }
		public void mouseExited(MouseEvent e) { }
		public void mousePressed(MouseEvent e) { }
		public void mouseReleased(MouseEvent e) { }

		public void mouseClicked(MouseEvent e)
		{
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				if (trayModeShowUnread)
					chatRoomsView.showAnyUnread();
				else if (mainView.isVisible())
				{
					if (mainView.getState() == Frame.ICONIFIED)
					{
						mainView.setState(Frame.NORMAL);
						mainView.setVisible(true);
						mainView.toFront();
						mainView.requestFocus();
					}
/*					else if (!mainView.isActive()) //TODO: występują problemy na windowsie
					{
						mainView.setVisible(false);
						mainView.setVisible(true);
						mainView.toFront();
						mainView.requestFocus();
					}
*/					else
						mainView.setVisible(false);
				}
				else
				{
					mainView.setVisible(true);
					mainView.toFront();
					mainView.requestFocus();
				}
			}
		}
	}

	class TrayInitThread extends Thread
	{
		public TrayInitThread()
		{
			super("ULC-TrayInitThread");
			this.setDaemon(true);
			this.start();
		}

		@Override public void run()
		{
			if (!SystemTray.isSupported())
				return;

			TrayMenuListener trayMenuListener = new TrayMenuListener();

			SystemTray tray = SystemTray.getSystemTray();
			PopupMenu trayMenu = new PopupMenu();
			
			trayIcon = new TrayIcon(ResourceManager.getImage("icon.png"),
					"UniLANChat", trayMenu);
			trayIcon.setImageAutoSize(true);
			trayIcon.addMouseListener(new TrayMouseListener());

			MenuItem itemZamknij = new MenuItem("Zamknij");
			itemZamknij.addActionListener(trayMenuListener);
			itemZamknij.setActionCommand("application.close");
			trayMenu.add(itemZamknij);

			try
			{
				tray.add(trayIcon);
			}
			catch (AWTException e)
			{
				trayIcon = null;
				return;
			}
		}
	}

	// </editor-fold>
}

/**
 * Pomocnicza klasa dla wątku odpowiedzialnego za utworzenie nowej instancji
 * głównego widoku.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
class MainViewInitializator implements Runnable
{
	protected boolean success = false;
	protected final MainController mainController;

	public MainViewInitializator(MainController mainController)
	{
		this.mainController = mainController;
	}

	public boolean wasSuccessful()
	{
		return success;
	}

	public void run()
	{
		try
		{
			new MainView(mainController);
			success = true;
		}
		catch (HeadlessException e)
		{
			success = false;
		}
	}
}
