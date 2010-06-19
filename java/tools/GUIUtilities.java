package tools;

import java.applet.Applet;
import java.awt.*;
import java.lang.reflect.*;
import java.util.Vector;
import javax.swing.*;
import resources.ResourceManager;

/**
 * Klasa pomocnicza dla aplikacji korzystających z GUI (Swing, ewentualnie AWT).
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class GUIUtilities
{
	private GUIUtilities() { }

	private final static boolean isLinux =
		System.getProperty("os.name").contains("Linux");

	/**
	 * Priorytet paczek Look and Feel (im niższy indeks, tym wyższy priorytet).
	 */
	protected static String[] lookAndFeelPriority = {
		"com.sun.java.swing.plaf.windows.WindowsLookAndFeel",
		"com.sun.java.swing.plaf.gtk.GTKLookAndFeel",
		"com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel",
		//"com.sun.java.swing.plaf.motif.MotifLookAndFeel",
		//"javax.swing.plaf.metal.MetalLookAndFeel",
		};

	/**
	 * Ustawia najlepszy wygląd (Look and Feel) według ustalonych priorytetów.
	 *
	 * @param multiThread czy wykonać w wątku Swing - metoda może się wykonywać
	 * kilkaset milisekund. Zwalnia bieżący wątek, ale opóźnia zadania Swing
	 * @see #lookAndFeelPriority
	 */
	public static void setBestLookAndFeel(boolean multiThread)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				for (int i = 0; i < lookAndFeelPriority.length; i++)
					try
					{
						UIManager.setLookAndFeel(lookAndFeelPriority[i]);
						if (lookAndFeelPriority[i].endsWith("GTKLookAndFeel"))
							loadGTKFileChooserUI();
						break;
					}
					catch (Exception e)
					{
						continue;
					}

				for (Frame f : Frame.getFrames())
					SwingUtilities.updateComponentTreeUI(f);
			}
		};

		if (multiThread)
			SwingUtilities.invokeLater(r);
		else
			r.run();
	}

	/**
	 * Zastępuje przestarzałą implementację interfejsu dialogu wyboru plików /
	 * folderów tą z Metal L&F, z pewnymi modyfikacjami.
	 *
	 * @see <a href="http://www.java2s.com/Tutorial/Java/0240__Swing/CustomizingaJFileChooserLookandFeel.htm">www.java2s.com - Customizing a JFileChooser Look and Feel</a>
	 */
	private static void loadGTKFileChooserUI()
	{
		UIManager.put("FileChooserUI",
				"javax.swing.plaf.metal.MetalFileChooserUI");

		UIManager.put("FileChooser.upFolderIcon",
				ResourceManager.getIcon("FileChooser/upFolderIcon.png"));
		UIManager.put("FileChooser.homeFolderIcon",
				ResourceManager.getIcon("FileChooser/homeFolderIcon.png"));
		UIManager.put("FileChooser.newFolderIcon",
				ResourceManager.getIcon("FileChooser/newFolderIcon.png"));
		UIManager.put("FileChooser.listViewIcon",
				ResourceManager.getIcon("FileChooser/listViewIcon.png"));
		UIManager.put("FileChooser.detailsViewIcon",
				ResourceManager.getIcon("FileChooser/detailsViewIcon.png"));

		UIManager.put("FileChooser.listViewBorder",
				BorderFactory.createEmptyBorder());
		UIManager.put("FileChooser.readOnly", false);
	}

	/**
	 * Zmienia nazwę aplikacji - wykorzystywana m.in. podczas grupowania okien
	 * na pasku zadań.
	 *
	 * @param applicationName nowa nazwa aplikacji
	 * @return <code>true</code>, jeżeli zakończono powodzeniem
	 */
	public static boolean setApplicationName(String applicationName)
	{
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		try
		{
			Field awtAppClassNameField =
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

	// <editor-fold desc="Przywoływanie okna na wierzch">

	/**
	 * Przywołuje okno na wierzch systemowego managera okien. Jest to obejście
	 * wadliwej implementacji metody {@link Frame#toFront()}. Oryginalna
	 * implementacja nie zawsze przywołuje okno na wierzch - np. pod linuksem,
	 * jeżeli okno jest zminimalizowane, a po zminimalizowaniu inne (natywne)
	 * okno uzyska focus.
	 *
	 * Wywołanie metody może wstrzymać bieżący wątek na 250ms (zazwyczaj
	 * kilkanaście) - w przypadku aktywacji zminimalizowanego okna, oczekuje na
	 * jego pojawienie się.
	 *
	 * @param window okno do przywołania
	 * @todo pod systemem Linux metoda przywołuje wszystkie okna aplikacji na
	 * wierzch - dobrze by było znaleźć na to jakieś obejście
	 * @see Frame#toFront()
	 */
	public static void bringWindowToFront(final Frame window)
	{
		if (window.getState() == Frame.ICONIFIED)
		{
			window.setState(Frame.NORMAL);

			//czekamy na pojawienie się okna - max 250ms
			long waitStart = System.currentTimeMillis();
			while (window.getState() != Frame.NORMAL &&
				System.currentTimeMillis() - waitStart < 250)
				Thread.yield();
		}

		bringWindowToFrontLinuxHack(window);

		window.setVisible(true);
		window.toFront();
		window.requestFocus();

		Thread linuxHackRepeater = new Thread()
		{
			@Override
			public void run()
			{
				long waitStart = System.currentTimeMillis();
				while (!window.isActive() &&
					System.currentTimeMillis() - waitStart < 500)
					Thread.yield();
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						bringWindowToFrontLinuxHack(window);
					}
				});
			}
		};
		linuxHackRepeater.setDaemon(true);
		linuxHackRepeater.start();
	}

	/**
	 * Obejście problemu nie przywoływania okien na wierzch pod systemem Linux,
	 * dla metody {@link #bringWindowToFront(java.awt.Frame)}.
	 *
	 * Obejście polega zasadniczo na ukryciu i pokazaniu (pod X11 - wszystkich)
	 * okien, gdy nie można uzyskać focusa.
	 *
	 * @param window okno do przywołania
	 * @see #bringWindowToFront(java.awt.Frame)
	 */
	private static void bringWindowToFrontLinuxHack(final Frame window)
	{
		window.setVisible(false);
		if (isLinux)
		{
			Vector<Frame> visibleFrames = new Vector<Frame>();
			for (Frame f : Frame.getFrames())
				if (f.isVisible())
				{
					if (f.getClass().getCanonicalName().equals(
						"sun.awt.X11.XTrayIconPeer.XTrayIconEmbeddedFrame"))
						continue;
					f.setVisible(false);
					visibleFrames.add(f);
				}
			for (Frame f : visibleFrames)
				f.setVisible(true);
		}
		window.setVisible(true);
	}

	// </editor-fold>

	/**
	 * Opakowanie {@link SwingUtilities#invokeAndWait}. Funkcję można wywoływać
	 * z {@link EventDispatchThread}. Przechwycone są też wyjątki
	 * {@link InterruptedException} oraz {@link InvocationTargetException} -
	 * po ich wystąpieniu, metoda zwraca <code>false</code>.
	 *
	 * @param runnable kod do wykonania w wątku AWT
	 * @return <code>true</code>, jeżeli zakończony powodzeniem
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
			e.printStackTrace();
			return false;
		}
		catch (InvocationTargetException e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Podmienia {@link RepaintManager} na pilnujący dostępu do tych zasobów
	 * Swing, do których dostęp powinien być tylko z {@link EventDispatchThread}.
	 *
	 * @param crashOnErrors czy w przypadku niedozwolonego dostępu rzucać
	 * wyjątek (czy tylko wyświetlić ostrzeżenie na stderr)
	 */
	public static void installCarefulRepaintManager(boolean crashOnErrors)
	{
		RepaintManager.setCurrentManager(new CarefulRepaintManager(crashOnErrors));
	}

	/**
	 * Wywołuje metodę validate na wszystkich komponentach nadrzędnych (włącznie
	 * z bieżącym), do samego korzenia.
	 *
	 * @param c bieżący komponent
	 */
	public static void validateRoot(Component c)
	{
		Component root = c;
		while (c != null)
		{
			root = c;
			c = c.getParent();
		}
		root.validate();
	}
}

