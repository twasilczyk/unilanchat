package views.swing;

import java.awt.event.*;
import javax.swing.*;

import main.Main;

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

	class MainMenuListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			String cmd = event.getActionCommand();
			if (cmd.equals("application.close"))
				mainView.getMainController().applicationClose();
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
			else
				assert(false);
		}
	}

	public MainMenu(MainView mainView)
	{
		this.mainView = mainView;

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// utworzenie tego okna zajmuje dużo czasu, a nie potrzebujemy
				// go od razu po starcie aplikacji
				aboutView = new AboutView();
			}
		});

		JMenu menuProgram = new JMenu("Program");
		menuProgram.addActionListener(mainMenuListener);
		this.add(menuProgram);

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
	}
}
