package jni;

/**
 * Klasa wyjątku oznaczającego błąd ładowania biblioteki natywnej (za pomocą
 * klasy {@link JNIManager}).
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class NativeLibraryLoadFailed extends UnsatisfiedLinkError
{
	/**
	 * Błąd ładowania spowodowany w klasie {@link JNIManager}.
	 *
	 * @param libname nazwa ładowanej biblioteki
	 */
	public NativeLibraryLoadFailed(String libname)
	{
		super("Błąd ładowania biblioteki " +libname);
	}

	/**
	 * Błąd ładowania spowodowany innym wyjątkiem.
	 *
	 * @param libname nazwa ładowanej biblioteki
	 * @param cause wyjątek, który spowodował błąd
	 */
	public NativeLibraryLoadFailed(String libname, Throwable cause)
	{
		super("Błąd ładowania biblioteki " +libname);
		initCause(cause);
	}
}
