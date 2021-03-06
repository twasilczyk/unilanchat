package views.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Level;
import javax.swing.*;

import controllers.MainController;
import main.*;
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
	protected final MainMenu mainMenu;

	protected TrayIcon trayIcon = null;
	protected boolean trayModeShowUnread = false;
	protected static Image trayIconImageReady = null;
	protected static Image trayIconImageUnread = null;

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
		super("Lista kontaktów (" + Configuration.getInstance().getNick() + ")");

		this.mainController = mainControllerObj;
		this.mainController.addObserver(this);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// utworzenie tego okna zajmuje dużo czasu, a nie potrzebujemy
				// go od razu po starcie aplikacji
				chatRoomsView = new ChatRoomsView(mainController.getChatController(), mainView);
				chatRoomsView.addObserver(new ChatRoomsViewObserver());
			}
		});

		setMinimumSize(new Dimension(100, 200));
		setPreferredSize(new Dimension(250, 450));
		setLayout(new BorderLayout());
		setIconImage(ResourceManager.getIcon("icons/32.png").getImage());

		addWindowListener(mainViewListener);

		mainMenu = new MainMenu(this);
		setJMenuBar(mainMenu);

		add(new MainRoomButtonPanel(), BorderLayout.NORTH);
		add(new ContactListPanel(this), BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.PAGE_AXIS));
		bottomPanel.add(new WarningNotificationPanel(Main.userNotifications));
		bottomPanel.add(new StatusPanel(mainController));
		add(bottomPanel, BorderLayout.SOUTH);

		pack();

		Configuration.WindowDimensions frameDim =
			Configuration.getInstance().getMainViewDimensions();
		if (frameDim == null)
			setLocationRelativeTo(null); //wyśrodkowanie okna
		else
		{
			frameDim = frameDim.shrinkToVisible();
			setSize(frameDim.width, frameDim.height);
			setLocation(frameDim.left, frameDim.top);
		}

		Configuration.getInstance().addObserver(this);
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
		else if (o == mainController && gArg instanceof String)
		{
			String arg = (String)gArg;
			if (arg.equals("applicationClose"))
			{
				Configuration.getInstance().setMainViewDimensions(
					new Configuration.WindowDimensions(
						getLocation().x, getLocation().y,
						getWidth(), getHeight()));
			}
		}
		else if (o instanceof Configuration)
		{
			final Configuration conf = (Configuration)o;
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					setTitle("Lista kontaktów (" + conf.getNick() + ")");
				}
			});
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
					mainView.getMainController().getChatController().
					getChatRoomList().getMain());
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
		boolean temp = GUIUtilities.swingInvokeAndWait(init);
		if (!temp || !init.wasSuccessful())
		{
			System.err.println("Nie załadowano głównego widoku: (" + temp + ";" + init.wasSuccessful() + ")");
			return false;
		}

		try
		{
			trayInitThread.join();
		}
		catch (InterruptedException ex) { }
		if (trayInitThread.trayIcon == null)
		{
			Main.logger.log(Level.WARNING, "Nie załadowano ikony w trayu");
			return false;
		}
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
				trayIcon.setImage(trayIconImageUnread);
			else
				trayIcon.setImage(trayIconImageReady);
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
			SystemTray tray;

			//TODO: jakoś trzeba zareagować - jest to podstawowa funkcja tego
			//widoku
			if (!SystemTray.isSupported())
			{
				Main.logger.log(Level.WARNING, "Tray: nie obsługiwany");
				return;
			}
			Main.logger.log(Level.INFO, "Tray: wspierany");

			try
			{
				tray = SystemTray.getSystemTray();
			}
			catch (Throwable e)
			{
				Main.logger.log(Level.SEVERE, "Tray: błąd pobierania", e);
				return;
			}
			Main.logger.log(Level.INFO, "Tray: pobrano");

			if (trayIconImageReady == null)
			{
				int traySize = Math.min(tray.getTrayIconSize().width,
					tray.getTrayIconSize().height);
				if (traySize < 22)
				{
					trayIconImageReady = ResourceManager.getImage("icons/16.png");
					trayIconImageUnread = ResourceManager.getImage("icons/message-16.png");
				}
				else if (traySize < 32)
				{
					trayIconImageReady = ResourceManager.getImage("icons/22.png");
					trayIconImageUnread = ResourceManager.getImage("icons/message-22.png");
				}
				else
				{
					trayIconImageReady = ResourceManager.getImage("icons/32.png");
					trayIconImageUnread = ResourceManager.getImage("icons/message-32.png");
				}


				trayIconImageReady = RemoveAlphaImageFilter.removeAlphaChannel(
					trayIconImageReady, 0xFFDDDDDD);
				trayIconImageUnread = RemoveAlphaImageFilter.removeAlphaChannel(
					trayIconImageUnread, 0xFFDDDDDD);
			}

			trayIcon = new TrayIcon(trayIconImageReady, Main.applicationName);
			trayIcon.setImageAutoSize(false);
			Main.logger.log(Level.INFO, "Tray: przygotowano ikonę");

			try
			{
				SystemTray.getSystemTray().add(trayIcon); // ~650 - 700ms
			}
			catch (AWTException e)
			{
				trayIcon = null;
				Main.logger.log(Level.SEVERE, "Tray: nie dodano", e);
				return;
			}

			Main.logger.log(Level.INFO, "Tray: OK");
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
