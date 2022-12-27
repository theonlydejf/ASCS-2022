package team.hobbyrobot.net.api.remoteevents;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.logging.VerbosityLogger;
import team.hobbyrobot.net.ClientRegisterer;
import team.hobbyrobot.net.api.registerer.ClientRegistererThread;
import team.hobbyrobot.net.api.registerer.RegisteredEndpoint;
import team.hobbyrobot.tdn.core.TDNRoot;

public class RemoteEventListenerServer extends Thread implements ClientRegisterer
{
	private int _port;
	private ClientRegistererThread _registerer;
	private LinkedList<RemoteEventListener> _listeners = new LinkedList<RemoteEventListener>();
	private Logger _logger;

	public RemoteEventListenerServer(int port, Logger logger)
	{
		_port = port;
		_logger = logger.createSubLogger("RemoteEventServer");
		_registerer = new ClientRegistererThread(_port, _logger);
		setDaemon(true);
	}

	private void receiveEventFromClient(RegisteredEndpoint client)
	{
		try
		{
			TDNRoot event = TDNRoot.readFromStream(client.reader);
			String eventName = event.get(RemoteEventProvider.EVENT_KEY).as();
			TDNRoot params = event.get(RemoteEventProvider.PARAMS_KEY).as();
			synchronized (_listeners)
			{
				for (RemoteEventListener l : _listeners)
					l.eventReceived(eventName, params, client.socket);
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		ArrayList<Thread> readThreads = new ArrayList<Thread>();
		while (!interrupted())
		{
			synchronized (_registerer.clients)
			{
				for (int i = 0; i < _registerer.countRegisteredClients(); i++)
				{
					final int j = i;
					if(readThreads.size() > i)
					{
						if(readThreads.get(i) != null && readThreads.get(i).isAlive())
							continue;
						
						Thread t = new Thread()
						{
							@Override
							public void run()
							{
								receiveEventFromClient(_registerer.clients.get(j));
							}
						};
						
						t.start();
						readThreads.set(i, t);
						continue;
					}
					Thread t = new Thread()
					{
						@Override
						public void run()
						{
							receiveEventFromClient(_registerer.clients.get(j));
						}
					};
					t.start();
					readThreads.add(t);
				}

			}
		}
		// formater:off
		/*
		 * RegisteredEndpoint client = null;
		 * while (!interrupted())
		 * {
		 * client = null;
		 * while (client == null)
		 * {
		 * if (interrupted())
		 * return;
		 * synchronized (_registerer.clients)
		 * {
		 * for (int i = 0; i < _registerer.countRegisteredClients(); i++)
		 * {
		 * RegisteredEndpoint ept = _registerer.clients.get(i);
		 * try
		 * {
		 * if (ept.reader.ready())
		 * {
		 * client = ept;
		 * break;
		 * }
		 * }
		 * catch (IOException e)
		 * {
		 * try
		 * {
		 * ept.close();
		 * _registerer.clients.remove(i);
		 * break;
		 * }
		 * catch (IOException e1)
		 * {
		 * e.printStackTrace();
		 * }
		 * }
		 * }
		 * }
		 * }
		 * try
		 * {
		 * while(client.inputStream.available() > 0)
		 * {
		 * TDNRoot event = TDNRoot.readFromStream(client.reader);
		 * String eventName = event.get(RemoteEventProvider.EVENT_KEY).as();
		 * TDNRoot params = event.get(RemoteEventProvider.PARAMS_KEY).as();
		 * synchronized (_listeners)
		 * {
		 * for (RemoteEventListener l : _listeners)
		 * l.eventReceived(eventName, params, client.socket);
		 * }
		 * }
		 * }
		 * catch (IOException e)
		 * {
		 * e.printStackTrace();
		 * }
		 * }
		 */
		// formater:on
	}

	public void addListener(RemoteEventListener l)
	{
		synchronized (_listeners)
		{
			_listeners.add(l);
		}
	}

	public void removeListener(RemoteEventListener l)
	{
		synchronized (_listeners)
		{
			_listeners.remove(l);
		}
	}

	@Override
	public void startRegisteringClients() throws IOException
	{
		_registerer.startRegisteringClients();
	}

	@Override
	public void stopRegisteringClients() throws IOException
	{
		_registerer.stopRegisteringClients();
	}

	@Override
	public int countRegisteredClients()
	{
		return _registerer.countRegisteredClients();
	}

	@Override
	public void closeRegisteredClients() throws IOException
	{
		_registerer.closeRegisteredClients();
	}

	@Override
	public boolean isRegisteringClients()
	{
		return _registerer.isRegisteringClients();
	}

	public int getPort()
	{
		return _port;
	}
}
