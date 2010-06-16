package main;

import controllers.MainController;
import java.io.File;
import tools.*;
import tools.systemintegration.SystemDirectories;

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
	public static final String version = "0.2";

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
		applicationName + " " + version + (isNightly ? " (nightly)" : "");

	private static String appDir;

	/**
	 * Zwraca katalog aplikacji. Może on zostać podany w parametrach wywołania
	 * programu, w innym wypadku katalog jest zależny od systemu operacyjnego.
	 *
	 * @return katalog aplikacji
	 */
	public static String getAppDir()
	{
		if (appDir == null)
			throw new NullPointerException("Jeszcze nie ustawiono");
		return appDir;
	}

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
		GUIUtilities.setBestLookAndFeel(true);
		
		// <editor-fold defaultstate="collapsed" desc="Wczytywanie parametrów">

		String paramNick = null, paramUserDir = null;

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("--nick") && i + 1 < args.length)
				paramNick = args[++i];
			else if (args[i].equals("--user-dir") && i + 1 < args.length)
				paramUserDir = args[++i];
		}

		if(paramUserDir != null)
			appDir = paramUserDir;
		else
			appDir = SystemDirectories.getAppStoreDir(applicationName, true);

		Configuration.loadInstance(appDir + "config.xml"); // ~50ms

		if (paramNick != null)
			Configuration.getInstance().setNick(paramNick);

		Configuration.getInstance().notifyObservers();

		// </editor-fold>

		final MainController mainController = new MainController();

		if (!views.swing.MainView.init(mainController))
			System.out.println("TMP: błąd ładowania programu!");
//		else
//			System.out.println("TMP: załadowano program");

		mainController.onAppLoad();
	}
}
