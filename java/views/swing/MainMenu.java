package views.swing;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import main.*;
import protocols.Account;
import protocols.ipmsg.IpmsgAccount;
import tools.ProcessingQueue;

/**
 * Komponent wyświetlający główne menu programu.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class MainMenu extends JMenuBar
{
	final MainView mainView;
	final MainMenuListener mainMenuListener = new MainMenuListener();

	AboutView aboutView;
	FileTransfersView fileTransfersView;

	class MainMenuListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			String cmd = event.getActionCommand();
			if (cmd.equals("application.close"))
				mainView.getMainController().applicationClose();
			else if (cmd.equals("application.transfers"))
			{
				if (fileTransfersView == null) // widok nie musi być od razu gotowy
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							if (fileTransfersView != null)
								fileTransfersView.showTransfers();
						}
					});
				else
					fileTransfersView.showTransfers();
			}
			else if (cmd.equals("help.about"))
			{
				if (aboutView == null) // widok nie musi być od razu gotowy
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							if (aboutView != null)
								aboutView.showAbout();
						}
					});
				else
					aboutView.showAbout();
			}
			else if (cmd.equals("debug.refreshIpmsg"))
			{
				for (Account acc : mainView.getMainController().getAccountsVector())
					if (acc instanceof IpmsgAccount)
						((IpmsgAccount)acc).speedupContactsRefresh();
			}
			else
				assert(false);
		}
	}

	class ProcessQueueObserver implements Observer
	{
		protected final JMenuItem menuItem;

		public ProcessQueueObserver(JMenuItem menuItem)
		{
			this.menuItem = menuItem;
		}

		public void update(final Observable o, Object arg)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					ProcessingQueue q = (ProcessingQueue)o;
					menuItem.setText(
						"Monitor kolejki zadań: " +
						q.getWaitingTasksCount() +
						(q.isBusy()?" (zajęty)":" (wolny)"));
				}
			});
		}
	}

	public MainMenu(MainView mainViewP)
	{
		this.mainView = mainViewP;

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// utworzenie tych okien zajmuje dużo czasu, a nie potrzebujemy
				// ich od razu po starcie aplikacji
				aboutView = new AboutView();
				fileTransfersView = new FileTransfersView(mainView.mainController.getFileTransfersController());
			}
		});

		JMenu menuProgram = new JMenu("Program");
		menuProgram.addActionListener(mainMenuListener);
		this.add(menuProgram);

		JMenuItem itemTransfers = new JMenuItem("Transfery plików");
		itemTransfers.setActionCommand("application.transfers");
		itemTransfers.addActionListener(mainMenuListener);
		menuProgram.add(itemTransfers);

		JMenuItem itemZamknij = new JMenuItem("Zamknij");
		itemZamknij.setActionCommand("application.close");
		itemZamknij.addActionListener(mainMenuListener);
		menuProgram.add(itemZamknij);

		JMenu menuHelp = new JMenu("Pomoc");
		menuHelp.addActionListener(mainMenuListener);
		this.add(menuHelp);

		JMenuItem itemAbout = new JMenuItem("O programie");
		itemAbout.setActionCommand("help.about");
		itemAbout.addActionListener(mainMenuListener);
		menuHelp.add(itemAbout);

		if (Configuration.getInstance().getDebugMode())
		{
			JMenu menuDebug = new JMenu("Debug");
			menuDebug.addActionListener(mainMenuListener);
			this.add(menuDebug);

			JMenuItem itemRefreshIpmsg = new JMenuItem("Odśwież listę kontaktów ipmsg");
			itemRefreshIpmsg.setActionCommand("debug.refreshIpmsg");
			itemRefreshIpmsg.addActionListener(mainMenuListener);
			menuDebug.add(itemRefreshIpmsg);

			JMenuItem itemPQueueMonitor = new JMenuItem("Monitor kolejki zadań: nieznany");
			menuDebug.add(itemPQueueMonitor);
			Main.backgroundProcessing.addObserver(
				new ProcessQueueObserver(itemPQueueMonitor));
		}
	}
}
