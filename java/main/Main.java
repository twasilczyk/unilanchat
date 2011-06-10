package main;

import java.util.logging.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import controllers.MainController;
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
	public static final ProcessingQueue backgroundProcessing = new ProcessingQueue();

	/**
	 * Stos ostrzeżeń dla użytkownika.
	 */
	public static final UserNotificationsStack userNotifications = new UserNotificationsStack();

	public static final Logger logger = Logger.getLogger("uncaughtExceptionsLog");

	/**
	 * Oznaczenie numeru wersji w formacie:
	 * <code>&lt;numer główny&gt;.&lt;numer dodatkowy&gt;[.&lt;numer wydania&gt;]</code>.
	 */
	public static final String version = "0.4";

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

		logger.setLevel(isNightly ? Level.INFO : Level.WARNING);

		System.setProperty("line.separator", "\n");

//		for (Object k : System.getProperties().keySet())
//			System.out.println(k.toString() + " = " + System.getProperty((String)k));

		// <editor-fold defaultstate="collapsed" desc="Wczytywanie parametrów">

		String paramNick = null, paramUserDir = null;

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("--nick") && i + 1 < args.length)
				paramNick = args[++i];
			else if (args[i].equals("--user-dir") && i + 1 < args.length)
				paramUserDir = args[++i];
			else if (args[i].equals("--verbose"))
				logger.setLevel(Level.ALL);
		}

		if(paramUserDir != null)
			appDir = paramUserDir;
		else
			appDir = SystemDirectories.getAppStoreDir(applicationName, true);

		// </editor-fold>

		installDefaultExceptionHandler();

		logger.log(Level.INFO, "Uruchamianie...");

		GUIUtilities.setApplicationName(applicationFullName);
		GUIUtilities.installCarefulRepaintManager(false);
		GUIUtilities.setBestLookAndFeel(true);

		// <editor-fold defaultstate="collapsed" desc="Wczytywanie konfiguracji">

		Configuration.loadInstance(appDir + "config.xml"); // ~50ms

		if (paramNick != null)
			Configuration.getInstance().setNick(paramNick);

		Configuration.getInstance().notifyObservers();

		// </editor-fold>

		final MainController mainController = new MainController();

		if (!views.swing.MainView.init(mainController))
		{
			logger.log(Level.WARNING, "Nie udało się uruchomić widoku");
			return;
		}
		else
			logger.log(Level.INFO, "Widok uruchomiony");

		mainController.onAppLoad();
	}

	private static void installDefaultExceptionHandler()
	{
		final FileHandler fileHandler;
		final ConsoleHandler consoleHandler = new ConsoleHandler();
		
		try
		{
			fileHandler = new FileHandler(getAppDir() + "error_log", true);
		}
		catch (IOException ex)
		{
			throw new RuntimeException("Nie udało się utworzyć logu");
		}

		Formatter exceptionFormatter = new Formatter()
		{
			final SimpleDateFormat dateFormat =
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			@Override
			public String format(LogRecord lr)
			{
				String exceptionDetails = "";
				if (lr.getThrown() != null)
				{
					StringWriter stackTraceWriter = new StringWriter();
					lr.getThrown().printStackTrace(new PrintWriter(stackTraceWriter));
					exceptionDetails = "\n" + stackTraceWriter.toString();
				}

				return dateFormat.format(new Date(lr.getMillis())) +
					" " + lr.getLevel().getName() + ": " +
					lr.getMessage() +
					exceptionDetails +
					"\n";
			}
		};

		fileHandler.setFormatter(exceptionFormatter);
		consoleHandler.setFormatter(exceptionFormatter);
		fileHandler.setLevel(logger.getLevel());
		consoleHandler.setLevel(logger.getLevel());

		logger.setUseParentHandlers(false);
		logger.addHandler(fileHandler);
		logger.addHandler(consoleHandler);

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
		{
			public void uncaughtException(Thread thread, Throwable thrwbl)
			{
				logger.log(Level.SEVERE,
					"Nie złapany wyjątek w wątku \"" +
					thread.getName() + "\"", thrwbl);
			}
		});
	}
}
