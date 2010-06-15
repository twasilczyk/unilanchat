package views.swing;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;

import components.swing.JStickyScrollPane;
import java.util.Vector;
import javax.swing.filechooser.FileSystemView;
import protocols.*;
import tools.GUIUtilities;
import tools.SetListener;

/**
 * Panel pokoju rozmów, zawierający listę wiadomości oraz pole do ich wysyłania.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class ChatRoomPanel extends JPanel implements SetListener<Message>
{
	protected final ChatRoom chatRoom;
	protected final ChatRoomsView chatRoomsView;

	protected final MessagesPanel messagesPanel;
	
	protected final JTextPane inputPane = new JTextPane();

	protected final JPanel inputBoxPanel = new JPanel(new BorderLayout(0, 2));
	protected final JScrollPane inputScrollPane = new JScrollPane(inputPane);
	private final AttachedFileList attachedFileList = new AttachedFileList();

	protected final static int minInputHeight = 25;
	protected final static int maxInputHeight = 100;

	protected boolean unread = false;

	public ChatRoomPanel(ChatRoom chatRoom, ChatRoomsView chatRoomsView)
	{
		this.chatRoomsView = chatRoomsView;
		this.chatRoom = chatRoom;
		messagesPanel = new MessagesPanel(chatRoomsView);
		chatRoom.getMessagesVector().addSetListener(this);

		this.setLayout(new BorderLayout(0, 4));
		this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 0));

		this.add(messagesPanel, BorderLayout.CENTER);
		this.add(inputBoxPanel, BorderLayout.SOUTH);

		inputBoxPanel.add(attachedFileList, BorderLayout.NORTH);
		inputBoxPanel.add(inputScrollPane, BorderLayout.SOUTH);

		inputPane.setBackground(Color.WHITE);
		inputPane.addKeyListener(new InputPaneListener());
		inputScrollPane.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		inputScrollPane.setPreferredSize(new Dimension(50, minInputHeight));
		inputScrollPane.setBorder(BorderFactory.createEmptyBorder());

		messagesPanel.addKeyListener(new MessagesPanelListener());
	}

	public void showFileAttachDialog()
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Wybierz pliki do wysłania");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setFileHidingEnabled(false);
		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;

		attachedFileList.addFiles(fileChooser.getSelectedFiles());
	}

	class InputPaneListener implements KeyListener
	{
		protected final Dimension scrollPaneDimension = new Dimension();
		protected int currHeight = minInputHeight;

		protected void setScrollPaneHeight(int height)
		{
			currHeight = height;
			scrollPaneDimension.width = inputScrollPane.getWidth();
			scrollPaneDimension.height = height;
			inputScrollPane.setPreferredSize(scrollPaneDimension);
			chatRoomsView.validate();
		}

		public void keyPressed(KeyEvent e)
		{
			if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
			{
				chatRoomsView.setVisible(false);
				return;
			}
			if (e.getKeyChar() == KeyEvent.VK_ENTER)
			{
				if (e.isShiftDown())
					inputPane.replaceSelection("\n");
				else
				{
					String sendText;
					synchronized (inputPane)
					{
						sendText = inputPane.getText().trim();
						inputPane.setText("");
					}

					if (!sendText.isEmpty() || !attachedFileList.isEmpty())
					{
						if (attachedFileList.isEmpty())
							chatRoomsView.chatController.sendMessage(chatRoom, sendText, null);
						else
						{
							chatRoomsView.chatController.sendMessage(chatRoom, sendText, attachedFileList.getFileList());
							attachedFileList.cleanFileList();
						}
					}
					setScrollPaneHeight(minInputHeight);
				}
			}
		}

		public void keyTyped(KeyEvent e)
		{
			// należy ponownie wyczyścić pole edycji, ponieważ dodał się do
			// niego enter, naciskany przy wysyłaniu
			if (e.getKeyChar() == KeyEvent.VK_ENTER && !e.isShiftDown())
				inputPane.setText("");
		}

		public void keyReleased(KeyEvent e)
		{
			inputPane.validate();
			if (currHeight < inputPane.getHeight() - 5)
			{
				int newHeight = inputPane.getHeight() + 5;
				if (newHeight < minInputHeight)
					newHeight = minInputHeight;
				else if (newHeight > maxInputHeight)
					newHeight = maxInputHeight;
				setScrollPaneHeight(newHeight);
			}
		}
	}

	class MessagesPanelListener implements KeyListener
	{

		public void keyTyped(KeyEvent e)
		{
			if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
			{
				chatRoomsView.setVisible(false);
				return;
			}
			if ((e.getKeyChar() >= 32 || e.getKeyChar() == 10) && !inputPane.hasFocus())
			{
				inputPane.requestFocus();
				if (e.getKeyChar() >= 32)
					inputPane.replaceSelection(String.valueOf(e.getKeyChar()));
			}
		}

		public void keyPressed(KeyEvent e) { }

		public void keyReleased(KeyEvent e) { }

	}

	public ChatRoom getChatRoom()
	{
		return chatRoom;
	}

	public void itemAdded(Message item)
	{
		messagesPanel.add(item);
		if (!isFocusedTab())
			setUnread(true);
	}

	public void itemRemoved(Message item)
	{
		throw new UnsupportedOperationException("Nie można usuwać wiadomości");
	}

	public void itemUpdated(Message item)
	{
		messagesPanel.update(item);
	}

	public boolean isFocusedTab()
	{
		if (!this.chatRoomsView.isFocused())
			return false;
		return (this == ((ChatTabs)this.getParent()).getSelectedRoom());
	}

	public synchronized void setUnread(boolean unread)
	{
		if (this.unread == unread)
			return;
		this.unread = unread;
		chatRoomsView.setUnread(this.chatRoom, unread);
	}

	public boolean isUnread()
	{
		return unread;
	}

	public void focusInput()
	{
		inputPane.requestFocusInWindow();
	}
}

class AttachedFileList extends JStickyScrollPane
{
	final JPanel listPanel = new JPanel(new GridLayout(0, 1));

	Vector<File> files = new Vector<File>();

	private final static FileSystemView fsView = FileSystemView.getFileSystemView();

	private static Dimension fileElementDimension = new Dimension(0, 24);
	private static Font removeButtonFont = new Font("Arial", Font.PLAIN, 10);

	public AttachedFileList()
	{
		setViewportView(listPanel);
		setVisible(false);
		setBorder(BorderFactory.createEmptyBorder());
	}

	private void updateHeight()
	{
		if (files.isEmpty())
		{
			if (isVisible())
			{
				setVisible(false);
				GUIUtilities.validateRoot(this);
			}
			return;
		}
		
		if (!isVisible())
		{
			setVisible(true);
			GUIUtilities.validateRoot(this);
		}

		int currHeight = files.size() * fileElementDimension.height + 5;

		Dimension currSize = new Dimension();
		currSize.height = Math.min(currHeight, 150);
		setPreferredSize(currSize);
		invalidate();

		GUIUtilities.validateRoot(this);
	}

	public void addFiles(File[] addedFiles)
	{
		synchronized (this)
		{
			for (File f : addedFiles)
			{
				if (!f.exists())
					continue;
				if (files.contains(f))
					continue;

				FileElement fe = new FileElement(f);
				listPanel.add(fe);
				files.add(f);
			}
		}

		updateHeight();
	}

	protected void delFile(File file)
	{
		synchronized (this)
		{
			if (!files.contains(file))
				return;
			files.remove(file);
			for (Component c : listPanel.getComponents())
			{
				FileElement fe = (FileElement)c;
				if (fe.file.equals(file))
					listPanel.remove(fe);
			}
		}

		updateHeight();
	}

	public boolean isEmpty()
	{
		return files.isEmpty();
	}

	public synchronized File[] getFileList()
	{
		File[] ret = new File[files.size()];
		return files.toArray(ret);
	}

	public synchronized void cleanFileList()
	{
		listPanel.removeAll();
		files.removeAllElements();
		updateHeight();
	}

	class FileElement extends JPanel
	{
		public final File file;

		public FileElement(File fileP)
		{
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 10));
			this.file = fileP;
			setPreferredSize(fileElementDimension);
			setMaximumSize(fileElementDimension);

			JButton removeButton = new JButton("x");
			removeButton.setFont(removeButtonFont);
			removeButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					delFile(file);
				}
			});

			JLabel fileNameLabel = new JLabel(file.getName(), fsView.getSystemIcon(file), SwingConstants.LEFT);
			fileNameLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

			add(fileNameLabel, BorderLayout.WEST);
			add(removeButton, BorderLayout.CENTER);
		}
	}
}
