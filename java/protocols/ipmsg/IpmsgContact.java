package protocols.ipmsg;

import protocols.*;

/**
 * Kontakt użytkownika korzystającego z protokołu IPMsg
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public class IpmsgContact extends Contact
{
	protected final IpmsgAccount account;
	protected final String ip;
	protected String name = "", group = "";
	protected String textStatus = "";

	protected UserStatus status = UserStatus.OFFLINE;

	public IpmsgContact(IpmsgAccount account, String ip)
	{
		if (account == null || ip == null)
			throw new NullPointerException();
		this.account = account;
		this.ip = ip;
	}

	public String getID()
	{
		return getID(ip);
	}

	public static String getID(String ip)
	{
		return ip + "@ipmsg";
	}

	/**
	 * TODO: przechowujmy ip, a nie id!
	 *
	 * @return ip usera
	 */
	public String getIP()
	{
		return ip;
	}

	public IpmsgAccount getAccount()
	{
		return account;
	}

	public String getName()
	{
		return name;
	}

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

	public String getGroup()
	{
		return group;
	}

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

	public UserStatus getStatus()
	{
		return status;
	}

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
