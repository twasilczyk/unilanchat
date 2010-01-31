package tools;

/**
 * Klasa kodu wykonywalnego z parametrem. Pomocnicza m.in. przy definiowaniu
 * klas inline dla metod invokeLater itp.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class ParametrizedRunnable<T> implements Runnable
{
	/**
	 * Parametr do przekazania wykonywanemu blokowi.
	 */
	protected final T parameter;

	/**
	 * Konstruktor przekazujący parametr do bloku.
	 *
	 * @param parameter Parametr do przekazania
	 */
	public ParametrizedRunnable(T parameter)
	{
		this.parameter = parameter;
	}
}
