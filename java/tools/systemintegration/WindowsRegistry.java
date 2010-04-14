package tools.systemintegration;

/**
 * Klasa do zarządzania rejestrem w Windows.
 *
 * @author Piotr Gajowiak
 */
public class WindowsRegistry
{
	/**
	 * Zwraca dane zapisane pod daną ścieżką i kluczem w rejestrze Windows.
	 * 
	 * @param keyPath ścieżka w rejestrze
	 * @param keyName klucz rejestru
	 * @return dane z klucza, bądź null gdy nie istnieją lub są puste
	 */
	public static Object readValue(String keyPath, String keyName)
	{

		String response = SystemProcesses.systemExec("REG QUERY \"" + keyPath.trim() + "\" " + "/v " + keyName);

		String[] lines = response.split("\n");

		boolean containsHeader = false;
		boolean containsPath = false;
		//boolean containsName = false;
		for(String line : lines)
		{
			line = line.trim();
			if(line == null || line.equals(""))
				continue;
			else if (line.contains("REG.EXE"))
				containsHeader = true;
			else if (line.equals(keyPath.trim()))
				containsPath = true;
			else if (line.startsWith(keyName))
			{
				line = line.substring(keyName.length()).trim();
				if (line.startsWith("REG_SZ"))
				{
					line = line.substring(6).trim();
					return line;
				}
				else if (line.startsWith("REG_BINARY"))
				{
					/*line = line.substring(10).trim();
					if(line.length() % 2 != 0)
						throw new RuntimeException("Nierozpoznawalny wynik polecenia");
					byte[] buf = new byte[line.length() / 2];
					try
					{
						for(int i = 0; i < line.length() / 2; i += 2)
							buf[i / 2] = (byte) Integer.parseInt(line.substring(i, i + 2), 16);
						return buf;
					}
					catch(NumberFormatException ex)
					{
						throw new RuntimeException("Nierozpoznawalny wynik polecenia", ex);
					}*/
					throw new UnsupportedOperationException("Obsluga REG_BINARY jest jeszcze nie przetestowana");
				}
				else if (line.startsWith("REG_DWORD"))
				{
					line = line.substring(9).trim();
					try
					{
						if (!line.startsWith("0x"))
							throw new RuntimeException("Nierozpoznawalny wynik polecenia");
						line = line.substring(2);
						return Integer.parseInt(line, 16);
					}
					catch (NumberFormatException ex)
					{
						throw new RuntimeException("Nierozpoznawalny wynik polecenia", ex);
					}
				}
				else if (line.startsWith("REG_MULTI_SZ"))
				{
					line = line.substring(12).trim();
					line = line.replace("\\0", "\n");
					return line;
				}
				else if (line.startsWith("REG_EXPAND_SZ"))
				{
					line = line.substring(13).trim();
					return line;
				}
				else
					throw new RuntimeException("Nierozpoznawalny wynik polecenia");
			}
		}
		if (!(containsHeader && containsPath))
			throw new RuntimeException("Nierozpoznawalny wynik polecenia");
		return null;
	}
}
