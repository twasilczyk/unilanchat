package protocols.ipmsg;

import java.nio.charset.Charset;

import tools.IP4Utilities;

/**
 * Pakiet przesyłany w protokole IPMsg. Jest to wersja częściowo
 * zdeserializowana (sekcja danych w formie tablicy bajtów)
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class IpmsgPacket
{
	protected final static Charset protocolCharset = Charset.forName("cp1250");

	public static long selfPacketNo = Math.round(Math.random() * 100000); //zaczynamy od unix timestamp

	public final long packetNo;
	public String data = "";

	/**
	 * ip - IPv4, jeżeli null -- broadcast
	 */
	public String ip;

	/**
	 * userName dotyczy zalogowanego użytkownika (nie jest to nick!)
	 */
	public String hostName = "", userName = "";

	protected long commandFlags = 0;

	protected final static long commMask = 0x000000FF;
	protected final static long optMask = 0xFFFFFF00;

	// <editor-fold defaultstate="collapsed" desc="Identyfikatory poleceń">

	public final static int COMM_NOOP =				0x00000000; // ignorowany

	public final static int COMM_ENTRY =			0x00000001;
	public final static int COMM_EXIT =				0x00000002;
	public final static int COMM_ANSENTRY =			0x00000003;
	public final static int COMM_ABSENCE =			0x00000004;

	public final static int COMM_BR_ISGETLIST =		0x00000010; // ?
	public final static int COMM_OKGETLIST =		0x00000011; // ?
	public final static int COMM_GETLIST =			0x00000012; // ?
	public final static int COMM_ANSLIST =			0x00000013; // ?
	public final static int COMM_BR_ISGETLIST2 =	0x00000018; // ?

	public final static int COMM_SENDMSG =			0x00000020;
	public final static int COMM_RECVMSG =			0x00000021;
	public final static int COMM_READMSG =			0x00000030; // ignorowany (odpowiedź na pieczętowanie)
	public final static int COMM_DELMSG =			0x00000031; // ignorowany (prawdopodobnie odpowiedź na COMM_READMSG)
	public final static int COMM_ANSREADMSG =		0x00000032; // ignorowany (odpowiedź na COMM_READMSG)

	public final static int COMM_GETINFO =			0x00000040; // ?
	public final static int COMM_SENDINFO =			0x00000041; // ?

	public final static int COMM_GETABSENCEINFO =	0x00000050; // ?
	public final static int COMM_SENDABSENCEINFO =	0x00000051; // ?

	public final static int COMM_GETFILEDATA =		0x00000060; // ?
	public final static int COMM_RELEASEFILES =		0x00000061; // ?
	public final static int COMM_GETDIRFILES =		0x00000062; // ?

	public final static int COMM_GETPUBKEY =		0x00000072; // w tej chwili nie wspierane
	public final static int COMM_ANSPUBKEY =		0x00000073; // w tej chwili nie wspierane

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Flagi poleceń">

	// powiadamianie o obecności
	public final static long FLAG_ABSENCE =		0x00000100;
	public final static long FLAG_SERVER =		0x00000200; // ?
	public final static long FLAG_DIALUP =		0x00010000; // ?
	public final static long FLAG_FILEATTACH =	0x00200000; // ?
	public final static long FLAG_ENCRYPT =		0x00400000;

	// przesyłanie wiadomości
	public final static long FLAG_SENDCHECK =	0x00000100;
	public final static long FLAG_SECRET =		0x00000200; // ignorowany (pieczętowanie)
	public final static long FLAG_BROADCAST =	0x00000400;
	public final static long FLAG_MULTICAST =	0x00000800;
	public final static long FLAG_NOPOPUP =		0x00001000; // ignorowany
	public final static long FLAG_AUTORET =		0x00002000; // ?
	public final static long FLAG_RETRY =		0x00004000; // ?
	public final static long FLAG_PASSWORD =	0x00008000; // ignorowany (pieczętowanie + hasło)
	public final static long FLAG_NOLOG =		0x00020000; // ignorowany
	public final static long FLAG_NEWMUTI =		0x00040000; // ?
	public final static long FLAG_NOADDLIST =	0x00080000; // ?
	public final static long FLAG_READCHECK =	0x00100000; // ignorowany (pieczętowanie)

	// </editor-fold>

	/**
	 * Konstruktor pakietów wysyłanych
	 *
	 * @param userName Nazwa zalogowanego użytkownika
	 */
	public IpmsgPacket(String userName)
	{
		if (userName == null)
			throw new NullPointerException();
		
		this.packetNo = ++selfPacketNo;
		this.hostName = IP4Utilities.getLocalHostName();
		this.userName = userName;
	}

	/**
	 * Konstruktor pakietów odbieranych
	 *
	 * @param packetNo Numer porządkowy pakietu (mogą być takie same dla
	 * różnych nadawców)
	 */
	public IpmsgPacket(long packetNo)
	{
		this.packetNo = packetNo;
	}

	// <editor-fold defaultstate="collapsed" desc="Serializacja">

	public byte[] getRAWData()
	{
		return ("1:" + //numer protokołu
				Long.toString(packetNo) + ":" +
				userName + ":" +
				hostName + ":" +
				Long.toString(commandFlags) + ":" +
				data + '\0').getBytes(protocolCharset);
	}

	public static IpmsgPacket fromRAWData(byte[] raw, int length)
	{
		String[] data = (new String(raw, 0, length, protocolCharset)).split(":", 6);
		if (data.length != 6)
			throw new IllegalArgumentException("Nieprawidłowa ilość sekcji");
		if (!data[0].equals("1"))
			throw new IllegalArgumentException("Nieznana wersja protokołu");

		IpmsgPacket packet;
		try
		{
			packet = new IpmsgPacket(Long.parseLong(data[1]));
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Nieprawidłowy numer pakietu");
		}

		packet.userName = data[2];
		packet.hostName = data[3];
		try
		{
			packet.commandFlags = Long.parseLong(data[4]);
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Nieprawidłowy numer polecenia");
		}

		if (data[5].length() > 0 && data[5].charAt(data[5].length() - 1) == '\0')
			packet.data = data[5].substring(0, data[5].length() - 1);
		else
			packet.data = data[5];

		return packet;
	}

	@Override public String toString()
	{
		return "IpmsgPacket[" +
			"id: " + packetNo + ", " +
			"nadawca: \"" + userName + "/" + hostName + "\", " +
			"ip: " + ip + ", " +
			"polecenie: " + getCommand() + ", " +
			"flagi: " + Long.toString(commandFlags & optMask, 16) + ", " +
			"dane: \"" + data + "\"]";
	}

	// </editor-fold>

	public void setCommand(int command)
	{
		commandFlags &= optMask;
		commandFlags |= command & commMask;
	}

	public int getCommand()
	{
		return (int)(commandFlags & commMask);
	}

	public void setFlag(long flag, boolean set)
	{
		flag &= optMask;
		if (set)
			commandFlags |= flag;
		else
			commandFlags &= ~flag;
	}

	public boolean getFlag(long flag)
	{
		return (commandFlags & (flag & optMask)) != 0;
	}
}
