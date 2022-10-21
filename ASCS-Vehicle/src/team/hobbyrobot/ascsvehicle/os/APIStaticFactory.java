package team.hobbyrobot.ascsvehicle.os;


import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map.Entry;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.logging.VerbosityLogger;
import team.hobbyrobot.net.api.Service;
import team.hobbyrobot.net.api.TDNAPIServer;
import team.hobbyrobot.subos.Referenceable;
import team.hobbyrobot.subos.SubOSController;

public class APIStaticFactory
{
	private static LinkedList<Entry<String, Service>> services = new LinkedList<>();
	private static Logger apiLogger = null;
	private static VerbosityLogger logger = null;
	private static int port = -1;
	private static TDNAPIServer api = null;
	private static Thread apiThread = null;
	
	public static Settings settings = Settings.DEFAULT;
	
	private APIStaticFactory()
	{
	}
	
	public static void queueService(String key, Service service)
	{
		services.add(new AbstractMap.SimpleEntry<String, Service>(key, service));
	}
	
	public static void dequeueService(String key, Service service)
	{
		services.remove(new AbstractMap.SimpleEntry<String, Service>(key, service));
	}
	
	public static void reset()
	{
		services.clear();
		apiLogger = null;
		port = -1;
		settings = Settings.DEFAULT;
		apiThread = null;
	}
	
	public static TDNAPIServer getLastAPIServer()
	{
		return api;
	}
	
	public static void createAPI(Object _percentage, Object _msgFeed) throws IOException
	{
		if(apiLogger == null)
			throw new RuntimeException("Logger was not specified when creating API");
		if(port < 0)
			throw new RuntimeException("Port was not specified when creating API");
		
		Referenceable<Float> percentage = (Referenceable<Float>) _percentage;
		final ArrayList<String> msgFeed = (ArrayList<String>) _msgFeed;
		
		api = new TDNAPIServer(getPort(), getAPILogger(), SubOSController.errorLogger);
		api.setVerbosity(logger == null ? VerbosityLogger.DEFAULT : logger.getVerbosityLevel());
		
		log("Registering services...", VerbosityLogger.DETAILED_OVERVIEW);
		float percentageIncrement = .8f / (float)services.size();
		while(services.size() > 0)
		{
			Entry<String, Service> serviceEntry = services.removeFirst();
			api.registerService(serviceEntry.getKey(), serviceEntry.getValue());
			percentage.setValue(percentage.getValue() + percentageIncrement);
			msgFeed.add(serviceEntry.getKey() + " done");
			log("\t" + serviceEntry.getKey() + " registered", VerbosityLogger.DETAILED_OVERVIEW);
		}
		
		percentage.setValue(.8f);
		if(settings.startServer)
		{
			apiThread = api.createThread();
			apiThread.start();
			msgFeed.add("API started");
			log("Started API server", VerbosityLogger.DETAILED_OVERVIEW);
		}
		percentage.setValue(.9f);

		if(settings.startRegisteringClients)
		{
			api.startRegisteringClients();
			msgFeed.add("registering clients");
			log("Started registering clients on API server", VerbosityLogger.DETAILED_OVERVIEW);
		}
		percentage.setValue(1f);
	}
	
	public static Logger getAPILogger()
	{
		return apiLogger;
	}

	public static void setAPILogger(Logger logger)
	{
		APIStaticFactory.apiLogger = logger;
	}
	
	public static void setInfoLogger(Logger logger)
	{
		APIStaticFactory.logger = new VerbosityLogger(logger);
	}
	
	public static Logger getInfoLogger()
	{
		return logger.getWrappedLogger();
	}
	
	private static void log(String msg, int verbosity)
	{
		if(logger != null)
			logger.log(msg, verbosity);
	}
	
	public static void setVerbosity(int verbosity)
	{
		if(logger != null)
			logger.setVerbosityLevel(verbosity);
	}

	public static int getPort()
	{
		return port;
	}

	public static void setPort(int port)
	{
		APIStaticFactory.port = port;
	}

	public static class Settings
	{
		public static final Settings DEFAULT = new Settings(false, false);
		
		private boolean startRegisteringClients;
		private boolean startServer;

		public Settings(boolean startRegisteringClients, boolean startServer)
		{
			this.startRegisteringClients = startRegisteringClients;
			this.startServer = startServer;
		}

		public boolean isStartRegisteringClients()
		{
			return startRegisteringClients;
		}
		
		public void setStartRegisteringClients(boolean startRegisteringClients)
		{
			this.startRegisteringClients = startRegisteringClients;
		}
		
		public boolean isStartServer()
		{
			return startServer;
		}
		
		public void setStartServer(boolean startServer)
		{
			this.startServer = startServer;
		}
	}
}
