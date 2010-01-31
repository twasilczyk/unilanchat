package main;

import controllers.MainController;
import tools.GUIUtilities;

/**
 * Główna klasa aplikacji, odpowiedzialna za jej uruchomienie.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class Main
{
	/**
	 * Nick użytkownika (tylko tymczasowo, dopóki nie będzie okien konfiguracyjnych).
	 *
	 * @todo nick ma być ustawiany w okienku konfiguracji
	 */
	public static String tmpNick = System.getProperty("user.name", "anonim");

	/**
	 * Uruchomienie aplikacji z konsoli.
	 *
	 * @param args Argumenty podane z konsoli.
	 */
	public static void main(String[] args)
	{
		if (args == null)
			throw new NullPointerException();

//		System.out.println("TMP: uruchamianie...");

		System.setProperty("line.separator", "\n");

//		for (Object k : System.getProperties().keySet())
//			System.out.println(k.toString() + " = " + System.getProperty((String)k));

		GUIUtilities.setApplicationName("UniLANChat");
		GUIUtilities.installCarefulRepaintManager(false);
		GUIUtilities.setBestLookAndFeel();

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("--nick"))
			{
				if (i + 1 < args.length)
					tmpNick = args[++i];
			}
		}

		final MainController mainController = new MainController();

		if (!views.swing.MainView.init(mainController))
			System.out.println("TMP: błąd ładowania programu!");
//		else
//			System.out.println("TMP: załadowano program");

		mainController.onAppLoad();
	}
}
