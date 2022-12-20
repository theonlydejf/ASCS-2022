package team.hobbyrobot.net.api;

import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.*;

// TODO: dodelat
class ResponseFactory
{	
	public static TDNRoot createExceptionResponse(APIErrorCode errorCode, String details)
	{
		return createRawResponse(errorCode.getIntValue(), new TDNRoot().insertValue(TDNAPIServer.ERROR_DETAILS_KEYWORD, new TDNValue(details, TDNParsers.STRING)));
	}
	
	public static TDNRoot createRawResponse(int errorCode, TDNRoot data)
	{
		return new TDNRoot().insertValue(TDNAPIServer.ERROR_CODE_KEYWORD, new TDNValue(errorCode, TDNParsers.INTEGER))
			.insertValue(TDNAPIServer.DATA_KEYWORD, new TDNValue(data, TDNParsers.ROOT));
	}
	
	public static TDNRoot createSuccessResponse(TDNRoot data)
	{
		if(data == null)
			return null;
		return createRawResponse(APIErrorCode.SUCCESS.getIntValue(), data);
	}
}
