package team.hobbyrobot.logging;

public class VerbosityLogger
{
	public static final int DEFAULT = 0;
	public static final int OVERVIEW = 1;
	public static final int DETAILED_OVERVIEW = 2;
	public static final int DEBUGGING = 3;
	
	public VerbosityLogger(Logger logger)
	{
		this.logger = logger;
	}
	
	private Logger logger;
	
	private int verbosityLevel = 0;
	
	public int getVerbosityLevel()
	{
		return verbosityLevel;
	}
	
	public void setVerbosityLevel(int level)
	{
		if(level >= Integer.MAX_VALUE)
			verbosityLevel = Integer.MAX_VALUE - 1;
		if(level <= Integer.MIN_VALUE)
			verbosityLevel = Integer.MIN_VALUE + 1;
		else
			verbosityLevel = level;
	}
	
	public void ignoreVerbosityLevel()
	{
		verbosityLevel = Integer.MAX_VALUE;
	}
	
	public void setUnbeatableVerbosityLevel()
	{
		verbosityLevel = Integer.MIN_VALUE;
	}
	
	public void log(String message, int currVerbosityLevel) 
	{
		if(currVerbosityLevel <= this.verbosityLevel)
			logger.log(message);
	}
	
	public void priorityLog(String message)
	{
		logger.log(message);
	}
	
	public Logger getWrappedLogger()
	{
		return logger;
	}
}
