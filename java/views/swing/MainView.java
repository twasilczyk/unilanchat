package views.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import controllers.MainController;
import main.Main;
import resources.ResourceManager;
import tools.*;
import tools.systemintegration.SystemProcesses;

/**
 * Widok odpowiadający za główne okno programu.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class MainView extends JFrame implements Observer
{
	protected final MainController mainController;
	protected final MainView mainView = this;

	private ChatRoomsView chatRoomsView;

	protected TrayIcon trayIcon = null;
	protected boolean trayModeShowUnread = false;

	private final MainViewListener mainViewListener =
		new MainViewListener();

	/**
	 * Kiedy ostatnio (unix timestamp) okno <strong>utraciło</strong> aktywność.
	 */
	private long wasActive = 0;

	/**
	 * Główny konstruktor. Aby utworzyć nową instancję widoku, należy skorzystać
	 * z metody {@link #init(MainController)}.
	 *
	 * @see #init(MainController)
	 * @param mainControllerObj główny kontroler, z którym powiązany jest widok
	 */
	protected MainView(MainController mainControllerObj)
	{
		super("Lista kontaktów");

		this.mainController = mainControllerObj;
		this.mainController.addObserver(this);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// utworzenie tego okna zajmuje dużo czasu, a nie potrzebujemy
				// go od razu po starcie aplikacji
				chatRoomsView = new ChatRoomsView(mainController.getChatController());
				chatRoomsView.addObserver(new ChatRoomsViewObserver());
			}
		});

		setMinimumSize(new Dimension(100, 200));
		setPreferredSize(new Dimension(250, 450));
		setLayout(new BorderLayout());
		setIconImage(ResourceManager.getIcon("icon.png").getImage());

		addWindowListener(mainViewListener);

		setJMenuBar(new MainMenu(this));

		add(new MainRoomButtonPanel(), BorderLayout.NORTH);
		add(new ContactListPanel(this), BorderLayout.CENTER);
		add(new StatusPanel(mainController), BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(null); //wyśrodkowanie okna
	}

	public void update(Observable o, Object gArg)
	{
		if (o == mainController && gArg instanceof Pair)
		{
			Pair arg = (Pair)gArg;
			if (arg.left.equals("newVersionAvailable"))
			{
				assert(arg.right instanceof Updater);
				final Updater updater = (Updater)arg.right;

				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						String newestVersion =
							(Main.isNightly ?
								updater.getNightlyVersion() + " (nightly)" :
								updater.getCurrentVersion());

						String[] options = new String[]
						{
							"aktualizuj",
							"później"
						};

						int selection = JOptionPane.showOptionDialog(mainView,
							"Nowa wersja programu jest już dostępna: " + newestVersion,
							"Nowa wersja programu",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.INFORMATION_MESSAGE, null,
							options, options[0]);
						
						if (selection == 0)
							SystemProcesses.openURL(updater.getHomepage());
					}
				});
			}
		}
	}

	class MainRoomButtonPanel extends JPanel
	{
		public MainRoomButtonPanel()
		{
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

			JButton mainRoomButton = new JButton("Pokój główny");
			mainRoomButton.setActionCommand("openMainRoom");
			mainRoomButton.addActionListener(mainViewListener);
			add(mainRoomButton, BorderLayout.CENTER);
		}
	}

	class MainViewListener implements ActionListener, WindowListener
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

		public void windowOpened(WindowEvent e) { }
		public void windowClosing(WindowEvent e) { }
		public void windowClosed(WindowEvent e) { }
		public void windowIconified(WindowEvent e) { }
		public void windowDeiconified(WindowEvent e) { }
		public void windowActivated(WindowEvent e) { }

		public void windowDeactivated(WindowEvent e)
		{
			wasActive = (new Date()).getTime();
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
		TrayInitThread trayInitThread = new TrayInitThread();

		MainViewInitializator init = new MainViewInitializator(mainController);
		if (!GUIUtilities.swingInvokeAndWait(init) || !init.wasSuccessful())
			return false;

		try
		{
			trayInitThread.join();
		}
		catch (InterruptedException ex) { }
		if (trayInitThread.trayIcon == null)
			return false;
		init.getMainView().trayIcon = trayInitThread.trayIcon;
		init.getMainView().finishTrayInitialization();

		return true;
	}

	public ChatRoomsView getChatRoomsView()
	{
		if (chatRoomsView == null)
		{
			long waitstart = System.currentTimeMillis();
			while (chatRoomsView == null &&
				System.currentTimeMillis() - waitstart < 10000) // czekamy max 10s
				Thread.yield();
			if (chatRoomsView == null)
				throw new NullPointerException("Nie zainicjowano chatRoomsView");
		}
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

			trayModeShowUnread = getChatRoomsView().isAnyUnread();

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
		public void mouseReleased(MouseEvent e) { }
		public void mouseClicked(MouseEvent e) {}

		public void mousePressed(MouseEvent e)
		{
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				if (trayModeShowUnread)
					getChatRoomsView().showAnyUnread();
				else if
					(
					//okno jest widoczne
					mainView.isVisible() &&

					//nie jest zminimalizowane
					mainView.getState() == Frame.NORMAL &&

					//oraz jest, lub było przed chwilą aktywne
					(mainView.isActive() || wasActive + 100 > (new Date()).getTime())
					)
					mainView.setVisible(false);
				else
					GUIUtilities.bringWindowToFront(mainView);
			}
		}
	}

	static class TrayInitThread extends Thread
	{
		private TrayIcon trayIcon;

		public TrayInitThread()
		{
			super("ULC-TrayInitThread");
			this.setDaemon(true);
			this.start();
		}

		@Override public void run()
		{
			//TODO: jakoś trzeba zareagować - jest to podstawowa funkcja tego
			//widoku
			if (!SystemTray.isSupported())
				return;

			trayIcon = new TrayIcon(ResourceManager.getImage("icon.png"),
					"UniLANChat");
			trayIcon.setImageAutoSize(true);

			try
			{
				SystemTray.getSystemTray().add(trayIcon); // ~650 - 700ms
			}
			catch (AWTException e)
			{
				trayIcon = null;
				return;
			}
		}
	}

	/**
	 * Związanie ikony w trayu z widokiem (po zainicjowaniu widoku).
	 *
	 * Ten kod został wydzielony z TrayInitThread, aby można było rozpocząć
	 * inicjalizację tej ikony jeszcze przed rozpoczęciem tworzenia widoku.
	 */
	private void finishTrayInitialization()
	{
		if (trayIcon == null)
			throw new NullPointerException();

		PopupMenu trayMenu = new PopupMenu();

		TrayMenuListener trayMenuListener = new TrayMenuListener();
		trayIcon.addMouseListener(new TrayMouseListener());

		MenuItem itemZamknij = new MenuItem("Zamknij");
		itemZamknij.addActionListener(trayMenuListener);
		itemZamknij.setActionCommand("application.close");
		trayMenu.add(itemZamknij);

		trayIcon.setPopupMenu(trayMenu);
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
	protected MainView mainView;
	protected final MainController mainController;

	public MainViewInitializator(MainController mainController)
	{
		this.mainController = mainController;
	}

	public boolean wasSuccessful()
	{
		return success;
	}

	public MainView getMainView()
	{
		return mainView;
	}

	public void run()
	{
		try
		{
			mainView = new MainView(mainController);
			success = true;
		}
		catch (HeadlessException e)
		{
			success = false;
		}
	}
}
