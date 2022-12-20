package team.hobbyrobot.net.api;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.logging.VerbosityLogger;
import team.hobbyrobot.net.ClientRegisterer;
import team.hobbyrobot.net.api.exceptions.RequestGeneralException;
import team.hobbyrobot.net.api.exceptions.RequestParamsException;
import team.hobbyrobot.net.api.exceptions.UnknownRequestException;
import team.hobbyrobot.net.api.util.Stopwatch;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class TDNAPIServer implements ClientRegisterer, Runnable, Closeable
{
	// - Constants for parsing requests - //
	public static final String SERVICE_KEYWORD = "service";
	public static final String REQUEST_KEYWORD = "request";
	public static final String PARAMS_KEYWORD = "params";

	// - Constants for parsing responses - //
	public static final String ERROR_CODE_KEYWORD = "errorCode";
	public static final String ERROR_DETAILS_KEYWORD = "details";
	public static final String DATA_KEYWORD = "data";
	public static final String PARSE_TOOK_KEYWORD = "parseTook";
	public static final String RESPONSE_TOOK_KEYWORD = "responseTook";
	public static final String PROCESS_TOOK_KEYWORD = "took";

    public static final String API_SERVICE_NAME = "API";
    public static final String HEARTBEAT_REQUEST_NAME = "Heartbeat";
	
	public static final int HEARTBEAT_TIMEOUT = 10000;

	// - Variables for TCP server - //
	/** Port on which the api server is listening */
	private int port;
	/** The instance of server on which the api is listening */
	private ServerSocket server;
	/**
	 * Flag which describes whether the api server should be listening (doesn't determin if
	 * the server is listening)
	 */
	private boolean shouldBeRunning = false;
	/** Maximum amount of clients, that can be connected to the server at once (-1 for no limit) */
	private int maxClients = -1;

	// - Variables for client registering - //
	/** Thread, on which the api is accepting clients */
	private Thread clientRegistererThread = null;
	/** List of all connected clients */
	private List<APIEndpoint> clients;
	/**
	 * Flag which describes whether the api server should try to accept new client
	 * (doesn't determin wheter the server is accepting nor whether the registerer reached
	 * client limit)
	 */
	private boolean acceptingClients = false;

	// - Variables for service support - //
	/** Map containing all registered services and their corresponding names */
	private Map<String, Service> services;

	// - Loggers - //
	/** Logger used to log any info */
	private VerbosityLogger infoLogger;
	/** Logger used to log any error */
	private Logger errorLogger;

	public TDNAPIServer(int port, Logger infoLogger, Logger errorLogger)
	{
		this.port = port;
		this.infoLogger = new VerbosityLogger(infoLogger.createSubLogger("API"));
		this.errorLogger = errorLogger;
		services = new Hashtable<String, Service>();
		clients = new ArrayList<APIEndpoint>();
		
		registerService(API_SERVICE_NAME, new APIService());
	}

	public TDNAPIServer(int port, Logger logger)
	{
		this(port, logger, null);
	}

	private void logError(String msg)
	{
		if (errorLogger == null)
			infoLogger.priorityLog(msg);
		else
			errorLogger.log(msg);
	}

	@Override
	public void run()
	{
		if (shouldBeRunning)
			return;
		shouldBeRunning = true;

		infoLogger.log("Started on port " + getPort(), VerbosityLogger.DETAILED_OVERVIEW);

		infoLogger.log("Waiting for request from " + countRegisteredClients() + " clients...",
			VerbosityLogger.DETAILED_OVERVIEW);
		LinkedList<APIEndpoint> clientsToRemove = new LinkedList<APIEndpoint>();
		while (shouldBeRunning)
		{
			if (countRegisteredClients() <= 0)
				continue;

			APIEndpoint client = null;
			try
			{
				Stopwatch sw = new Stopwatch();
				synchronized (clients)
				{
					// Check if any client has sent something
					for (APIEndpoint _client : clients)
					{
						// If the current client has been quiet for a certain amount of time 
						//   -> mark him and discnnect him later
						if (_client.heartbeatSw.elapsed() > HEARTBEAT_TIMEOUT)
						{
							clientsToRemove.add(_client);
							continue;
						}
						
						// If the current client has sent something -> process the data he has sent
						if (_client.inputStream.available() > 0)
						{
							client = _client;
							sw.reset();
							break;
						}
					}
					
					// Disconnect all quiet clients
					for (APIEndpoint disconnectedClient : clientsToRemove)
					{
						disconnectedClient.close();
						clients.remove(disconnectedClient);
						infoLogger.log("Client disconnected: " + disconnectedClient.socket.getInetAddress().toString()
							+ " " + disconnectedClient.socket.getPort() + ". Current client count: "
							+ countRegisteredClients(), VerbosityLogger.OVERVIEW);
					}
					if (clientsToRemove.size() > 0)
						clientsToRemove.clear();
				}
				
				if (client == null) // If none of the clients have sent anything
					continue;
				
				Stopwatch partSw = new Stopwatch();
				// Read request
				TDNRoot request = TDNRoot.readFromStream(client.reader);
				int parseTook = partSw.elapsed();
				partSw.reset();
				TDNRoot response = processRequest(request, client);
				if (response != null)
				{
					int responseTook = partSw.elapsed();
					response.insertValue(PROCESS_TOOK_KEYWORD, new TDNValue(sw.elapsed(), TDNParsers.INTEGER));
					response.insertValue(PARSE_TOOK_KEYWORD, new TDNValue(parseTook, TDNParsers.INTEGER));
					response.insertValue(RESPONSE_TOOK_KEYWORD, new TDNValue(responseTook, TDNParsers.INTEGER));

					response.writeToStream(client.writer);
				}

				infoLogger.log("Waiting for request from " + countRegisteredClients() + " clients...",
					VerbosityLogger.DETAILED_OVERVIEW);
			}
			catch (IOException ex)
			{
				logError("TDNAPIServer - exception was thrown when talking to a client; EXCEPTION: "
					+ Logger.getExceptionInfo(ex));
			}
		}

		infoLogger.log("API ended...", VerbosityLogger.DETAILED_OVERVIEW);
	}

	private synchronized TDNRoot processRequest(TDNRoot request, APIEndpoint client)
	{
		// Check if a valid service is contained inside the request
		TDNValue serviceStr = request.get(SERVICE_KEYWORD);
		if (serviceStr == null || !serviceStr.parser().typeKey().equals(TDNParsers.STRING.typeKey()))
			return ResponseFactory.createExceptionResponse(APIErrorCode.UNKNOWN_SERVICE,
				"Unable to find service in the root");

		// True if the user is requesting api service
		boolean apiRequest = ((String) serviceStr.value).equals(API_SERVICE_NAME);
		if(!apiRequest)
		    infoLogger.log("Processing request for service " + serviceStr.value, VerbosityLogger.DETAILED_OVERVIEW);
		// Heartbeat
		if (apiRequest)
			client.heartbeatSw.reset();
		
		// Get the requested service
		Service service = services.get((String) serviceStr.value);
		// Check if the requested service is unknown
		if (service == null)
			return ResponseFactory.createExceptionResponse(APIErrorCode.UNKNOWN_SERVICE,
				"Service \"" + (String) serviceStr.value + "\" is not registered");

		// Check if a valid request name is contained inside the request
		TDNValue requestName = request.get(REQUEST_KEYWORD);
		if (requestName == null || !requestName.parser().typeKey().equals(TDNParsers.STRING.typeKey()))
			return ResponseFactory.createExceptionResponse(APIErrorCode.UNKNOWN_REQUEST,
				"Unable to find request in the root");

		// Check if a valid params root is contained inside the request
		TDNValue params = request.get(PARAMS_KEYWORD);
		if (params != null)
			if (!params.parser().typeKey().equals(TDNParsers.ROOT.typeKey()))
				return ResponseFactory.createExceptionResponse(APIErrorCode.PARAMS_ERROR,
					"Unable to find params in the root");

		try
		{
			client.heartbeatSw.reset();
			return ResponseFactory.createSuccessResponse(
				service.processRequest((String) requestName.value, params == null ? new TDNRoot() : (TDNRoot) params.value, client.socket));
		}
		catch (UnknownRequestException e)
		{
			return ResponseFactory.createExceptionResponse(APIErrorCode.UNKNOWN_REQUEST,
				"Unknown request: " + (String) requestName.value);
		}
		catch (RequestParamsException e)
		{
			return ResponseFactory.createExceptionResponse(APIErrorCode.PARAMS_ERROR,
				e.message + "; Bad params: " + joinStr(", ", e.badParams));
		}
		catch (RequestGeneralException e)
		{
			return ResponseFactory.createExceptionResponse(APIErrorCode.GENERAL_EXCEPTION, e.details);
		}
	}

	private static String joinStr(String separator, String[] input)
	{

		if (input == null || input.length <= 0)
			return "";

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < input.length; i++)
		{

			sb.append(input[i]);

			// if not the last item
			if (i != input.length - 1)
			{
				sb.append(separator);
			}

		}

		return sb.toString();

	}

	//TODO
	public synchronized void close() throws IOException
	{
		if(isRegisteringClients())
			stopRegisteringClients();
		
		shouldBeRunning = false;
		if(server != null)
			server.close();
		for(APIEndpoint client : clients)
			client.close();
		//throw new RuntimeException("Not implemented yet");
	}

	@Override
	public void startRegisteringClients() throws IOException
	{
		if (isRegisteringClients())
			return;

		acceptingClients = true;
		clients = new LinkedList<APIEndpoint>();
		server = new ServerSocket(getPort());

		clientRegistererThread = new Thread()
		{
			public void run()
			{
				infoLogger.log("\tClientRegisterer starting...", VerbosityLogger.DETAILED_OVERVIEW);
				while (acceptingClients)
				{
					if (getMaxClients() > -1 && clients.size() >= getMaxClients())
						continue;

					acceptClient();
				}
				infoLogger.log("\tClientRegisterer stopped", VerbosityLogger.DETAILED_OVERVIEW);
			}
		};
		clientRegistererThread.setDaemon(true);
		clientRegistererThread.setPriority(Thread.MIN_PRIORITY);
		clientRegistererThread.start();
	}

	public synchronized void registerService(String serviceKey, Service service)
	{
		service.init();
		services.put(serviceKey, service);
	}

	private void acceptClient()
	{
		Socket client = null;
		try
		{
			infoLogger.log("\tClientRegisterer wating for client...", VerbosityLogger.DETAILED_OVERVIEW);
			client = server.accept();
		}
		catch (SocketException ex)
		{
			infoLogger.log("SocketException was thrown when waiting for a clinet"
				+ " in ASCSVehicleAPIServer. This may indicate that the registering server was closed. EXCEPTION: "
				+ Logger.getExceptionInfo(ex), VerbosityLogger.DEFAULT);
		}
		catch (IOException ex)
		{
			logError("IOException was thrown when waiting for a clinet" + " in ASCSVehicleAPIServer. EXCEPTION: "
				+ Logger.getExceptionInfo(ex));
		}
		if (client == null)
			return;

		try
		{
			synchronized (clients)
			{
				clients.add(new APIEndpoint(client));
			}
			infoLogger.log("\tClientRegisterer client registered: " + client.getInetAddress(),
				VerbosityLogger.OVERVIEW);
		}
		catch (IOException ex)
		{
			logError("IOException was thrown when connecting to a clinet" + " in ASCSVehicleAPIServer. EXCEPTION: "
				+ Logger.getExceptionInfo(ex));
		}
	}

	@Override
	public synchronized void stopRegisteringClients() throws IOException
	{
		if (!isRegisteringClients())
			return;

		acceptingClients = false;
		server.close();
		server = null;
	}

	@Override
	public synchronized boolean isRegisteringClients()
	{
		return clientRegistererThread != null && clientRegistererThread.isAlive();
	}

	@Override
	public synchronized void closeRegisteredClients() throws IOException
	{
		for (APIEndpoint client : clients)
			client.close();
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

	public int getPort()
	{
		return port;
	}

	public int getMaxClients()
	{
		return maxClients;
	}

	public void setMaxClients(int max)
	{
		maxClients = max;
	}

	public Thread createThread()
	{
		Thread t = new Thread(this);
		t.setDaemon(true);
		t.setPriority(Thread.MAX_PRIORITY);
		return t;
	}

	public void setVerbosity(int verbosityLevel)
	{
		infoLogger.setVerbosityLevel(verbosityLevel);
	}

	private static class APIEndpoint implements Closeable
	{
		public Socket socket;
		public BufferedReader reader;
		public BufferedWriter writer;
		public InputStream inputStream;
		public OutputStream outputStream;
		public Stopwatch heartbeatSw;

		public APIEndpoint(Socket client) throws IOException
		{
			this.socket = client;
			inputStream = client.getInputStream();
			outputStream = client.getOutputStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			writer = new BufferedWriter(new OutputStreamWriter(outputStream));
			heartbeatSw = new Stopwatch();
		}

		@Override
		public void close() throws IOException
		{
			socket.close();
		}
	}

	private class APIService implements Service
	{

		@Override
		public TDNRoot processRequest(String request, TDNRoot params, Socket client)
			throws UnknownRequestException, RequestParamsException, RequestGeneralException
		{
			if (request.equals(HEARTBEAT_REQUEST_NAME))
			{
				infoLogger.log("API heartbeat", VerbosityLogger.DEBUGGING);
				return null;
			}

			return null;
		}

		@Override
		public void init()
		{
		}

	}
}
