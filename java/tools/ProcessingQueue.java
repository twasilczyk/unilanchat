package tools;

import java.util.Vector;

/**
 * Klasa kolejkująca zadania do wykonania w osobnym wątku. Może być
 * wykorzystywana np. do realizacji bardziej obciążających odpowiedzi na akcje
 * użytkownika, aby nie obciążać wątku Swinga.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ProcessingQueue extends SimpleObservable
{
	/**
	 * Kolejka zadań do wykonania.
	 */
	protected final Vector<Runnable> taskQueue = new Vector<Runnable>();

	/**
	 * Czy aktualnie jest wykonywane jakieś zadanie.
	 */
	protected boolean busy = false;
	
	private final ApplicationProcessingThread procThread =
		new ApplicationProcessingThread();

	/**
	 * Kolejkuje zadanie do wykonania w osobnym wątku. Zadania wykonywane są
	 * po kolei, nie są tworzone dodatkowe wątki.
	 *
	 * @param doRun zadanie do wykonania
	 */
	public void invokeLater(Runnable doRun)
	{
		synchronized (taskQueue)
		{
			if (taskQueue.isEmpty())
				taskQueue.notifyAll();
			taskQueue.add(doRun);
		}
		notifyObservers();
	}

	/**
	 * @return ilość zadań w kolejce do wykonania
	 */
	public int getWaitingTasksCount()
	{
		return taskQueue.size();
	}

	/**
	 * @return czy aktualnie jest wykonywane jakieś zadanie
	 */
	public boolean isBusy()
	{
		return busy;
	}

	private static int applicationProcessingThreadCount = 0;

	/**
	 * Wątek wykonujący zakolejkowane zadania.
	 */
	class ApplicationProcessingThread extends Thread
	{
		public ApplicationProcessingThread()
		{
			super("ProcessingQueue-" + (applicationProcessingThreadCount++));
			setDaemon(true);
			start();
		}

		@Override public void run()
		{
			while (true)
				if (taskQueue.isEmpty())
				{
					try
					{
						synchronized (taskQueue)
						{
							taskQueue.wait();
						}
					}
					catch (InterruptedException ex)
					{
						return;
					}
				}
				else
				{
					Runnable doRun = taskQueue.remove(0);
					busy = true;
					notifyObservers();
					
					try
					{
						doRun.run();
					}
					catch (Throwable ex)
					{
						ex.printStackTrace();
					}
					
					busy = false;
					notifyObservers();
				}
		}
	}
}
