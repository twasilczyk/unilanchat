package tools;

/**
 * Klasa wątku odpowiedzialnego za zarządzenia dostarczaniem danych, które mają
 * być cache-owane.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class CachedDataProvider extends Thread
{
	/**
	 * Co jaki czas (ms) dane mają się odświeżać.
	 */
	public int refreshRate = 60000;

	/**
	 * Ile czasu (ms) mamy czekać na pierwsze pobranie danych.
	 */
	public int firstRefreshTimeout = 100;

	/**
	 * Czy dane są już gotowe.
	 */
	private boolean isDataReady = false;

	/**
	 * Zlicza ilość instancji klasy - do generowania nazwy wątku.
	 */
	private static int instanceCounter = 0;

	/**
	 * Czy wątek ma rozpoczynać pracę od razu po utworzeniu.
	 */
	protected boolean autoStart = true;

	/**
	 * Główny konstruktor.
	 */
	public CachedDataProvider()
	{
		super("CachedDataProvider-" + (instanceCounter++));
		setDaemon(true);
		if (autoStart)
			start();
	}

	/**
	 * Konstruktor z możliwością podania nazwy wątku.
	 *
	 * @param name nazwa wątku
	 */
	public CachedDataProvider(String name)
	{
		super(name);
		setDaemon(true);
		if (autoStart)
			start();
	}

	/**
	 * Pętla pobierająca dane co ustalony przez {@link #refreshRate} czas.
	 *
	 * @see #refreshRate
	 */
	@Override final public void run()
	{
		while (true)
		{
			loadData();

			if (!isDataReady)
				synchronized (this)
				{
					isDataReady = true;
					notifyAll();
				}

			try
			{
				sleep(refreshRate);
			}
			catch (InterruptedException ex)
			{
				return;
			}
		}
	}

	/**
	 * Metoda pobierająca dane, wywoływana cyklicznie w {@link #run()}
	 *
	 * @see #run()
	 */
	abstract protected void loadData();

	/**
	 * Oczekuje na pierwsze pobranie danych. Jeżeli dane są gotowe, nic nie robi
	 * i szybko kończy działanie. W przeciwnym przypadku, oczekuje
	 * {@link #firstRefreshTimeout} milisekund na pobranie danych. Jeżeli
	 * nastąpi ono przed tym czasem, niezwłocznie kończy działanie, jeżeli nie -
	 * zaznacza, że pobrano dane (dzięki czemu kolejne wywołania nie będą trwały
	 * tak długo) i kończy działanie.
	 */
	public final void waitForData()
	{
		if (!isDataReady)
			synchronized (this)
			{
				if (!isDataReady)
					try
					{
						wait(firstRefreshTimeout);
					}
					catch (InterruptedException ex)
					{
						return;
					}
				if (!isDataReady)
				{
					isDataReady = true;
					notifyAll();
				}
			}
	}
}
