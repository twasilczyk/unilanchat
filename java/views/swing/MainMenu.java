package views.swing;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import main.*;
import protocols.Account;
import protocols.ipmsg.IpmsgAccount;
import tools.*;

/**
 * Komponent wyświetlający główne menu programu.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class MainMenu extends JMenuBar
{
	final MainView mainView;
	final MainMenuListener mainMenuListener = new MainMenuListener();

	HeavyObjectLoader<AboutView> aboutView =
			new HeavyObjectLoader<AboutView>(1000);
	HeavyObjectLoader<FileTransfersView> fileTransfersView =
			new HeavyObjectLoader<FileTransfersView>(1000);
	HeavyObjectLoader<ConfigurationView> configurationView =
			new HeavyObjectLoader<ConfigurationView>(1000);

	public void showFileTransfersView()
	{
		Main.backgroundProcessing.invokeLater(new Runnable()
		{
			public void run()
			{
				fileTransfersView.get().showTransfers();
			}
		});
	}

	class MainMenuListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			String cmd = event.getActionCommand();
			if (cmd.equals("application.close"))
				mainView.getMainController().applicationClose();
			else if (cmd.equals("application.transfers"))
			{
				Main.backgroundProcessing.invokeLater(new Runnable()
				{
					public void run()
					{
						showFileTransfersView();
					}
				});
			}
			else if (cmd.equals("help.about"))
			{
				Main.backgroundProcessing.invokeLater(new Runnable()
				{
					public void run()
					{
						aboutView.get().showAbout();
					}
				});
			}
			else if (cmd.equals("application.configuration"))
			{
				Main.backgroundProcessing.invokeLater(new Runnable()
				{
					public void run()
					{
						configurationView.get().showConfiguration();
					}
				});
			}
			else if (cmd.equals("debug.refreshIpmsg"))
			{
				for (Account acc : mainView.getMainController().getAccountsVector())
					if (acc instanceof IpmsgAccount)
						((IpmsgAccount)acc).speedupContactsRefresh();
			}
			else if (cmd.equals("debug.printIfaces"))
			{
				StringBuilder msg = new StringBuilder();

				msg.append("Adresy lokalne:\n");
				for (java.net.Inet4Address addr : net.InterfaceInfoProvider.getLocalAdresses())
				{
					msg.append(addr);
					msg.append('\n');
				}
				msg.append("\nAdresy broadcast:\n");
				for (java.net.Inet4Address addr : net.InterfaceInfoProvider.getBroadcastAdresses())
				{
					msg.append(addr);
					msg.append('\n');
				}
				JOptionPane.showMessageDialog(mainView, msg.toString(),
					"Interfejsy sieciowe", JOptionPane.INFORMATION_MESSAGE);
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

		fileTransfersView.load(new HeavyObjectLoader.SwingInitializer<FileTransfersView>()
		{
			@Override
			public FileTransfersView buildSwing()
			{
				return new FileTransfersView(
					mainView.mainController.getFileTransfersController());
			}
		});

		aboutView.load(new HeavyObjectLoader.SwingInitializer<AboutView>()
		{
			@Override
			public AboutView buildSwing()
			{
				return new AboutView();
			}
		});

		configurationView.load(new HeavyObjectLoader.SwingInitializer<ConfigurationView>()
		{
			@Override
			public ConfigurationView buildSwing()
			{
				return new ConfigurationView(mainView.mainController);
			}
		});

		JMenu menuProgram = new JMenu("Program");
		menuProgram.addActionListener(mainMenuListener);
		this.add(menuProgram);

		JMenuItem itemTransfers = new JMenuItem("Transfery plików");
		itemTransfers.setActionCommand("application.transfers");
		itemTransfers.addActionListener(mainMenuListener);
		menuProgram.add(itemTransfers);

		JMenuItem itemConfiguration = new JMenuItem("Ustawienia");
		itemConfiguration.setActionCommand("application.configuration");
		itemConfiguration.addActionListener(mainMenuListener);
		menuProgram.add(itemConfiguration);

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

			JMenuItem itemPrintIfaces = new JMenuItem("Interfejsy sieciowe");
			itemPrintIfaces.setActionCommand("debug.printIfaces");
			itemPrintIfaces.addActionListener(mainMenuListener);
			menuDebug.add(itemPrintIfaces);

			JMenuItem itemPQueueMonitor = new JMenuItem("Monitor kolejki zadań: nieznany");
			menuDebug.add(itemPQueueMonitor);
			Main.backgroundProcessing.addObserver(
				new ProcessQueueObserver(itemPQueueMonitor));
		}
	}
}
