package jni;

/**
 * Klasa wyjątku oznaczającego błąd ładowania biblioteki natywnej (za pomocą
 * klasy JNIManager).
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class NativeLibraryLoadFailed extends UnsatisfiedLinkError
{
	public NativeLibraryLoadFailed(String libname)
	{
		super("Błąd ładowania biblioteki " +libname);
	}

	public NativeLibraryLoadFailed(String libname, Throwable cause)
	{
		super("Błąd ładowania biblioteki " +libname);
		initCause(cause);
	}
}
