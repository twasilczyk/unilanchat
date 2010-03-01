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
	 * Główna kolejka dla zadań do wykonania w tle.
	 */
	public static ProcessingQueue backgroundProcessing = new ProcessingQueue();

	/**
	 * Oznaczenie numeru wersji w formacie:
	 * <code>&lt;numer główny&gt;.&lt;numer dodatkowy&gt;[.&lt;numer wydania&gt;]</code>.
	 */
	public static final String version = "0.1";

	/**
	 * Czy jest to build testowy. Stabilne wydania mają ustawioną tą flagę
	 * na false.
	 */
	public static final boolean isNightly = true;

	/**
	 * Nazwa aplikacji (krótka).
	 *
	 * @see #applicationFullName
	 */
	public static final String applicationName = "UniLANChat";

	/**
	 * Pełna nazwa aplikacji, wraz z numerem wersji.
	 *
	 * @see #applicationName
	 * @see #version
	 * @see #isNightly
	 */
	public static final String applicationFullName =
		applicationName + " " + version + (isNightly?" (nightly)":"");

	/**
	 * Uruchomienie aplikacji z konsoli.
	 *
	 * @param args argumenty podane z konsoli.
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
			if (args[i].equals("--nick") && i + 1 < args.length)
				Configuration.getInstance().setNick(args[++i]);

			Configuration.getInstance().notifyObservers();
		}

		final MainController mainController = new MainController();

		if (!views.swing.MainView.init(mainController))
			System.out.println("TMP: błąd ładowania programu!");
//		else
//			System.out.println("TMP: załadowano program");

		mainController.onAppLoad();
	}
}
