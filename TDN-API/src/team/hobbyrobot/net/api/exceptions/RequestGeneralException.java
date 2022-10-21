package team.hobbyrobot.net.api.exceptions;

public class RequestGeneralException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1789808304160236804L;
	
	public String details;
	
	public RequestGeneralException(String details)
	{
		super(details);
		this.details = details;
	}
}
