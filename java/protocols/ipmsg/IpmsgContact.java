package protocols.ipmsg;

import protocols.*;

/**
 * Kontakt użytkownika korzystającego z protokołu IPMsg.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class IpmsgContact extends Contact
{
	/**
	 * Adres IPv4 kontaktu.
	 */
	protected final String ip;

	/**
	 * Nazwa kontaktu.
	 */
	protected String name = "";

	/**
	 * Nazwa grupy kontaktu.
	 */
	protected String group = "";

	/**
	 * Status opisowy kontaktu.
	 */
	protected String textStatus = "";

	/**
	 * Status kontaktu.
	 */
	protected UserStatus status = UserStatus.OFFLINE;

	/**
	 * Główny konstruktor.
	 *
	 * @param account konto, z którym ma być powiązany kontakt
	 * @param ip adres IPv4 kontaktu
	 */
	public IpmsgContact(IpmsgAccount account, String ip)
	{
		super(account);
		if (account == null || ip == null)
			throw new NullPointerException();
		this.ip = ip;
	}

	public String getID()
	{
		return getID(ip);
	}

	/**
	 * Pobiera unikalny idektyfikator kontaktu o podanym adresie IPv4.
	 *
	 * @param ip adres kontaktu
	 * @return identyfikator
	 */
	public static String getID(String ip)
	{
		return ip + "@ipmsg";
	}

	/**
	 * Pobiera adres IPv4 kontaktu.
	 *
	 * @return ip kontaktu
	 */
	public String getIP()
	{
		return ip;
	}

	public IpmsgAccount getAccount()
	{
		return (IpmsgAccount)account;
	}

	public String getName()
	{
		return name;
	}

	/**
	 * Ustawia nazwę kontaktu.
	 *
	 * @param name nazwa kontaktu
	 */
	public void setName(String name)
	{
		if (name == null)
			throw new NullPointerException();
		name = name.trim();
		if (this.name.equals(name))
			return;
		this.name = name.trim();
		this.notifyObservers();
	}

	/**
	 * Zwraca grupę, do której jest zapisany użytkownik (kontakt). 
	 *
	 * @return grupa kontaktu
	 */
	public String getGroup()
	{
		return group;
	}

	/**
	 * Ustawia nazwę grupy kontaktu.
	 *
	 * @param name grupa kontaktu
	 */
	public void setGroup(String name)
	{
		if (name == null)
			throw new NullPointerException();
		name = name.trim();
		if (this.group.equals(name))
			return;
		this.group = name.trim();
		this.notifyObservers();
	}

	/**
	 * Pobiera status kontaktu.
	 *
	 * @return status kontaktu
	 */
	public UserStatus getStatus()
	{
		return status;
	}

	/**
	 * Ustawia status kontaktu.
	 *
	 * @param status status kontaktu
	 */
	public void setStatus(UserStatus status)
	{
		if (status == null)
			throw new NullPointerException();
		if (this.status.equals(status))
			return;

		this.status = status;
		this.notifyObservers();
	}

	public String getTextStatus()
	{
		return textStatus.isEmpty()?null:textStatus;
	}

	/**
	 * Ustawia status opisowy kontaktu.
	 *
	 * @param status status opisowy
	 */
	public void setTextStatus(String status)
	{
		if (status == null)
			throw new NullPointerException();
		status = status.trim();
		if (textStatus.equals(status))
			return;
		textStatus = status;
		this.notifyObservers();
	}
}
