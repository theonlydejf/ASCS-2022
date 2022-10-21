package team.hobbyrobot.net.api.streaming;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import team.hobbyrobot.tdn.core.TDNRoot;

public class TDNSender implements Closeable
{
	private String _host;
	private int _port;
	
	private Socket _socket;
	private BufferedWriter _bw;
	
	public TDNSender(String host, int port)
	{
		_host = host;
		_port = port;
		_socket = null;
		_bw = null;
	}
	
	public void connect() throws UnknownHostException, IOException
	{
		_socket = new Socket(_host, _port);
		_bw = new BufferedWriter(new OutputStreamWriter(_socket.getOutputStream()));
	}
	
	public void send(TDNRoot root) throws IOException
	{
		root.writeToStream(_bw);
	}
	
	
	
	public boolean isConnected()
	{
		return _bw != null;
	}

	@Override
	public void close() throws IOException
	{
		if(_socket != null)
		{
			_socket.close();
			_bw = null;
		}
	}
}
