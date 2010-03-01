package tools;

/**
 * Kolekcja zawierająca dwa elementy.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class Pair<L, R>
{
	/**
	 * Pierwszy element pary.
	 */
	public final L left;

	/**
	 * Drugi element pary.
	 */
	public final R right;

	/**
	 * Konstruktor inicjujący na stałe oba elementy pary.
	 *
	 * @param left pierwszy element
	 * @param right drugi element
	 */
	public Pair(L left, R right)
	{
		this.left = left;
		this.right = right;
	}
}
