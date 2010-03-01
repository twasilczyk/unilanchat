package views.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import components.swing.JImagePanel;
import controllers.MainController;
import protocols.Contact;
import resources.ResourceManager;

/**
 * Komponent wyświetlający panel zmiany statusu (status Dostępny / Zaraz wracam
 * oraz status opisowy).
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class StatusPanel extends JPanel implements Observer
{
	private final TextStatusPanel textStatusPanel;
	private final MainStatusCombo mainStatusCombo;
	protected final MainController mainController;

	public StatusPanel(MainController mainController)
	{
		this.mainController = mainController;
		textStatusPanel = new TextStatusPanel();
		mainStatusCombo = new MainStatusCombo();

		textStatusPanel.setVisible(false);

		this.setLayout(new BorderLayout());
		this.add(textStatusPanel, BorderLayout.SOUTH);
		this.add(mainStatusCombo, BorderLayout.NORTH);

		mainController.addObserver(this);
	}

	public void update(Observable o, Object arg)
	{
		if (o.equals(mainController) && arg.equals("status"))
			mainStatusCombo.update();
	}

	// <editor-fold defaultstate="collapsed" desc="Panel zmiany opisu">

	class TextStatusPanel extends JPanel implements ActionListener
	{
		protected final JTextField statusText = new JTextField();
		protected final JButton statusAccept = new JButton("OK");

		public TextStatusPanel()
		{
			this.setLayout(new BorderLayout());
			this.add(statusText, BorderLayout.CENTER);
			this.add(statusAccept, BorderLayout.EAST);

			statusAccept.setActionCommand("setStatus");
			statusAccept.addActionListener(this);
			statusAccept.setPreferredSize(new Dimension(50, 20));

			statusText.setActionCommand("setStatus");
			statusText.addActionListener(this);
		}

		public void actionPerformed(ActionEvent e)
		{
			if (!e.getActionCommand().equals("setStatus"))
				return;
			mainController.setTextStatus(statusText.getText());
			hidePanel();
		}

		@Override public boolean requestFocusInWindow()
		{
			return statusText.requestFocusInWindow();
		}

		public void showPanel()
		{
			statusText.setText(mainController.getTextStatus());
			setVisible(true);
			requestFocusInWindow();
			statusText.selectAll();
		}

		public void hidePanel()
		{
			setVisible(false);
			mainStatusCombo.requestFocusInWindow();
		}
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Panel zmiany statusu">

	class MainStatusCombo extends JComboBox
	{
		public MainStatusCombo()
		{
			super(new Object[]{
				Contact.UserStatus.ONLINE,
				Contact.UserStatus.BUSY,
				Contact.UserStatus.OFFLINE,
				"changeDescription"
			});
			this.setRenderer(new MainStatusComboRenderer());

			update();

			MainStatusComboController mainStatusComboController =
				new MainStatusComboController();

			addPopupMenuListener(mainStatusComboController);
			addKeyListener(mainStatusComboController);
		}

		public void update()
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					setSelectedItem(mainController.getStatus());
				}
			});
		}
	}

	class MainStatusComboController implements PopupMenuListener, KeyListener
	{
		protected boolean menuIsVisible = false;

		public void popupMenuCanceled(PopupMenuEvent e)
		{
			mainStatusCombo.setSelectedItem(mainController.getStatus());
		}

		public void popupMenuWillBecomeVisible(PopupMenuEvent e)
		{
			menuIsVisible = true;
		}

		public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
		{
			menuIsVisible = false;
			
			Object selected = mainStatusCombo.getSelectedItem();

			if (selected instanceof Contact.UserStatus)
			{
				mainStatusCombo.update();
				mainController.setStatus((Contact.UserStatus)selected);
			}
			if (selected instanceof String)
			{
				if (selected.equals("changeDescription"))
					textStatusPanel.showPanel();
				mainStatusCombo.update();
			}
		}

		public void keyTyped(KeyEvent e) { }
		public void keyReleased(KeyEvent e) { }

		public void keyPressed(KeyEvent e)
		{
			if (!menuIsVisible)
				mainStatusCombo.showPopup();
		}
	}
	
	class MainStatusComboRenderer implements ListCellRenderer
	{
		protected final JPanel render = new JPanel();

		protected final JImagePanel statusIcon = new JImagePanel();
		protected final JLabel statusLabel = new JLabel();

		protected final Image iconOnline = ResourceManager.getImage("status/online.png");
		protected final Image iconOffline = ResourceManager.getImage("status/offline.png");
		protected final Image iconBusy = ResourceManager.getImage("status/busy.png");

		protected final Color selectedStatusColor = new Color(100, 150, 200);

		public MainStatusComboRenderer()
		{
			render.setLayout(new BorderLayout(5, 0));
			render.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

			render.add(statusIcon, BorderLayout.WEST);
			render.add(statusLabel, BorderLayout.CENTER);

			statusIcon.setPreferredSize(new Dimension(12, 12));
		}

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus)
		{
			if (value instanceof Contact.UserStatus)
			{
				Contact.UserStatus status = (Contact.UserStatus)value;

				switch (status)
				{
					case ONLINE:
						statusLabel.setText("Dostępny");
						statusIcon.image = iconOnline;
						break;
					case BUSY:
						statusLabel.setText("Zaraz wracam");
						statusIcon.image = iconBusy;
						break;
					case OFFLINE:
						statusLabel.setText("Rozłączony");
						statusIcon.image = iconOffline;
						break;
				}
			}
			else if (value instanceof String)
			{
				statusLabel.setText("Zmień opis");
				statusIcon.image = null;
			}
			else
				throw new IllegalArgumentException();

			if (isSelected)
			{
				render.setOpaque(true);
				statusLabel.setForeground(Color.WHITE);
				render.setBackground(selectedStatusColor); // windows zapomina
			}
			else
			{
				render.setOpaque(false);
				statusLabel.setForeground(Color.BLACK);
			}

			return render;
		}
	}
	
	// </editor-fold>
}
