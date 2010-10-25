package views.swing;

import java.awt.*;
import java.awt.event.*;
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
			else if(e.getActionCommand().equals("ok"))
			{
				setVisible(false);
				saveConfigurationFromControls();
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
	
	private final GridBagConstraints gridBagConstraints = new GridBagConstraints();
	private final Border controlLabelBorder = BorderFactory.createEmptyBorder(0, 0, 0, 10);
	{
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new Insets(5, 0, 0, 0);
	}

	private synchronized void addControl(Component comp, String label)
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

		gridBagConstraints.weightx = 0.8;
		gridBagConstraints.gridx = 1;
		controlsPanel.add(comp, gridBagConstraints);

		comp.addKeyListener(configurationViewListener);
	}
	
	protected final JTextField fieldNick = new JTextField();
	protected final JCheckBox fieldIgnoreAutoResponses = new JCheckBox();
	protected final JCheckBox fieldAutoUpdate = new JCheckBox();

	protected final void prepareControls()
	{
		addControl(fieldNick, "Nick:");

		fieldIgnoreAutoResponses.setText("Ignoruj automatyczne odpowiedzi");
		addControl(fieldIgnoreAutoResponses, null);

		fieldAutoUpdate.setText("Automatycznie sprawdzaj aktualizacje");
		addControl(fieldAutoUpdate, null);
		
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