/**
 * RepaintManager sprawdzający, czy wątek mający dostęp do obiektów Swing, jest
 * do tego uprawniony.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
class CarefulRepaintManager extends RepaintManager
{
	protected final boolean crashOnErrors;

	/**
	 * Główny konstruktor.
	 *
	 * @param crashOnErrors czy w przypadku modyfikacji obiektu Swing z
	 * nieuprawnionego wątku, wywołać wyjątek
	 */
	public CarefulRepaintManager(boolean crashOnErrors)
	{
		this.crashOnErrors = crashOnErrors;
	}

	/**
	 * Sprawdzenie, czy bieżący wątek jest uprawniony do modyfikowania obiektów
	 * swing.
	 */
	protected void doThreadCheck()
	{
		if (SwingUtilities.isEventDispatchThread())
			return;
		if (Thread.currentThread().getName().startsWith("Image Fetcher "))
			return;
		
		if (crashOnErrors)
			throw new RuntimeException("Nie wywołano z EventDispatchThread, ale z: " +
				Thread.currentThread().getName());
		else
		{
			System.err.println("Nie wywołano z EventDispatchThread, ale z: " +
				Thread.currentThread().getName());
			Thread.dumpStack();
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

	@Override public void addDirtyRegion(Applet applet, int x, int y, int w, int h)
	{
		doThreadCheck();
		super.addDirtyRegion(applet, x, y, w, h);
	}

	@Override public void addDirtyRegion(Window window, int x, int y, int w, int h)
	{
		doThreadCheck();
		super.addDirtyRegion(window, x, y, w, h);
	}
}
