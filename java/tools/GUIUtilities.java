package tools;

import java.awt.*;
import javax.swing.*;


/**
 * Klasa pomocnicza dla aplikacji korzystających z GUI (Swing, ewentualnie AWT).
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class GUIUtilities
{
	private GUIUtilities() { }

	/**
	 * Priorytet paczek Look and Feel (im niższy indeks, tym wyższy priorytet).
	 */
	protected static String[] lookAndFeelPriority = {
		"com.sun.java.swing.plaf.windows.WindowsLookAndFeel",
		"com.sun.java.swing.plaf.gtk.GTKLookAndFeel",
		"com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel",
		//"javax.swing.plaf.synth.SynthLookAndFeel",
		//"com.sun.java.swing.plaf.motif.MotifLookAndFeel",
		//"javax.swing.plaf.metal.MetalLookAndFeel",
		};

	/**
	 * Ustawia najlepszy wygląd (Look and Feel) według ustalonych priorytetów.
	 *
	 * @see #lookAndFeelPriority
	 * @return Nazwa ustawionego Look and Feel
	 */
	public static String setBestLookAndFeel()
	{
		for (int i = 0; i < lookAndFeelPriority.length; i++)
			try
			{
				UIManager.setLookAndFeel(lookAndFeelPriority[i]);
				return lookAndFeelPriority[i];
			}
			catch (Exception e)
			{
			}

		return UIManager.getLookAndFeel().getName();
	}

	/**
	 * Zmienia nazwę aplikacji - wykorzystywana m.in. podczas grupowania okien
	 * na pasku zadań.
	 *
	 * @param applicationName Nowa nazwa aplikacji
	 * @return Czy zakończono powodzeniem
	 */
	public static boolean setApplicationName(String applicationName)
	{
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		try
		{
			java.lang.reflect.Field awtAppClassNameField =
				toolkit.getClass().getDeclaredField("awtAppClassName");
			awtAppClassNameField.setAccessible(true);
			awtAppClassNameField.set(toolkit, applicationName);
			awtAppClassNameField.setAccessible(false);
		}
		catch (NoSuchFieldException e)
		{
			return false;
		}
		catch (IllegalAccessException e)
		{
			return false;
		}
		return true;
	}

	/**
	 * Wyśrodkowuje okno na ekranie.
	 *
	 * @param window Okno do wyśrodkowania
	 */
	public static void centerWindow(Window window)
	{
		if (window == null)
			throw new NullPointerException();
		Rectangle ownerPos;
		if (window.getOwner() == null)
		{
			ownerPos = new Rectangle();
			ownerPos.setSize(Toolkit.getDefaultToolkit().getScreenSize());
			ownerPos.setLocation(window.getLocation());
		}
		else
			ownerPos = window.getOwner().getBounds();
		window.setLocation(
				(int) ((ownerPos.getWidth() - window.getWidth()) / 2 + ownerPos.getX()),
				(int) ((ownerPos.getHeight() - window.getHeight()) / 2 + ownerPos.getY())
				);
	}

	/**
	 * Opakowanie SwingUtilities.invokeAndWait. Funkcję można wywoływać
	 * z EventDispatcherThread. Przechwycone są też wyjątki InterruptedException
	 * oraz InvocationTargetException - po ich wystąpieniu, metoda zwraca fałsz.
	 *
	 * @param runnable Kod do wykonania w wątku AWT
	 * @return Czy zakończony powodzeniem
	 */
	public static boolean swingInvokeAndWait(Runnable runnable)
	{
		try
		{
			if (SwingUtilities.isEventDispatchThread())
				runnable.run();
			else
				SwingUtilities.invokeAndWait(runnable);
		}
		catch (InterruptedException e)
		{
			return false;
		}
		catch (java.lang.reflect.InvocationTargetException e)
		{
			return false;
		}
		return true;
	}

	/**
	 * Podmienia RepaintManager na pilnujący dostępu do tych zasobów Swing, do
	 * których dostęp powinien być tylko z EventDispatchThread.
	 *
	 * @param crashOnErrors Czy w przypadku niedozwolonego dostępu rzucać
	 * wyjątek (czy tylko wyświetlić ostrzeżenie na stderr)
	 */
	public static void installCarefulRepaintManager(boolean crashOnErrors)
	{
		RepaintManager.setCurrentManager(new CarefulRepaintManager(crashOnErrors));

	}
}
class CarefulRepaintManager extends RepaintManager
{
	protected final boolean crashOnErrors;

	public CarefulRepaintManager(boolean crashOnErrors)
	{
		this.crashOnErrors = crashOnErrors;
	}

	protected void doThreadCheck()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			if (crashOnErrors)
				throw new RuntimeException("Nie wywołano z EventDispatchThread");
			else
			{
				System.err.println("Nie wywołano z EventDispatchThread");
				Thread.dumpStack();
			}
		}
	}

	@Override public synchronized void addInvalidComponent(JComponent jComponent)
	{
		doThreadCheck();
		super.addInvalidComponent(jComponent);
	}

	@Override public void addDirtyRegion(JComponent c, int x, int y, int w, int h)
	{
		doThreadCheck();
		super.addDirtyRegion(c, x, y, w, h);
	}
}
