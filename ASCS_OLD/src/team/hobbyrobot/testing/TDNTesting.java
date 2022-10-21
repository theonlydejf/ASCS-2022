package team.hobbyrobot.testing;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map.Entry;

import team.hobbyrobot.tdn.base.*;
import team.hobbyrobot.tdn.core.*;
import team.hobbyrobot.subos.logging.Logger;
import team.hobbyrobot.subos.logging.SocketLoggerEndpointRegisterer;
import team.hobbyrobot.subos.logging.VerbosityLogger;
import team.hobbyrobot.subos.net.api.*;

public class TDNTesting
{

	public static void main(String[] args) throws IOException
	{
		Logger serverLogger = new Logger("API Server Logger");
		serverLogger.registerEndpoint(new PrintWriter(System.out));

		Logger clientLogger = new Logger("API Client Logger");
		clientLogger.registerEndpoint(new PrintWriter(System.out));

		TDNAPIServer api = new TDNAPIServer(2222, serverLogger);
		api.setVerbosity(VerbosityLogger.DEBUGGING);
		api.registerService("TestService", new TestService());
		api.startRegisteringClients();

		TDNAPIClient client = new TDNAPIClient("localhost", 2222, clientLogger);
		client.setVerbosity(VerbosityLogger.DEBUGGING);

		Thread apiThread = api.createThread();
		apiThread.start();

		System.in.read();
		printRoot(client.request("TestService", "tohleJeRequest", new TDNRoot().insertValue("hodnota", new TDNValue(69, TDNParsers.INTEGER))));
		System.in.read();
		System.out.println("Exiting...");
	}

	public static void printRoot(TDNRoot root)
	{
		System.out.println("(");
		for (Entry<String, TDNValue> val : root)
		{
			System.out.print(val.getKey());
			System.out.print(": ");
			if (val.getValue().value instanceof TDNRoot)
			{
				printRoot((TDNRoot) val.getValue().value);
				continue;
			}
			if (val.getValue().value instanceof TDNArray)
			{
				TDNArray arr = (TDNArray) val.getValue().value;
				System.out.println("[");
				for (Object item : arr)
				{
					if (arr.itemParser.typeKey().equals(new TDNRootParser().typeKey()))
						printRoot((TDNRoot) item);
					else
						System.out.println(item);
					System.out.print(",");
					continue;
				}
				System.out.println("]");
				continue;
			}
			System.out.println(val.getValue().value);
		}
		System.out.println(")");
	}

}
