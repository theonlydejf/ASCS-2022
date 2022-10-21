package team.hobbyrobot.net.api.tests;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map.Entry;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.logging.VerbosityLogger;
import team.hobbyrobot.net.api.*;
import team.hobbyrobot.net.api.exceptions.RequestGeneralException;
import team.hobbyrobot.net.api.exceptions.RequestParamsException;
import team.hobbyrobot.net.api.exceptions.UnknownRequestException;
import team.hobbyrobot.tdn.base.TDNArray;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNRootParser;
import team.hobbyrobot.tdn.core.TDNValue;

class APITest
{

	public static void main(String[] args) throws UnknownHostException, IOException
	{
		Logger serverLogger = new Logger("SERVER");
		serverLogger.registerEndpoint(new PrintWriter(System.out));
		Logger clientLogger = new Logger("CLIENT");
		clientLogger.registerEndpoint(new PrintWriter(System.out));
		
		TDNAPIServer server = new TDNAPIServer(1112, serverLogger, serverLogger.createSubLogger("ERR"));
		server.registerService("test", new TestService());
		server.setVerbosity(VerbosityLogger.DEBUGGING);
		Thread serverThread = server.createThread();
		serverThread.start();
		server.startRegisteringClients();
		
		TDNAPIClient client = new TDNAPIClient("localhost", 1112, clientLogger);
		
		client.setVerbosity(VerbosityLogger.DEBUGGING);
		System.out.println("Sending...");
		client.request("test", "ahoj", new TDNRoot());
		System.out.println("sent");
		
		System.in.read();
		server.stopRegisteringClients();
		System.in.read();
		client.request("test", "ahoj", new TDNRoot());
		System.in.read();
		server.close();
		
		System.in.read();

/*		System.in.read();
		System.out.println("Stopping");
		try
		{
			server.stop();			
		}
		catch(IOException ex)
		{
			System.out.println("exception cought");
		}
		System.in.read();*/
		//System.exit(0);
	}

	public static class TestService implements Service
	{

		@Override
		public TDNRoot processRequest(String request, TDNRoot params, Socket client)
			throws UnknownRequestException, RequestParamsException, RequestGeneralException
		{
			System.out.println("rqst=" + request);
			System.out.print("params=");
			printRoot(params);
			return new TDNRoot();
		}

		@Override
		public void init()
		{
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public static void printRoot(TDNRoot root)
	{
		System.out.println("(");
		StringBuilder sb = new StringBuilder();
		for (Entry<String, TDNValue> val : root)
		{
			sb.append(val.getKey());
			sb.append(": ");
			if (val.getValue().value instanceof TDNRoot)
			{
				printRoot((TDNRoot) val.getValue().value);
				continue;
			}
			if (val.getValue().value instanceof TDNArray)
			{
				System.out.println(sb.toString() + "[");
				sb = new StringBuilder();
				TDNArray arr = (TDNArray) val.getValue().value;
				for (Object item : arr)
				{
					sb.append(",");
					if (arr.itemParser.typeKey().equals(new TDNRootParser().typeKey()))
						printRoot((TDNRoot) item);
					else
					{
						System.out.println(sb.toString() + item);
						sb = new StringBuilder();
					}
					continue;
				}
				System.out.println(sb.toString() + "]");
				sb = new StringBuilder();
				continue;
			}
			System.out.println(sb.toString() + val.getValue().value);
			sb = new StringBuilder();
		}
		System.out.println(sb.toString() + ")");
	}
}
