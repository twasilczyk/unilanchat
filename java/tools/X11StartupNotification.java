package tools;

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
	 * została załadowana
	 */
	public static final boolean isSupported =
		JNIManager.tryLoadLibrary("tools_X11StartupNotification");
	
	protected static final String startupIDEnvName = "DESKTOP_STARTUP_ID";

	private X11StartupNotification() { }

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

	public static void notifyStartupComplete(String startupID)
	{
		if (startupID == null)
			throw new NullPointerException();
		if (!isSupported)
			throw new UnsupportedOperationException();
		notifyStartupCompleteNative(startupID);
	}
}
