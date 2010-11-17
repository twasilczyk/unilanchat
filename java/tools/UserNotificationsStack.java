package tools;

import java.util.*;

/**
 * Kolekcja wiadomości dla użytkownika. W przyszłości może zostać rozszerzona
 * o wiadomości typu unique (np. o braku połączenia), usuwanie, a także
 * o dynamicznie konfigurowalne przyciski z ich akcjami.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class UserNotificationsStack extends SimpleObservable
{
	final Stack<Notification> notifications =
		new Stack<Notification>();

	/**
	 * Dodanie nowej wiadomości do stosu.
	 *
	 * @param title Tytuł wiadomości
	 * @param contents Treść wiadomości
	 */
	public void add(String title, String contents)
	{
		notifications.push(new Notification(title, contents));
		notifyObservers();
	}

	/**
	 * Sprawdza wiadomość na górze stosu bez jej zdejmowania.
	 *
	 * @return wiadomość z góry stosu, lub <code>null</code>, jeżeli pusty
	 */
	public Notification peek()
	{
		try
		{
			return notifications.peek();
		}
		catch (EmptyStackException ex)
		{
			return null;
		}
	}

	/**
	 * Sprawdza wiadomość na górze stosu i ją z niego zdejmuje.
	 *
	 * @return wiadomość z góry stosu, lub <code>null</code>, jeżeli pusty
	 */
	public Notification pop()
	{
		try
		{
			Notification notification = notifications.pop();
			notifyObservers();
			return notification;
		}
		catch (EmptyStackException ex)
		{
			return null;
		}
	}

	/**
	 * Sprawdza, czy stos jest pusty.
	 * 
	 * @return <code>true</code>, jeżeli pusty
	 */
	public boolean empty()
	{
		return notifications.empty();
	}

	/**
	 * Klasa przechowująca obiekty wiadomości. W przyszłości będą także
	 * przechowywać dynamiczne przyciski wraz z ich akcjami.
	 */
	public static class Notification
	{
		/**
		 * Tytuł wiadomości.
		 */
		public final String title;

		/**
		 * Treść wiadmomości.
		 */
		public final String contents;

		/**
		 * Utworzenie nowej wiadomości z ustalonym tytułem i treścią.
		 *
		 * @param title tytuł wiadomości
		 * @param contents treść wiadomości
		 */
		public Notification(String title, String contents)
		{
			if (title == null || contents == null)
				throw new NullPointerException();

			this.title = title;
			this.contents = contents;
		}
	}
}
