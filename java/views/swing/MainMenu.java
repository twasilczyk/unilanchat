package views.swing;

import java.awt.event.*;
import javax.swing.*;

/**
 * Komponent wyświetlający główne menu programu.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class MainMenu extends JMenuBar
{
	final MainView mainView;
	final MainMenuListener mainMenuListener = new MainMenuListener();

	class MainMenuListener implements ActionListener
	{
		public void actionPerformed(ActionEvent event)
		{
			String cmd = event.getActionCommand();
			if (cmd.equals("application.close"))
				mainView.getMainController().applicationClose();
			else
				assert(false);
		}
	}

	public MainMenu(MainView mainView)
	{
		this.mainView = mainView;

		JMenu menuProgram = new JMenu("Program");
		menuProgram.addActionListener(mainMenuListener);
		this.add(menuProgram);

		JMenuItem itemZamknij = new JMenuItem("Zamknij");
		itemZamknij.setActionCommand("application.close");
		itemZamknij.addActionListener(mainMenuListener);
		menuProgram.add(itemZamknij);
	}
}
