package team.hobbyrobot.python;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Bridge
{
	private int _port;
	private String _host;
	private Socket _bridgeSocket;
	private InputStreamReader _bridgeReader;
	private DataReceiver _receiver;
	private LinkedList<BridgeListener> _listeners;
	
	public Bridge(String host, int port)
	{
		_port = port;
		_host = host;
		_listeners = new LinkedList<BridgeListener>();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> close()));
	}
	
	public void start() throws IOException
	{
		if(_bridgeSocket != null)
			return;
		
		_bridgeSocket = new Socket(_host, _port);
		
		_bridgeReader = new InputStreamReader(_bridgeSocket.getInputStream());
		_receiver = new DataReceiver();
		_receiver.start();
	}
	
	public void close()
	{
		if(_bridgeSocket == null || _bridgeSocket.isClosed())
			return;
		try
		{
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(_bridgeSocket.getOutputStream()));
			pw.println("CLOSE");
			pw.flush();
			//_bridgeSocket.close();
		}
		catch (IOException e)
		{
			System.err.println("Closing bridge socket threw IOException: " + e.toString());
		}
	}
	
	public HashMap getLast()
	{
		return _receiver.getLast();
	}
	
	public void addListener(BridgeListener listener)
	{
		synchronized(_listeners)
		{
			_listeners.add(listener);			
		}
	}
	
	public void removeListener(BridgeListener listener)
	{
		synchronized(_listeners)
		{
			_listeners.remove(listener);			
		}
	}
	
	private class DataReceiver extends Thread
	{
		private JSONParser _parser = new JSONParser();
		private JSONObject _lastObj = null;
		
		public JSONObject getLast()
		{
			if(_lastObj == null)
				return null;
			synchronized(_lastObj)
			{
				return _lastObj;
			}
		}
		
		@Override
		public void run()
		{
			BufferedReader br = new BufferedReader(_bridgeReader);
			try
			{
				main: while(true)
				{	
					String line = null;
					try
					{
						line = br.readLine();
					}
					catch (SocketException e)
					{
						
					}
					
					if(line == null)
						break main;
					JSONObject tmp = (JSONObject) _parser.parse(line);
					
					if(_lastObj == null)
					{
						_lastObj = tmp;
						continue;
					}
					
					synchronized(_lastObj)
					{
						_lastObj = tmp;
					}
					
					synchronized(_listeners)
					{
						for(BridgeListener l : _listeners)
							l.dataReceived(_lastObj);
					}
				}
			}
			catch (ParseException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
