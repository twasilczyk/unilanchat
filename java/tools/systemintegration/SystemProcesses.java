package tools.systemintegration;

import java.io.IOException;

import tools.WholeStreamReader;

/**
 * Klasa do zarządzania procesami systemowymi.
 * 
 * @author Piotr Gajowiak
 */
public class SystemProcesses
{
	/**
	 * Wykonuje polecenie zadane w parametrze i zwraca jego wynik.
	 * 
	 * @param command polecenie do wykonania
	 * @return wynik działania polecenia
	 */
	public static String systemExec(String command)
	{
		try
		{
			Process process = Runtime.getRuntime().exec(command);
			WholeStreamReader reader = new WholeStreamReader(process.getInputStream());
			reader.start();
			process.waitFor();
			reader.join();
			return reader.getResult();
		}
		catch (IOException ex)
		{
			throw new RuntimeException("Nie udało się uruchomić polecenia: " + command, ex);
		}
		catch (InterruptedException ex)
		{
			throw new RuntimeException("Nie udało się uruchomić polecenia: " + command, ex);
		}
	}
}
