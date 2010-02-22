package main;

import controllers.MainController;
import tools.*;

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

	public static ProcessingQueue backgroundProcessing = new ProcessingQueue();

	/**
	 * Oznaczenie numeru wersji w formacie:
	 * <numer główny>.<numer dodatkowy>[.<numer wydania>]
	 */
	public static final String version = "0.1";

	/**
	 * Stabilne wydania mają ustawioną tą flagę na false.
	 */
	public static final boolean isNightly = true;

	public static final String applicationName = "UniLANChat";

	public static final String applicationFullName =
		applicationName + " " + version + (isNightly?" (nightly)":"");

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

		GUIUtilities.setApplicationName(applicationFullName);
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
