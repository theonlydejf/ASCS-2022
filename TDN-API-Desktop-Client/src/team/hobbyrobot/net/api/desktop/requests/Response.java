package team.hobbyrobot.net.api.desktop.requests;

import java.io.IOException;

import team.hobbyrobot.net.api.APIErrorCode;
import team.hobbyrobot.net.api.TDNAPIServer;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class Response
{
	private TDNRoot _raw;
	private boolean _success;
	private String _errorDetails;
	private TDNRoot _data;
	private TDNValue _took;
	private TDNValue _parseTook;
	private TDNValue _responseTook;

	public Response(TDNRoot response)
	{
		_raw = response;
		TDNValue errorCodeTDN = _raw.get(TDNAPIServer.ERROR_CODE_KEYWORD);
		if (errorCodeTDN == null)
			throw new RuntimeException(
				"Something went terribely wrong, cuz there's no error code in the api response :o");

		_success = errorCodeTDN.value.equals(APIErrorCode.SUCCESS.getIntValue());
		
		_took = _raw.get(TDNAPIServer.PROCESS_TOOK_KEYWORD);
		_parseTook = _raw.get(TDNAPIServer.PARSE_TOOK_KEYWORD);
		_responseTook = _raw.get(TDNAPIServer.RESPONSE_TOOK_KEYWORD);
	}

	public boolean wasRequestSuccessful()
	{
		return _success;
	}

	public TDNRoot getData()
	{
		if (_data == null)
		{
			TDNValue dataRaw = _raw.get(TDNAPIServer.DATA_KEYWORD);
			if (!dataRaw.parser().typeKey().equals(TDNParsers.ROOT.typeKey()))
				throw new RuntimeException("Key for data isn't a root!");

			_data = dataRaw.as();
		}

		return _data;
	}

	public String getErrorDetails()
	{
		if (_success)
			return null;

		if (_errorDetails == null)
		{
			TDNValue detailsTDN = getData().get(TDNAPIServer.ERROR_DETAILS_KEYWORD);
			if (detailsTDN == null)
				throw new RuntimeException("Data don't contain object with error details!");
			_errorDetails = detailsTDN.as();
		}
		
		return _errorDetails;
	}
	
	public int getProcessTook()
	{
		if(_took == null)
			throw new RuntimeException("Response doesn't containt information about how long the process took");
		return (int)_took.value;
	}
	
	public int getParseTook()
	{
		if(_parseTook == null)
			throw new RuntimeException("Response doesn't containt information about how long the parsing took");
		return (int)_parseTook.value;
	}
	
	public int getResponseTook()
	{
		if(_responseTook == null)
			throw new RuntimeException("Response doesn't containt information about how long the response took");
		return (int)_responseTook.value;
	}
}
