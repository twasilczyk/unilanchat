package protocols.ipmsg;

import java.nio.charset.Charset;

import tools.IP4Utilities;

/**
 * Pakiet przesyłany w protokole IPMsg. Jest to wersja częściowo
 * zdeserializowana (sekcja danych jest w formie tablicy bajtów).
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class IpmsgPacket
{
	/**
	 * Kodowanie, w którym przesyłane są wiadomości.
	 */
	protected final static Charset protocolCharset = Charset.forName("cp1250");

	/**
	 * Numer ostatnio wygenerowanego pakietu wychodzącego. W oryginalnej
	 * implementacji jest to unix timestamp.
	 */
	public static long selfPacketNo = Math.round(Math.random() * 100000);

	/**
	 * Numer kolejny pakietu.
	 */
	public final long packetNo;

	/**
	 * Sekcja danych w pakiecie.
	 */
	public String data = "";

	/**
	 * Powiązany adres IPv4. Jeżeli pakiet jest przychodzący - ip nadawcy.
	 * Jeżeli wychodzący - odbiorcy (null - broadcast).
	 */
	public String ip;

	/**
	 * Nazwa hosta nadawcy.
	 */
	public String hostName = "";

	/**
	 * Nazwa użytkownika zalogowanego na maszynie nadawcy (nie jest to nick).
	 */
	public String userName = "";

	/**
	 * Ustawione flagi poleceń wraz z identyfikatorem polecenia.
	 */
	protected long commandFlags = COMM_NOOP;

	/**
	 * Maska identyfikatora polecenia w polu polecenia.
	 */
	protected final static long commMask = 0x000000FF;

	/**
	 * Maska flag poleceń w polu polecenia.
	 */
	protected final static long optMask = 0xFFFFFF00;

	// <editor-fold defaultstate="collapsed" desc="Identyfikatory poleceń">

	/**
	 * Pusty pakiet (ignorowany).
	 */
	public final static int COMM_NOOP = 0x00000000;

		// <editor-fold defaultstate="collapsed" desc="Powiadamianie o obecności">

	/**
	 * Rozpoczęcie sesji lub odświeżenie listy. Przy rozpoczęciu sesji należy
	 * wysłać taki pakiet na adres broadcast.
	 */
	public final static int COMM_ENTRY = 0x00000001;

	/**
	 * Zakończenie sesji, skutkuje usunięciem z list obecności innych
	 * użytkowników. Należy wysłać na adres broadcast.
	 *
	 * W oryginalnej implementacji ipmsg, wysyłany dwa razy.
	 */
	public final static int COMM_EXIT = 0x00000002;

	/**
	 * Potwierdzenie odebrania pakietu COMM_ENTRY. Nasłuchiwanie tych pakietów
	 * jest wykorzystywane do uzupełnienia listy obecności nowych użytkowników.
	 */
	public final static int COMM_ANSENTRY = 0x00000003;

	/**
	 * Powiadomienie o zmianie statusu.
	 */
	public final static int COMM_ABSENCE = 0x00000004;

		// </editor-fold>

	public final static int COMM_BR_ISGETLIST = 0x00000010;
	public final static int COMM_OKGETLIST = 0x00000011;
	public final static int COMM_GETLIST = 0x00000012;
	public final static int COMM_ANSLIST = 0x00000013;
	public final static int COMM_BR_ISGETLIST2 = 0x00000018;

		// <editor-fold defaultstate="collapsed" desc="Przesyłanie wiadomości">

	/**
	 * Wysłanie wiadomości.
	 */
	public final static int COMM_SENDMSG = 0x00000020;

	/**
	 * Potwierdzennie odbioru wiadomości, dla pakietów z flagą FLAG_SENDCHECK.
	 */
	public final static int COMM_RECVMSG = 0x00000021;

	/**
	 * Potwierdzenie otworzenia zapieczętowanej wiadomości - flaga
	 * FLAG_READCHECK (ignorowany).
	 */
	public final static int COMM_READMSG = 0x00000030;

	/**
	 * Powiadomienie o odrzuceniu zapieczętowanej wiadomości - flaga
	 * FLAG_READCHECK (ignorowany).
	 */
	public final static int COMM_DELMSG = 0x00000031;

	/**
	 * Potwierdzenie przesłania powiadomienia COMM_READMSG lub COMM_DELMSG
	 * (ignorowany).
	 */
	public final static int COMM_ANSREADMSG = 0x00000032;

		// </editor-fold>

	public final static int COMM_GETINFO =			0x00000040;
	public final static int COMM_SENDINFO =			0x00000041;

	public final static int COMM_GETABSENCEINFO =	0x00000050;
	public final static int COMM_SENDABSENCEINFO =	0x00000051;

	public final static int COMM_GETFILEDATA =		0x00000060;
	public final static int COMM_RELEASEFILES =		0x00000061;
	public final static int COMM_GETDIRFILES =		0x00000062;

		// <editor-fold defaultstate="collapsed" desc="Wymiana kluczy RSA">

	/**
	 * Żądanie wysłania publicznego klucza RSA.
	 */
	public final static int COMM_GETPUBKEY = 0x00000072;

	/**
	 * Odpowiedź na COMM_GETPUBKEY.
	 */
	public final static int COMM_ANSPUBKEY = 0x00000073;

		// </editor-fold>

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Flagi poleceń">

		// <editor-fold desc="Powiadamianie o obecności">

	/**
	 * Powiadamianie o obecności, status: nieobecny.
	 */
	public final static long FLAG_ABSENCE = 0x00000100;

	/**
	 * Powiadamianie o obecności, (zarezerwowany).
	 */
	public final static long FLAG_SERVER = 0x00000200;

	/**
	 * Powiadamianie o obecności, klient ustawia tą flagę, jeżeli nie może
	 * odbierać wiadomości broadcast. Należy wysyłać do niego takie wiadomości
	 * indywidualnie.
	 *
	 * Nie jest pewne, czy ipmsg ma to zaimplementowane.
	 */
	public final static long FLAG_DIALUP = 0x00010000;

	public final static long FLAG_FILEATTACH = 0x00200000;

	public final static long FLAG_ENCRYPT = 0x00400000;

		// </editor-fold>

		// <editor-fold desc="Przesyłanie wiadomości">

	/**
	 * Oczekiwanie potwierdzenia (COMM_RECVMSG) przesłanej wiadomości. Jeżeli
	 * nie nadejdzie, wiadomość jest przesyłana ponownie.
	 */
	public final static long FLAG_SENDCHECK = 0x00000100;

	/**
	 * Wiadomość zapieczętowana (ignorowany).
	 */
	public final static long FLAG_SECRET = 0x00000200;

	/**
	 * Wiadomość zapieczętowana z potwierdzeniem przeczytania, używana razem
	 * z FLAG_SECRET (ignorowany).
	 */
	public final static long FLAG_READCHECK =	0x00100000;

	/**
	 * Wiadomość broadcast (do wszystkich). Na taką wiadomość nie są odsyłane
	 * potwierdzenia.
	 *
	 * Ipmsg domyślnie stosuje tą flagę przy wiadomościach do wielu
	 * (niekoniecznie wszystkich) osób.
	 */
	public final static long FLAG_BROADCAST = 0x00000400;

	/**
	 * Wiadomość multicast (do wybranej grupy). Na taką wiadomość są odsyłane
	 * potwierdzenia.
	 */
	public final static long FLAG_MULTICAST = 0x00000800;

	/**
	 * Wiadomość multicast dla nowych wersji protokołu (zarezerwowana).
	 */
	public final static long FLAG_MULTICAST_NEW = 0x00040000;

	/**
	 * Flaga nieprawidłowa w wersji protokołu Draft-9. Ignorowany.
	 */
	public final static long FLAG_NOPOPUP =		0x00001000;

	/**
	 * Flaga oznaczająca, że odpowiedź jest automatyczna. Jest to zabezpieczenie
	 * przed efektem "ping-pong", między dwoma osobami z ustawioną automatyczną
	 * informacją o nieobecności.
	 */
	public final static long FLAG_AUTORET =		0x00002000;
	
	public final static long FLAG_RETRY =		0x00004000;

	/**
	 * Flaga zapieczętowania i zabezpieczenia hasłem (ignorowany).
	 *
	 * W oryginalnej implementacji ipmsg użytkownik przed odpieczętowaniem
	 * (i przeczytaniem wiadomości) musi podać swoje hasło, ustawione wcześniej
	 * w programie (domyślnie puste).
	 */
	public final static long FLAG_PASSWORD =	0x00008000;

	/**
	 * Flaga sugerująca, aby nie zapisywać danej wiadomości w logu (ignorowany).
	 */
	public final static long FLAG_NOLOG =		0x00020000;

	public final static long FLAG_NOADDLIST =	0x00080000;

		// </editor-fold>

	// </editor-fold>

	/**
	 * Konstruktor pakietów wysyłanych.
	 *
	 * @param userName nazwa zalogowanego użytkownika
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
	 * Konstruktor pakietów odbieranych.
	 *
	 * @param packetNo numer porządkowy pakietu (mogą być takie same dla
	 * różnych nadawców)
	 */
	public IpmsgPacket(long packetNo)
	{
		this.packetNo = packetNo;
	}

	// <editor-fold defaultstate="collapsed" desc="Serializacja">

	/**
	 * Serializacja - zwraca dane gotowe do wysłania.
	 *
	 * @return zserializowany pakiet
	 */
	public byte[] getRAWData()
	{
		return ("1:" + //numer protokołu
				Long.toString(packetNo) + ":" +
				userName + ":" +
				hostName + ":" +
				Long.toString(commandFlags) + ":" +
				data + '\0').getBytes(protocolCharset);
	}

	/**
	 * Deserializacja - zwraca pakiet odpowiadający danym binarnym.
	 *
	 * @param raw Bufor z danymi binarnymi
	 * @param length Długość danych binarnych w buforze
	 * @return Zdeserializowany pakiet
	 */
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

	/**
	 * Zamienia zawartość pakietu na formę gotową do wyświetlenia, np. do
	 * debugowania.
	 *
	 * @return Tekstowa forma pakietu
	 */
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

	/**
	 * Ustawia identyfikator polecenia.
	 *
	 * @param command identyfikator polecenia
	 */
	public void setCommand(int command)
	{
		commandFlags &= optMask;
		commandFlags |= command & commMask;
	}

	/**
	 * Zwraca identyfikator polecenia.
	 *
	 * @return identyfikator polecenia
	 */
	public int getCommand()
	{
		return (int)(commandFlags & commMask);
	}

	/**
	 * Ustawia wybraną flagę polecenia.
	 *
	 * @param flag flaga do ustawienia
	 * @param set czy ustawić (czy wyczyścić) flagę
	 */
	public void setFlag(long flag, boolean set)
	{
		flag &= optMask;
		if (set)
			commandFlags |= flag;
		else
			commandFlags &= ~flag;
	}

	/**
	 * Zwraca wybraną flagę polecenia.
	 *
	 * @param flag flaga do pobrania
	 * @return stan wybranej flagi
	 */
	public boolean getFlag(long flag)
	{
		return (commandFlags & (flag & optMask)) != 0;
	}
}
