package components.swing;

import java.io.File;
import javax.swing.*;

/**
 * Okno dialogowe pozwalające wybrać miejsce do zapisania pliku. Jeżeli plik
 * istnieje, użytkownik zostaje spytany, czy plik ma być nadpisany. Jeżeli jest
 * to katalog, nie będzie możliwości nadpisania.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class JSaveFileChooser extends JFileChooser
{
	public JSaveFileChooser()
	{
		setFileHidingEnabled(false);
	}

	@Override
	public void approveSelection()
	{
		File file = getSelectedFile();

		if (file != null && file.exists())
		{
			if (!file.isDirectory() && file.canWrite())
			{
				if (JOptionPane.NO_OPTION == JOptionPane.showOptionDialog(this,
					"Czy na pewno chcesz nadpisać plik?", "Plik istnieje",
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
					null, null, this))
					return;
			}
			else
			{
				JOptionPane.showMessageDialog(this,
					"Plik lub folder istnieje i nie można go nadpisać.", "Plik istnieje",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		super.approveSelection();
	}
}
