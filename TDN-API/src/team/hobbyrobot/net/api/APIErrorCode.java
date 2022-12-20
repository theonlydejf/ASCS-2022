package team.hobbyrobot.net.api;

public enum APIErrorCode
{
	SUCCESS(0),
	UNKNOWN_SERVICE(1),
	UNKNOWN_REQUEST(2),
	PARAMS_ERROR(3),
	GENERAL_EXCEPTION(4);
	
	private int intValue;
	
	private APIErrorCode(int val)
	{
		intValue = val;
	}
	
	public int getIntValue()
	{
		return intValue;
	}
}
