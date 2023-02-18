package team.hobbyrobot.net.api.services;

import java.net.Socket;
import java.util.Hashtable;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.net.api.Service;
import team.hobbyrobot.net.api.exceptions.RequestGeneralException;
import team.hobbyrobot.net.api.exceptions.RequestParamsException;
import team.hobbyrobot.net.api.exceptions.UnknownRequestException;
import team.hobbyrobot.tdn.core.TDNRoot;

public abstract class AbstractService implements Service
{
	protected Hashtable<String, RequestInvoker> requests = null;

	protected abstract static class RequestInvoker
	{
		protected TDNRoot params;
		
		protected Socket client;
		
		public abstract TDNRoot invoke(TDNRoot params) throws RequestParamsException, RequestGeneralException;
	}

	@Override
	public TDNRoot processRequest(String request, TDNRoot params, Socket client)
		throws UnknownRequestException, RequestParamsException, RequestGeneralException
	{
		try
		{
			RequestInvoker requestMethod = requests.get(request);			
			if (requestMethod == null)
				throw new UnknownRequestException();
			
	        requestMethod.client = client;

			return requestMethod.invoke(params);
		}
		catch (UnknownRequestException e)
		{
			throw e;
		}
		catch (RequestParamsException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new RequestGeneralException(
				"Exception was thrown while performing a request: " + Logger.getExceptionInfo(e));
		}
	}

	@Override
	public void init()
	{
		 requests = initRequests();
	}
	
	protected abstract Hashtable<String, RequestInvoker> initRequests();
}
