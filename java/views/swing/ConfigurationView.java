package views.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.Border;

import controllers.MainController;
import main.Configuration;
import resources.ResourceManager;

/**
 * Widok okna konfiguracji.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ConfigurationView extends JFrame
{
	final JPanel buttonsPanel = new JPanel();
	final JPanel controlsPanel = new JPanel();
	final ConfigurationViewListener configurationViewListener =
		new ConfigurationViewListener();
	final MainController mainController;

	public ConfigurationView(MainController mainController)
	{
		super("Konfiguracja");
		if (mainController == null)
			throw new NullPointerException();
		setVisible(false);
		this.mainController = mainController;

		setMinimumSize(new Dimension(400, 200));
		setPreferredSize(new Dimension(400, 200));
		setLayout(new BorderLayout());
		setIconImage(ResourceManager.getIcon("icons/32.png").getImage());
		addKeyListener(configurationViewListener);

		buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(buttonsPanel, BorderLayout.SOUTH);

		JPanel superControlsPanel = new JPanel(new BorderLayout());
		add(superControlsPanel, BorderLayout.CENTER);

		controlsPanel.setLayout(new GridBagLayout());
		controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		superControlsPanel.add(controlsPanel, BorderLayout.NORTH);

		JButton cancelButton = new JButton("Anuluj");
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(configurationViewListener);
		cancelButton.addKeyListener(configurationViewListener);
		buttonsPanel.add(cancelButton);

		JButton okButton = new JButton("OK");
		okButton.setActionCommand("ok");
		okButton.addActionListener(configurationViewListener);
		okButton.addKeyListener(configurationViewListener);
		okButton.setDefaultCapable(true);
		buttonsPanel.add(okButton);

		prepareControls();

		pack();
		setLocationRelativeTo(null); //wyśrodkowanie okna
	}

	public void showConfiguration()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if (getState() == Frame.ICONIFIED)
					setState(Frame.NORMAL);
				setVisible(true);
				requestFocus();
			}
		});
	}

	class ConfigurationViewListener implements ActionListener, KeyListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if (e.getActionCommand().equals("cancel"))
			{
				setVisible(false);
				loadControlsFromConfiguration();
			}
			else if (e.getActionCommand().equals("ok"))
			{
				setVisible(false);
				saveConfigurationFromControls();
			}
			else if (e.getActionCommand().startsWith("hint-"))
			{
				int hintID = Integer.parseInt(e.getActionCommand().substring(5));

				JOptionPane.showMessageDialog(controlsPanel, hintList.get(hintID),
					"Pomoc", JOptionPane.INFORMATION_MESSAGE);
			}
			else
				assert(false);
		}

		public void keyTyped(KeyEvent e) { }
		public void keyReleased(KeyEvent e) { }

		public void keyPressed(KeyEvent e)
		{
			if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
			{
				actionPerformed(new ActionEvent(e.getSource(), e.getID(), "cancel"));
				return;
			}
		}
	}

	// <editor-fold defaultstate="collapsed" desc="Kontrolki konfiguracji">

	protected static final Vector<String> hintList = new Vector<String>();

	private final GridBagConstraints gridBagConstraints = new GridBagConstraints();
	private final Border controlLabelBorder = BorderFactory.createEmptyBorder(0, 0, 0, 10);
	{
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new Insets(5, 0, 0, 0);
		gridBagConstraints.gridy = 0;
	}

	private synchronized void addControl(Component comp, String label, String hint)
	{
		if (comp == null)
			throw new NullPointerException();
		if (label != null)
		{
			JLabel controlLabel = new JLabel(label);
			controlLabel.setBorder(controlLabelBorder);
			controlLabel.setLabelFor(comp);
			controlLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			
			gridBagConstraints.weightx = 0.2;
			gridBagConstraints.gridx = 0;
			controlsPanel.add(controlLabel, gridBagConstraints);
		}

		gridBagConstraints.weightx = 0.7;
		gridBagConstraints.gridx = 1;
		controlsPanel.add(comp, gridBagConstraints);
		comp.addKeyListener(configurationViewListener);

		if (hint != null)
		{
			hintList.add(hint);
			int hintIndex = hintList.indexOf(hint);

			gridBagConstraints.weightx = 0.1;
			gridBagConstraints.gridx = 2;

			JButton hintButton = new JButton("?");
			hintButton.addActionListener(configurationViewListener);
			hintButton.setActionCommand("hint-" + hintIndex);
			hintButton.setBorderPainted(false);
			controlsPanel.add(hintButton, gridBagConstraints);
			hintButton.addKeyListener(configurationViewListener);
		}

		gridBagConstraints.gridy++;
	}
	
	protected final JTextField fieldNick = new JTextField();
	protected final JCheckBox fieldIgnoreAutoResponses = new JCheckBox();
	protected final JCheckBox fieldAutoUpdate = new JCheckBox();

	protected final void prepareControls()
	{
		addControl(fieldNick, "Nick:", null);

		fieldIgnoreAutoResponses.setText("Ignoruj automatyczne odpowiedzi");
		addControl(fieldIgnoreAutoResponses, null,
			"Oryginalny klient IPMsg pozwala ustawić \"automatyczną odpowiedź\" " +
			"na każdą otrzymaną wiadomość.\nTakie wiadomości bywają mało " +
			"użyteczne (zazwyczaj brzmią np. \"zaraz wracam\"), za to mogą " +
			"być\nuciążliwe, szczególnie podczas rozmowy w pokoju głównym.\n\n" +
			"Ta opcja pozwala na ignorowanie takich wiadomości, bez " +
			"wpływu na pozostałe.");

		fieldAutoUpdate.setText("Automatycznie sprawdzaj aktualizacje");
		addControl(fieldAutoUpdate, null,
			"Program może sprawdzać, czy jest już dostępna jego nowa wersja, a " +
			"gdy taka się pojawi,\npoinformować o tym użytkownika i skierować go " +
			"na stronę, z której można ją pobrać.\n\n" +
			"W czasie sprawdzania numeru wersji, nie są wysyłane żadne prywatne " +
			"dane (co można\nzweryfikować przeglądając kod źródłowy), a sama " +
			"instalacja nowej wersji musi być\nprzeprowadzona ręcznie przez użytkownika.");
		
		loadControlsFromConfiguration();
	}

	protected void loadControlsFromConfiguration()
	{
		Configuration conf = Configuration.getInstance();

		fieldNick.setText(conf.getNick());
		fieldIgnoreAutoResponses.setSelected(conf.getIgnoreAutoResponses());
		fieldAutoUpdate.setSelected(conf.getAutoUpdate());
	}

	protected void saveConfigurationFromControls()
	{
		Configuration conf = Configuration.getInstance();
		
		conf.setNick(fieldNick.getText());
		conf.setIgnoreAutoResponses(fieldIgnoreAutoResponses.isSelected());
		conf.setAutoUpdate(fieldAutoUpdate.isSelected());

		conf.notifyObservers();
		mainController.saveConfiguration();
	}

	// </editor-fold>

}
