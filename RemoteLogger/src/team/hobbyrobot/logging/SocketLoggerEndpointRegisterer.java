package team.hobbyrobot.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import team.hobbyrobot.net.ClientRegisterer;

public class SocketLoggerEndpointRegisterer implements ClientRegisterer
{
	private Logger logger;
	private int port;

	private ServerSocket serverSocket;
	private List<Socket> registeredClients = new ArrayList<Socket>();
	private Thread registeringThread;

	private boolean shouldBeRunning = false;

	public SocketLoggerEndpointRegisterer(Logger logger, int port)
	{
		this.logger = logger;
		this.port = port;
	}

	public void startRegisteringClients() throws IOException
	{
		if(isRegisteringClients())
			return;
		
		serverSocket = new ServerSocket(getPort());
		shouldBeRunning = true;
		
		registeringThread = new Thread()
		{
			public void run()
			{
				while(shouldBeRunning)
				{
					try
					{
						registerClient();						
					}
					catch(IOException ex)
					{
						logger.log("IOException was thrown when registering a clinet"
							+ " in SocketLoggerEndpointRegisterer. EXCEPTION: " + Logger.getExceptionInfo(ex));
					}
				}
			}
		};
		registeringThread.setDaemon(true);
		registeringThread.setPriority(Thread.MIN_PRIORITY);
		registeringThread.start();
	}
	
	private synchronized void registerClient() throws IOException
	{
		Socket client = null;
		try
		{
			client = serverSocket.accept();
		}
		catch (SocketException ex)
		{
			logger.log("SocketException was thrown when waiting for a clinet"
				+ " in SocketLoggerEndpointRegisterer. This may indicate that the "
				+ "registering server was closed. EXCEPTION: " + Logger.getExceptionInfo(ex));
		}
		catch (IOException ex)
		{
			logger.log("IOException was thrown when waiting for a clinet"
				+ " in SocketLoggerEndpointRegisterer. EXCEPTION: " + Logger.getExceptionInfo(ex));
		}
		if(client == null)
			return;
		
		PrintWriter pw = new PrintWriter(client.getOutputStream());
		logger.registerEndpoint(pw);
		registeredClients.add(client);
	}

	public synchronized void stopRegisteringClients() throws IOException
	{
		if(!isRegisteringClients())
			return;
		shouldBeRunning = false;
		serverSocket.close();
		serverSocket = null;
	}

	public int getPort()
	{
		return port;
	}
	
	public synchronized int countRegisteredClients()
	{
		return registeredClients.size();
	}
	
	public synchronized void closeRegisteredClients() throws IOException
	{
		for(Socket client : registeredClients)
			client.close();
		registeredClients.clear();
	}

	public boolean isRegisteringClients()
	{
		return registeringThread != null && registeringThread.isAlive();
	}
}
