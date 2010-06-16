package views.swing;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

import controllers.FileTransfersController;
import protocols.*;
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

	private final FileTransfersTableModel fileTransfersTableModel;

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
		this.fileTransfersTableModel = new FileTransfersTableModel();

		setMinimumSize(new Dimension(300, 100));
		setPreferredSize(new Dimension(600, 250));
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

	class FileTransfersList extends JTable
	{
		public FileTransfersList()
		{
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setModel(fileTransfersTableModel);
			setDefaultRenderer(FileTransfersCellRendererProgress.class,
					new FileTransfersCellRendererProgress());
			setRowMargin(2);
			setBackground(Color.WHITE);
			setShowGrid(false);
			setOpaque(false);
		}
	}

	class FileTransfersTableModel extends AbstractTableModel
	{
		public FileTransfersTableModel()
		{
			ipmsgTransferredFiles.addSetListener(new FileTransfersSetListener());
		}

		public int getRowCount()
		{
			return ipmsgTransferredFiles.size();
		}

		public int getColumnCount()
		{
			return 6;
		}

		@Override
		public String getColumnName(int column)
		{
			switch (column)
			{
				case 0:
					return "Kierunek";
				case 1:
					return "Nazwa pliku";
				case 2:
					return "Kontakt";
				case 3:
					return "Postęp";
				case 4:
					return "Transfer";
				case 5:
					return "Status";
				default:
					throw new UnsupportedOperationException("Nieprawidłowy numer kolumny");
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			if (columnIndex == 3)
				return FileTransfersCellRendererProgress.class;
			return super.getColumnClass(columnIndex);
		}

		protected final String[] speedSuffixes = {
			"B/s", "kB/s", "MB/s", "GB/s", "TB/s"
		};

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			if (columnIndex == 0)
			{
				if (ipmsgTransferredFiles.get(rowIndex) instanceof ReceivedFile)
					return "Pobieranie";
				else
					return "Wysyłanie";
			}
			else if (columnIndex == 1)
				return ipmsgTransferredFiles.get(rowIndex).getFileName();
			else if (columnIndex == 2)
				return ipmsgTransferredFiles.get(rowIndex).getContact().getName();
			else if (columnIndex == 3)
			{
				IpmsgTransferredFile file = ipmsgTransferredFiles.get(rowIndex);
				long current = file.getTransferredDataSize();
				Long total = file.getFileSize();
				if (total == null || total <= 0)
					return 0.0;
				else if (total <= current)
					return 1.0;
				else
					return ((double)current)/((double)total);
			}
			else if (columnIndex == 4)
			{
				double speed = ipmsgTransferredFiles.get(rowIndex).getTransferSpeed();
				int speedSuffix = 0;
				while (speed >= 1000 && speedSuffix < speedSuffixes.length - 1)
				{
					speed /= 1000;
					speedSuffix++;
				}

				if (speed >= 100)
					return String.format("%.0f %s", speed, speedSuffixes[speedSuffix]);
				else if (speed >= 10)
					return String.format("%.1f %s", speed, speedSuffixes[speedSuffix]);
				else
					return String.format("%.2f %s", speed, speedSuffixes[speedSuffix]);
			}
			else if (columnIndex == 5)
			{
				switch (ipmsgTransferredFiles.get(rowIndex).getState())
				{
					case COMPLETED:
						return "Ukończono";
					case ERROR:
						return "Błąd";
					case TRANSFERRING:
						return "Przesyłanie";
					case WAITING_FOR_CONNECTION:
						return "Oczekuje";
					default:
						throw new UnsupportedOperationException("Nieznany status");
				}
			}
			else
				throw new UnsupportedOperationException("Nieprawidłowy numer kolumny");
		}

		public void refreshData()
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					fireTableDataChanged();
				}
			});
		}
	}

	class FileTransfersCellRendererProgress implements TableCellRenderer
	{
		protected final JProgressBar progressBar = new JProgressBar(0, 100);

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			Double progress = (Double)value;
			progressBar.setValue((int)Math.round(progress * 100));
			return progressBar;
		}
	}

	class FileTransfersSetListener implements SetListener<IpmsgTransferredFile>
	{
		public void itemAdded(IpmsgTransferredFile item)
		{
			fileTransfersTableModel.refreshData();
		}

		public void itemRemoved(IpmsgTransferredFile item)
		{
			fileTransfersTableModel.refreshData();
		}

		public void itemUpdated(IpmsgTransferredFile item)
		{
			fileTransfersTableModel.refreshData();
		}
	}
}
