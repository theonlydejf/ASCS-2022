package team.hobbyrobot.net.api.exceptions;

public class UnknownRequestException extends Exception
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1734715876990656911L;
	
	public UnknownRequestException()
	{
		super();
	}
	
	public UnknownRequestException(String message)
	{
		super(message);
	}
}
