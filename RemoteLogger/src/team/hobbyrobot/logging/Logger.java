package team.hobbyrobot.logging;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Logger implements Closeable
{
    public static Logger main = new Logger();
	private ArrayList<PrintWriter> writers = new ArrayList<PrintWriter>();
	private ArrayList<String> localLog = null;
	private boolean includeName = true;
	
	protected String name = null;
	
	public Logger()
	{
		this(null, false);
	}
	
	public Logger(String name)
	{
		this(name, false);
	}
	
	public Logger(String name, boolean saveLogToMemory)
	{
		this.name = name;
		this.localLog = saveLogToMemory ? new ArrayList<String>() : null;
		
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				log("Logger shutting down...");
				try
				{
					close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	public String[] getLog()
	{
		if(localLog == null)
			return null;
		return localLog.toArray(new String[localLog.size()]);
	}
	
	public void setIncludeName(boolean val)
	{
		includeName = val;
	}

	public synchronized void registerEndpoint(PrintWriter writer)
	{
		writers.add(writer);
	}

	public synchronized void unregisterEndpoint(PrintWriter writer)
	{
		writers.remove(writer);
	}

	public synchronized void log(String message)
	{
		String msg = message;
		if(includeName && name != null)
			msg = name + ": " + msg;
		
		if(localLog != null)
			localLog.add(msg);

		ArrayList<PrintWriter> badWriters = new ArrayList<PrintWriter>();
		for (PrintWriter pw : writers)
		{
			try
			{
				pw.println(msg);
				pw.flush();
			}
			catch (Exception ex)
			{
				badWriters.add(pw);
			}
		}

		if (badWriters.size() > 0)
		{
			for (PrintWriter pw : badWriters)
				writers.remove(pw);
			for (int i = 0; i < badWriters.size(); i++)
				log("Writer for logger threw an exception");
		}
	}

	public static String getExceptionInfo(Exception ex)
	{
		String msg = "Error message: " + ex.getMessage() + "; Stack trace: ";

		for (StackTraceElement trace : ex.getStackTrace())
		{
			msg += "\n\t" + trace.toString();
		}

		msg += "\n; Cause: " + ex.getCause() + "; ToString: " + ex.toString();
		return msg;
	}

	@Override
	public void close() throws IOException
	{
		for (PrintWriter pw : writers)
			pw.close();
	}
	
	public Logger createSubLogger(String name, boolean saveLoggerToMemory)
	{
		return new SubLogger(name, this, saveLoggerToMemory);
	}
	
	public Logger createSubLogger(String name)
	{
		return createSubLogger(name, false);
	}
	
	public String getName()
	{
		return name;
	}
	
	public class SubLogger extends Logger
	{
		private Logger parent;
		SubLogger(String name, Logger parent, boolean saveLoggerToMemory)
		{
			super(name, saveLoggerToMemory);
			this.parent = parent;
			setIncludeName(false);
		}
		
		@Override
		public synchronized void log(String message) 
		{
			super.log(message);
			parent.log(name + ": " + message);
		}
		
		public Logger getParent()
		{
			return parent;
		}
	}
}
