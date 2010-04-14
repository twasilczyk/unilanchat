package tools.systemintegration;

import jni.JNIManager;

/**
 * Obsługa funkcji powiadamiania o uruchamianiu w systemie Linux.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class X11StartupNotification
{
	/**
	 * Czy system obsługuje powiadamianie i odpowiednia biblioteka natywna
	 * została załadowana.
	 */
	public static final boolean isSupported =
		JNIManager.tryLoadLibrary("tools_systemintegration_X11StartupNotification");

	/**
	 * Nazwa zmiennej systemowej, przechowującej ID uruchomionej aplikacji.
	 */
	protected static final String startupIDEnvName = "DESKTOP_STARTUP_ID";

	private X11StartupNotification() { }

	/**
	 * Powiadomienie o uruchomieniu bieżącej aplikacji.
	 */
	public static void notifyStartupComplete()
	{
		if (!isSupported)
			throw new UnsupportedOperationException();

		String startupID = System.getenv(startupIDEnvName);
		if (startupID == null || startupID.isEmpty())
			return;
		System.clearProperty(startupIDEnvName);

		notifyStartupComplete(startupID);
	}

	private static native void notifyStartupCompleteNative(String startupID);

	/**
	 * Powiadomienie o uruchomieniu aplikacji o podanym ID.
	 *
	 * @param startupID ID uruchomionej aplikacji
	 */
	public static void notifyStartupComplete(String startupID)
	{
		if (startupID == null)
			throw new NullPointerException();
		if (!isSupported)
			throw new UnsupportedOperationException();
		notifyStartupCompleteNative(startupID);
	}
}
