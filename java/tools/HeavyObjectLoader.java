package tools;

import java.util.Vector;
import javax.swing.SwingUtilities;

/**
 * Klasa służąca do inicjowania "ciężkich" komponentów w oddzielnym wątku.
 *
 * @param <T> opakowywana klasa
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class HeavyObjectLoader<T>
{
	private final HeavyObjectLoaderThread loaderThread =
			new HeavyObjectLoaderThread();

	/**
	 * Wskazuje, czy opakowywany obiekt jest już gotowy do użycia.
	 */
	protected boolean isReady = false;

	/**
	 * Opakowywany obiekt.
	 */
	protected T obj;

	/**
	 * Ile czasu (w milisekundach) wątek inicjujący obiekt będzie czekał, zanim
	 * rozpocznie pracę.
	 */
	protected final long initialDelay;

	/**
	 * Inicjalizator opakowywanego obiektu.
	 */
	protected Initializer<T> initializer = null;

	/**
	 * Domyślny konstruktor, uruchamia proces inicjowania od razu po utworzeniu.
	 */
	public HeavyObjectLoader()
	{
		this.initialDelay = 0;
		loaderThread.start();
	}

	/**
	 * Konstruktor, który czeka z rozpoczęciem inicjowania obiektu określony
	 * czas. Odliczanie czasu rozpoczyna się wraz z utworzeniem instancji tej
	 * klasy (a nie wraz z dostarczeniem obiektu inicjalizatora).
	 *
	 * W szczególności, jeżeli inicjalizator zostanie dostarczony po upływie
	 * podanego czasu, inicjalizacja obiektu zostanie przeprowadzona
	 * bezzwłocznie.
	 *
	 * @param initialDelay czas oczekiwania przed rozpoczęciem inicjalizacji
	 * (w milisekundach)
	 */
	public HeavyObjectLoader(long initialDelay)
	{
		this.initialDelay = initialDelay;
		loaderThread.start();
	}

	private static int heavyObjectLoaderThreadCount = 0;

	class HeavyObjectLoaderThread extends Thread
	{
		public HeavyObjectLoaderThread()
		{
			super("HeavyObjectLoaderThread-" + (heavyObjectLoaderThreadCount++));
			setDaemon(true);
			setPriority(getPriority() - 1);
		}

		@Override
		public void run()
		{
			try
			{
				Thread.sleep(initialDelay);
			}
			catch (InterruptedException ex)
			{
				return;
			}
			
			while (initializer == null)
			{
				try
				{
					synchronized(this)
					{
						wait();
					}
				}
				catch (InterruptedException ex)
				{
					return;
				}
			}

			obj = initializer.build();
			isReady = true;
		}
	}

	/**
	 * Inicjalizator opakowywanego obiektu.
	 *
	 * @param <T> opakowywana klasa
	 */
	public interface Initializer<T>
	{
		/**
		 * Generuje opakowywany obiekt. Być może będzie się długo wykonywać.
		 *
		 * @return wygenerowany obiekt
		 */
		public T build();
	}

	/**
	 * Inicjalizator opakowywanego komponentu Swing. Obiekty tego typu muszą być
	 * obsługiwane w wątku EventDispatchThread, więc ta klasa robi to w sposób
	 * przezroczysty.
	 *
	 * @param <T> opakowywana klasa
	 */
	public static abstract class SwingInitializer<T> implements Initializer<T>
	{
		final Vector<T> buildBuffer = new Vector<T>();

		/**
		 * Generuje opakowywany obiekt w wątku EventDispatchThread, wywołując
		 * w nim metodę {@link #buildSwing()}.
		 *
		 * @return wygenerowany obiekt
		 * @see #buildSwing()
		 */
		final public T build()
		{
			if (!buildBuffer.isEmpty())
				return buildBuffer.firstElement();
			GUIUtilities.swingInvokeAndWait(new Runnable()
			{
				public void run()
				{
					buildBuffer.addElement(buildSwing());
				}
			});
			if (buildBuffer.isEmpty())
				throw new RuntimeException("Nie wygenerowano obiektu");
			assert(buildBuffer.size() == 1);
			
			return buildBuffer.firstElement();
		}

		/**
		 * Generuje opakowywany obiekt Swing. Być może będzie się długo
		 * wykonywać. Metoda zostanie wykonana w wątku EventDispatchThread (nie
		 * ma potrzeby używania {@link SwingUtilities#invokeAndWait(java.lang.Runnable)}).
		 *
		 * @return wygenerowany obiekt
		 */
		public abstract T buildSwing();
	}

	/**
	 * Dostarcza inicjalizatora dla obiektu. Należy wywołać raz, najlepiej
	 * wkrótce po utworzeniu instancji tej klasy.
	 *
	 * @param initializer inicjalizator dla opakowywanego obiektu
	 */
	public void load(Initializer<T> initializer)
	{
		if (initializer == null)
			throw new NullPointerException();

		synchronized (loaderThread)
		{
			if (this.initializer != null)
				throw new RuntimeException("Nie można dwukrotnie inicjować obiektu");
			this.initializer = initializer;
			
			loaderThread.notify();
		}
	}

	/**
	 * Zwraca opakowywany obiekt.
	 *
	 * @return opakowywany obiekt, lub <code>null</code> jeżeli przerwano
	 * ({@link InterruptedException}) oczekiwanie na wątek inicjujący
	 */
	public T get()
	{
		if (SwingUtilities.isEventDispatchThread())
			throw new RuntimeException("Nie powinno być wykonywane z EventDispatchThread");

		if (isReady)
			return obj;

		if (!isReady)
		{
			try
			{
				loaderThread.join();
			}
			catch (InterruptedException ex)
			{
				return null;
			}
		}

		assert(isReady);

		return obj;
	}

	/**
	 * Odpowiada, czy opakowywany obiekt jest już gotowy do użycia.
	 *
	 * @return <code>true</code>, jeżeli obiekt jest gotowy do użycia
	 */
	public boolean isReady()
	{
		return isReady;
	}
}
