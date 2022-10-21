package team.hobbyrobot.ascsvehicle.api.services;

import java.net.Socket;

import team.hobbyrobot.net.api.Service;
import team.hobbyrobot.net.api.exceptions.RequestGeneralException;
import team.hobbyrobot.net.api.exceptions.RequestParamsException;
import team.hobbyrobot.net.api.exceptions.UnknownRequestException;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;
import team.hobbyrobot.tdn.base.TDNParsers;

public class TestService implements Service
{
	@Override
	public TDNRoot processRequest(String request, TDNRoot params, Socket client)
		throws UnknownRequestException, RequestParamsException, RequestGeneralException
	{
		TDNRoot root = new TDNRoot();
		
		root.insertValue("request", new TDNValue(request, TDNParsers.STRING));
		root.insertValue("recieved-params", new TDNValue(params, TDNParsers.ROOT));
		
		return root;
	}

	@Override
	public void init()
	{
		// TODO Auto-generated method stub
		
	}

}
