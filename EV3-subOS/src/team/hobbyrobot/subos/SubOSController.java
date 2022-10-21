package team.hobbyrobot.subos;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedList;

import lejos.hardware.Battery;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.utility.Delay;
import lejos.utility.Stopwatch;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.subos.errorhandling.ErrorLogging;
import team.hobbyrobot.subos.graphics.GraphicsController;
import team.hobbyrobot.subos.graphics.infobar.InfoBarController;
import team.hobbyrobot.subos.graphics.infobar.InfoBarData;
import team.hobbyrobot.subos.hardware.BrickHardware;
import team.hobbyrobot.subos.hardware.LEDBlinkingStyle;
import team.hobbyrobot.subos.hardware.RobotHardware;

/**
 * Controller subOS - Veskere dulezite veci, ktere jsou potreba k behu subOS jsou obsazeny zde
 * 
 * @author David Krcmar
 * @version 0.0
 */
public class SubOSController
{
	/** KONSTANTA - Uroven, pod kterou kdyz baterka klesne tak robot na to upozorni */
	public static final float BATTERY_THRESHOLD = 8f;
	public static String ERROR_LOG_FILENAME;

	private static Resources _resources;
	
	private static String currentViewName = "NONE";
	
	private static String[] LoadingScreenActions = new String[] { "0:team.hobbyrobot.subos.hardware.RobotHardware:initRobotHardware" };
	
	/** LoadingScreen, ktery inicializuje dulezite veci pro robota (napr. RobotHardware) */
	public static LoadingScreen loadingScreen;

	public static Logger mainLogger;
	public static Logger errorLogger;
	
	/** Ukazuje se uz ze je malo baterky? */
	public static boolean startedBatteryLEDOverriding = false;

	private static Thread subOSBackgroundThread;

	/**
	 * <strong> !! JE POTREBA SPUSTIT HNED NA ZACATKU !! </strong><br>
	 * Pripravi zakladni veci pro beh subOS
	 * 
	 * @param <InfoBarType>    Typ instance InfoBarData
	 * @param hardware         Hardware robot, na kterem subOS bezi
	 * @param infoBarTypeClass Tryda InfoBarData (ziskate pomoci *.class napr:
	 *                         {@link subOS.supportClasses.Defaults.BasicInfoBar}.class
	 * @return Instanci InfoBarData, ktera bezi na subOS
	 * @throws IOException 
	 */
	public static <InfoBarType extends InfoBarData> InfoBarType init(RobotHardware hardware,
		Class<InfoBarType> infoBarTypeClass, Logger logger, String errorLogefileName, String resourcesFilename) throws IOException
	{
		Stopwatch sw = new Stopwatch();
		
		ERROR_LOG_FILENAME = errorLogefileName;
		
		//Inicializuj grafiku
		GraphicsController.init();
		
		initLoggers(logger);
		
		_resources = Resources.loadGlobalResources(resourcesFilename);
		
		//Nastav hardware robots, ktery se bude inicializovat
		RobotHardware.RobotHardwareToInitialize = hardware;

		//Constructor InfoBaru
		Constructor<InfoBarType> infoBarConstructor = null;
		//Vytvor InfoBar
		try
		{
			infoBarConstructor = infoBarTypeClass.getConstructor(RobotHardware.class);
			InfoBarController.infoBarData = infoBarConstructor.newInstance(hardware);
		}
		catch (Exception e)
		{
			ErrorLogging.logFatalError("Error when creating instance of InfoBar!; " + Logger.getExceptionInfo(e));
			ErrorLogging.startErrorLogScreen();
			e.printStackTrace();
		}
		//Inicializuj a spust InfoBar
		InfoBarController.init();
		InfoBarController.start();

		//Vytvor a spust vedlejsi subOS Thread
		subOSBackgroundThread = new Thread()
		{
			@Override
			public void run()
			{
				Stopwatch batterySw = new Stopwatch();
				while (true)
				{
					if (Battery.getVoltage() >= BATTERY_THRESHOLD)
					{
						batterySw.reset();
					}
					if (batterySw.elapsed() > 1000 && !startedBatteryLEDOverriding)
					{
						BrickHardware.setLEDPattern(2, LEDBlinkingStyle.NONE, Integer.MAX_VALUE);
						//GraphicsController.alert = true;
						startedBatteryLEDOverriding = true;
					}
					yield();
				}
			}
		};
		subOSBackgroundThread.setDaemon(true);
		subOSBackgroundThread.setPriority(Thread.MIN_PRIORITY);
		subOSBackgroundThread.start();

		loadingScreen = new LoadingScreen("Init hardware", LoadingScreenActions);
		//Spust LoadingScreen
		loadingScreen.start();

		//Restartuj InfoBar
		InfoBarController.stop();
		try
		{
			infoBarConstructor = infoBarTypeClass.getConstructor(RobotHardware.class);
			InfoBarController.infoBarData = infoBarConstructor.newInstance(hardware);
		}
		catch (Exception e)
		{
			ErrorLogging.logFatalError("Error when creating instance of InfoBar!!;" + Logger.getExceptionInfo(e));
			ErrorLogging.startErrorLogScreen();
			e.printStackTrace();
		}
		InfoBarController.start();

		mainLogger.log("subOS started in " + sw.elapsed() + "ms");
		return (InfoBarType) InfoBarController.infoBarData;
	}

	private static void initLoggers(Logger mainLogger) throws IOException
	{
		SubOSController.mainLogger = mainLogger;

		errorLogger = mainLogger.createSubLogger("ErrorLogger", true);
		File logFile = new File(ERROR_LOG_FILENAME);
		
		if (!logFile.exists())
		{
			try
			{
				logFile.createNewFile();
			}
			catch (IOException e)
			{
				//Pokud se nepovedlo vytvorit soubor -> fatal error
				e.printStackTrace();
				BrickHardware.setLEDPattern(2, LEDBlinkingStyle.DOUBLEBLINK, 2);
			}
		}
		FileWriter fw = new FileWriter(ERROR_LOG_FILENAME, true);
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter pw = new PrintWriter(bw);
		
		errorLogger.registerEndpoint(pw);
	}
	
	public static void setViewName(String name)
	{
		currentViewName = name;
	}
	
	public static String getViewName()
	{
		return currentViewName;
	}
	
	public static Resources getSobOSResources()
	{
		return _resources;
	}
}
