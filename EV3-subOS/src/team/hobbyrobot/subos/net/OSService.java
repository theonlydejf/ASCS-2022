package team.hobbyrobot.subos.net;

import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;

import team.hobbyrobot.net.api.Service;
import team.hobbyrobot.net.api.exceptions.RequestGeneralException;
import team.hobbyrobot.net.api.exceptions.RequestParamsException;
import team.hobbyrobot.net.api.exceptions.UnknownRequestException;
import team.hobbyrobot.net.api.services.AbstractService;
import team.hobbyrobot.subos.Resources;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class OSService extends AbstractService
{

	@Override
	protected Hashtable<String, RequestInvoker> initRequests()
	{
		return new Hashtable<String, RequestInvoker>() 
		{
			{
				put("getResources", new RequestInvoker() 
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException, RequestGeneralException
					{
						return new TDNRoot().insertValue("resources", new TDNValue(Resources.global().getTDNRoot(), TDNParsers.ROOT));
					}
				});
				
				put("getResource", new RequestInvoker() 
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException, RequestGeneralException
					{
						TDNValue pathTDN = params.get("path");
						if(pathTDN == null)
							throw new RequestParamsException("Path was not found in the params root", "path");
						
						return new TDNRoot().insertValue("resource", Resources.global().getTDN((String)pathTDN.as()));
					}
				});
				
				put("setResource", new RequestInvoker() 
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException, RequestGeneralException
					{
						TDNValue pathTDN = params.get("path");
						if(pathTDN == null)
							throw new RequestParamsException("Path was not found in the params root", "path");
						TDNValue valueTDN = params.get("value");
						if(valueTDN == null)
							throw new RequestParamsException("Value was not found in the params root", "value");
						
						Resources.global().set((String)pathTDN.as(), valueTDN);
						try
						{
							Resources.global().push();							
						}
						catch(IOException ex)
						{
							throw new RequestGeneralException("Saving resources failed: " + ex.toString());
						}
						
						return new TDNRoot();
					}
				});
			}
		};
	}
}
