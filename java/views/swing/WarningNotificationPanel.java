package views.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import resources.ResourceManager;
import tools.UserNotificationsStack;

/**
 * Panel informujący użytkownika o problemach związanych z aplikacją.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class WarningNotificationPanel extends JPanel
{
	final JLabel titleLabel = new JLabel();
	final JTextPane contentPane = new JTextPane();
	final WarningNotificationPanelListener listener =
		new WarningNotificationPanelListener();

	final UserNotificationsStack messages;

	public WarningNotificationPanel(UserNotificationsStack messages)
	{
		if (messages == null)
			throw new NullPointerException();
		this.messages = messages;
		messages.addObserver(listener);

		setLayout(new BorderLayout(0, 3));
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		titleLabel.setIcon(ResourceManager.getIcon("warningIcon.png"));
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		add(titleLabel, BorderLayout.NORTH);

		JPanel buttonPanel = new JPanel();
		add(buttonPanel, BorderLayout.SOUTH);
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.add(Box.createHorizontalGlue());

		JButton buttonOK = new JButton("OK");
		buttonOK.setFont(buttonOK.getFont().deriveFont(
			(float)(buttonOK.getFont().getSize() * 0.8)));
		buttonOK.setActionCommand("ok");
		buttonOK.addActionListener(listener);
		buttonPanel.add(buttonOK);

		contentPane.setBackground(null);
		contentPane.setBorder(null);
		contentPane.setEditable(false);
		contentPane.setText("test");
		contentPane.setFont(contentPane.getFont().deriveFont(
			(float)(contentPane.getFont().getSize() * 0.85)));
		// dodawane przy wyświetlaniu wiadomości, ponieważ w przeciwnym wypadku
		// BorderLayout zwija to pole
//		add(contentPane, BorderLayout.CENTER);

		refresh();
	}

	class WarningNotificationPanelListener implements Observer, ActionListener
	{
		public void actionPerformed(ActionEvent ae)
		{
			if (ae.getActionCommand().equals("ok"))
				messages.pop();
			else
				throw new RuntimeException("Nieprawidłowa akcja");
		}

		public void update(Observable o, Object o1)
		{
			if (o != messages)
				throw new RuntimeException("Nieprawidłowy observable");
			refresh();
		}
	}

	final protected void refresh()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				UserNotificationsStack.Notification msg = messages.peek();

				if (msg == null)
				{
					setVisible(false);
					return;
				}

				titleLabel.setText(msg.title);
				contentPane.setText(msg.contents);
				setVisible(true);
				add(contentPane, BorderLayout.CENTER);
			}
		});
	}
}
