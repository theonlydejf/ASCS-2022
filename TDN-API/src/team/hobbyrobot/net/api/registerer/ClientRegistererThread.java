package team.hobbyrobot.net.api.registerer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.logging.VerbosityLogger;
import team.hobbyrobot.net.ClientRegisterer;

public class ClientRegistererThread extends Thread implements ClientRegisterer
{
	private int _port;
	private boolean _acceptingClients = false;
	public LinkedList<RegisteredEndpoint> clients = new LinkedList<RegisteredEndpoint>();
	private ServerSocket _server;
	private VerbosityLogger _logger;
	
	public ClientRegistererThread(int port, Logger logger)
	{
		_port = port;
		_logger = new VerbosityLogger(logger == null ? new Logger() : logger.createSubLogger("CLientRegisterer"));
		
		_logger.setVerbosityLevel(VerbosityLogger.DEFAULT);
		setDaemon(true);
		setPriority(Thread.MIN_PRIORITY);
	}
	
	@Override
	public void run()
	{
		while (_acceptingClients)
			acceptClient();
	}
	
	private void acceptClient()
	{
		Socket client = null;
		try
		{
			_logger.log("Waiting for client on port " + _port, VerbosityLogger.DETAILED_OVERVIEW);
			client = _server.accept();
		}
		catch (SocketException ex)
		{
			_logger.log("SocketException was thrown when waiting for a clinet"
				+ " in ASCSVehicleAPIServer. This may indicate that the registering server was closed. EXCEPTION: "
				+ Logger.getExceptionInfo(ex), VerbosityLogger.DEFAULT);
		}
		catch (IOException ex)
		{
			_logger.log("IOException was thrown when waiting for a clinet" + " in ASCSVehicleAPIServer. EXCEPTION: "
				+ Logger.getExceptionInfo(ex), VerbosityLogger.DEFAULT);
		}
		if (client == null)
			return;

		try
		{
			synchronized (clients)
			{
				clients.add(new RegisteredEndpoint(client));
			}
			_logger.log("\t client registered: " + client.getInetAddress(),
				VerbosityLogger.OVERVIEW);
		}
		catch (IOException ex)
		{
			_logger.log("IOException was thrown when connecting to a clinet" + " in ASCSVehicleAPIServer. EXCEPTION: "
				+ Logger.getExceptionInfo(ex), VerbosityLogger.DEFAULT);
		}
	}
	
	@Override
	public void startRegisteringClients() throws IOException
	{
		if (isRegisteringClients())
			return;
		
		_server = new ServerSocket(_port);
		_acceptingClients = true;

		start();
	}

	@Override
	public synchronized void stopRegisteringClients() throws IOException
	{
		if (!isRegisteringClients())
			return;

		_acceptingClients = false;
		_server.close();
		_server = null;
	}

	@Override
	public synchronized boolean isRegisteringClients()
	{
		return isAlive();
	}

	@Override
	public synchronized void closeRegisteredClients() throws IOException
	{
		synchronized (clients)
		{
			for (RegisteredEndpoint client : clients)
				client.close();
		}
		clients.clear();
	}

	@Override
	public int countRegisteredClients()
	{
		synchronized (clients)
		{
			return clients.size();
		}
	}
}
