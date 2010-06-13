package views.swing;

import java.awt.*;
import javax.swing.*;

import controllers.FileTransfersController;
import protocols.Account;
import protocols.ipmsg.*;
import resources.ResourceManager;
import tools.*;

/**
 * Widok odpowiedzialny za listę (okno) przesyłanych (wysyłanych i odbieranych)
 * plików.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class FileTransfersView extends JFrame
{
	public final FileTransfersController fileTransfersController;

	protected final ListenableVector<IpmsgTransferredFile> ipmsgTransferredFiles;

	private final FileTransfersListModel fileTransfersListModel;

	public FileTransfersView(FileTransfersController fileTransfersController)
	{
		super("Transfery plików");
		setVisible(false);

		this.fileTransfersController = fileTransfersController;

		ListenableVector<IpmsgTransferredFile> ipmsgTransferredFilesInit = null;
		for (Account acc : fileTransfersController.getMainController().getAccountsVector())
		{
			if (!(acc instanceof IpmsgAccount))
				continue;
			IpmsgAccount ipmsgAcc = (IpmsgAccount)acc;
			ipmsgTransferredFilesInit = ipmsgAcc.transferredFiles;
			break;
		}
		if (ipmsgTransferredFilesInit == null)
			throw new NullPointerException();
		this.ipmsgTransferredFiles = ipmsgTransferredFilesInit;
		this.fileTransfersListModel = new FileTransfersListModel();

		setMinimumSize(new Dimension(200, 100));
		setPreferredSize(new Dimension(400, 100));
		setLayout(new BorderLayout(10, 10));
		setIconImage(ResourceManager.getIcon("icons/32.png").getImage());

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		mainPanel.add(new FileTransfersListPanel(), BorderLayout.CENTER);
		add(mainPanel, BorderLayout.CENTER);

		pack();
		setLocationRelativeTo(null); //wyśrodkowanie okna
	}

	public void showTransfers()
	{
		if (getState() == Frame.ICONIFIED)
			setState(Frame.NORMAL);
		setVisible(true);
		requestFocus();
	}

	class FileTransfersListPanel extends JScrollPane
	{
		public FileTransfersListPanel()
		{
			this.setViewportView(new FileTransfersList());
			this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			this.setBorder(BorderFactory.createEmptyBorder());
		}
	}

	class FileTransfersList extends JList
	{
		public FileTransfersList()
		{
			this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.setModel(fileTransfersListModel);
//			this.setCellRenderer(new FileTransfersListPanelRenderer());
//			this.addMouseListener(new FileTransfersListListener());
		}
	}

	public class FileTransfersListModel extends AbstractListModel
	{
		public FileTransfersListModel()
		{
			ipmsgTransferredFiles.addSetListener(new FileTransfersSetListener());
		}

		public int getSize()
		{
			return ipmsgTransferredFiles.size();
		}

		public Object getElementAt(int index)
		{
			return ipmsgTransferredFiles.get(index);
		}

		protected void refreshList()
		{
			final FileTransfersListModel model = this;
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					fireContentsChanged(model, 0, Integer.MAX_VALUE);
				}
			});
		}
	}

	public class FileTransfersSetListener implements SetListener<IpmsgTransferredFile>
	{
		public void itemAdded(IpmsgTransferredFile item)
		{
			fileTransfersListModel.refreshList();
		}

		public void itemRemoved(IpmsgTransferredFile item)
		{
			fileTransfersListModel.refreshList();
		}

		public void itemUpdated(IpmsgTransferredFile item)
		{
			fileTransfersListModel.refreshList();
		}
	}
}
