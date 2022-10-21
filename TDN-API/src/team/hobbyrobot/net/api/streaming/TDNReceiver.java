package team.hobbyrobot.net.api.streaming;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.tdn.core.TDNRoot;

public class TDNReceiver extends Thread
{
	private TDNRoot lastRoot;
	private ServerSocket _server;
	private boolean _shouldRun;
	private List<TDNReceiverListener> listeners;

	/** Needs to be started explicitely */
	public TDNReceiver(int port) throws IOException
	{
		_server = new ServerSocket(port);
		setPriority(Thread.MIN_PRIORITY);
		setDaemon(true);
		_shouldRun = false;
		listeners = new LinkedList<TDNReceiverListener>();
		lastRoot = new TDNRoot();
	}

	public void stopReceiving() throws IOException
	{
		_shouldRun = false;
		_server.close();
	}

	public int getPort()
	{
		return _server.getLocalPort();
	}
	
	public void addListener(TDNReceiverListener listener)
	{
		listeners.add(listener);
	}
	
	public TDNRoot getLastRoot()
	{
		return lastRoot;
	}
	
	@Override
	public void run()
	{
		_shouldRun = true;
		// Wait for connection
		try (Socket client = _server.accept();
			InputStreamReader sr = new InputStreamReader(client.getInputStream());
			BufferedReader br = new BufferedReader(sr))
		{
		    long millis = System.currentTimeMillis();
			while(_shouldRun)
			{
				try
				{
					// Read root
					synchronized (lastRoot)
					{
					    millis = System.currentTimeMillis();
						lastRoot = TDNRoot.readFromStream(br);
						Logger.main.log("tdn parse took " + (System.currentTimeMillis() - millis));
						for(TDNReceiverListener listener : listeners)
							listener.rootReceived(lastRoot);
					}
				}
				catch (SocketException e)
				{
					_shouldRun = false;
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			_shouldRun = false;
		}
	}
}
