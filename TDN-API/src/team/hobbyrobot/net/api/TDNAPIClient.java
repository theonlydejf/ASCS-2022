package team.hobbyrobot.net.api;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.logging.VerbosityLogger;
import team.hobbyrobot.net.api.util.Timer;
import team.hobbyrobot.net.api.util.TimerListener;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class TDNAPIClient implements TimerListener, Closeable
{
	public static final int HEARTBEAT_INTERVAL = TDNAPIServer.HEARTBEAT_TIMEOUT - 300;
	
	private static final TDNRoot HEARTBEAT_REQUEST;
	
	private Socket _client;
	private BufferedReader _reader;
	private BufferedWriter _writer;
	private Timer _heartbeatTimer;
	private VerbosityLogger _logger;
	
	static
	{
		HEARTBEAT_REQUEST = new TDNRoot();
		HEARTBEAT_REQUEST.put("service", new TDNValue(TDNAPIServer.API_SERVICE_NAME, TDNParsers.STRING));
		HEARTBEAT_REQUEST.put("request", new TDNValue(TDNAPIServer.HEARTBEAT_REQUEST_NAME, TDNParsers.STRING));
	}
	
	public TDNAPIClient(String hostname, int port, Logger logger) throws UnknownHostException, IOException
	{
		_client = new Socket(hostname, port);
		_reader = new BufferedReader(new InputStreamReader(_client.getInputStream()));
		_writer = new BufferedWriter(new OutputStreamWriter(_client.getOutputStream()));
		_heartbeatTimer = new Timer(HEARTBEAT_INTERVAL, this);
		_heartbeatTimer.start();
		_logger = new VerbosityLogger(logger);
	}

	@Override
	public void timedOut()
	{
		try
		{
			HEARTBEAT_REQUEST.writeToStream(_writer);
			_logger.log("Heartbeat sent", VerbosityLogger.DEBUGGING);
		}
		catch (IOException e)
		{
			_logger.log("Heartbeat failed!", VerbosityLogger.DEFAULT);
			e.printStackTrace();
		}
	}
	
	public TDNRoot rawRequest(TDNRoot request) throws IOException
    {
        request.writeToStream(_writer);
        return TDNRoot.readFromStream(_reader);
    }

    public static TDNRoot createAPIRequest(String service, String request, TDNRoot params)
    {
        TDNRoot tdnRequest = new TDNRoot();
        tdnRequest.put(TDNAPIServer.SERVICE_KEYWORD, new TDNValue(service, TDNParsers.STRING));
        tdnRequest.put(TDNAPIServer.REQUEST_KEYWORD, new TDNValue(request, TDNParsers.STRING));
        tdnRequest.put(TDNAPIServer.PARAMS_KEYWORD, new TDNValue(params, TDNParsers.ROOT));
        return tdnRequest;
    }

    public TDNRoot request(String service, String request, TDNRoot _params) throws IOException
    {
        return rawRequest(createAPIRequest(service, request, _params));
    }
    
	public void setVerbosity(int verbosityLevel)
	{
		_logger.setVerbosityLevel(verbosityLevel);
	}
	
	public String getIP()
	{
	    return _client.getInetAddress().toString();
	}

    @Override
    public void close() throws IOException 
    {
        _logger.log("Closing...", VerbosityLogger.DEFAULT);
        _heartbeatTimer.stop();
        _client.close();
    }
}
