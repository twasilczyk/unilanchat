package views.swing;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

import components.swing.JImagePanel;
import java.awt.event.ActionListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import resources.ResourceManager;
import tools.html.HTMLUtilities;
import tools.html.HyperlinkHighlighter;

/**
 * Widok okna informacji o programie.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class AboutView extends JFrame
{
	final JPanel wholePanel = new JPanel();
	final AboutViewListener aboutViewListener = new AboutViewListener();

	public AboutView()
	{
		super("O programie " + main.Main.applicationName);
		setVisible(false);

		wholePanel.setLayout(new BorderLayout(0, 10));
		wholePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		setMinimumSize(new Dimension(350, 200));
		setPreferredSize(new Dimension(350, 300));
		setLayout(new BorderLayout());
		setIconImage(ResourceManager.getIcon("icon.png").getImage());

		wholePanel.add(new TitleBar(), BorderLayout.NORTH);

		JScrollPane contentScrollPane = new JScrollPane(new ContentPanel());
		contentScrollPane.setBorder(BorderFactory.createEmptyBorder());
		wholePanel.add(contentScrollPane, BorderLayout.CENTER);

		wholePanel.add(new CloseButtonPanel(), BorderLayout.SOUTH);

		add(wholePanel, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(null); //wyśrodkowanie okna
	}

	public void showAbout()
	{
		if (getState() == Frame.ICONIFIED)
			setState(Frame.NORMAL);
		setVisible(true);
		requestFocus();
	}

	class AboutViewListener implements HyperlinkListener, ActionListener
	{
		public void hyperlinkUpdate(HyperlinkEvent e)
		{
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
				HTMLUtilities.openURL(e.getDescription());
		}

		public void actionPerformed(ActionEvent e)
		{
			if (e.getActionCommand().equals("close"))
				setVisible(false);
			else
				assert(false);
		}
	}

	class TitleBar extends JPanel
	{
		public TitleBar()
		{
			setLayout(new BorderLayout(10, 0));

			JImagePanel logo = new JImagePanel(ResourceManager.getImage("icon.png"));
			logo.setPreferredSize(new Dimension(32, 32));
			add(logo, BorderLayout.WEST);

			JLabel title = new JLabel(main.Main.applicationFullName);
			add(title, BorderLayout.CENTER);
			title.setFont(new Font("Arial", Font.BOLD, 20));
		}
	}

	class CloseButtonPanel extends JPanel
	{
		public CloseButtonPanel()
		{
			setLayout(new BorderLayout());

			JButton closeButton = new JButton("Zamknij");
			closeButton.setActionCommand("close");
			closeButton.addActionListener(aboutViewListener);

			add(closeButton, BorderLayout.EAST);
		}
	}

	class ContentPanel extends JEditorPane
	{
		public ContentPanel()
		{
			setBackground(Color.WHITE);
			setEditable(false);

			try
			{
				setPage(ResourceManager.get("about.html"));
			}
			catch (IOException ex)
			{
				throw new RuntimeException("Błąd wczytywania strony o programie", ex);
			}

			new HyperlinkHighlighter(this);
			addHyperlinkListener(aboutViewListener);
		}
	}
}
