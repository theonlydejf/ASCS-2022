package team.hobbyrobot.net.api;

enum ErrorCode
{
	SUCCESS(0),
	UNKNOWN_SERVICE(1),
	UNKNOWN_REQUEST(2),
	PARAMS_ERROR(3),
	GENERAL_EXCEPTION(4);
	
	private int intValue;
	
	ErrorCode(int val)
	{
		intValue = val;
	}
	
	int getIntValue()
	{
		return intValue;
	}
}
