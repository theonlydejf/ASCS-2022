package team.hobbyrobot.ascsvehicle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map.Entry;

import lejos.hardware.Battery;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.RotateMoveController;
import lejos.utility.Delay;
import team.hobbyrobot.ascsvehicle.api.services.MovementService;
import team.hobbyrobot.ascsvehicle.api.services.TestService;
import team.hobbyrobot.ascsvehicle.navigation.TDNPoseCorrectionProvider;
import team.hobbyrobot.ascsvehicle.navigation.TDNPoseCorrectionProviderListener;
import team.hobbyrobot.ascsvehicle.os.APIStaticFactory;
import team.hobbyrobot.ascsvehicle.os.ASCSVehicleHardware;
import team.hobbyrobot.ascsvehicle.os.VehicleInfoBar;
import team.hobbyrobot.subos.LoadingScreen;
import team.hobbyrobot.subos.Resources;
import team.hobbyrobot.subos.SubOSController;
import team.hobbyrobot.subos.errorhandling.ErrorLogging;
import team.hobbyrobot.subos.graphics.GraphicsController;
import team.hobbyrobot.subos.graphics.infobar.BasicInfoBar;
import team.hobbyrobot.subos.hardware.BrickHardware;
import team.hobbyrobot.subos.hardware.LEDBlinkingStyle;
import team.hobbyrobot.subos.hardware.motor.EV3DCMediumRegulatedMotor;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.logging.SocketLoggerEndpointRegisterer;
import team.hobbyrobot.logging.VerbosityLogger;
import team.hobbyrobot.subos.menu.MenuItem;
import team.hobbyrobot.subos.menu.MenuScreen;
import team.hobbyrobot.subos.menu.RobotInfoScreen;
import team.hobbyrobot.subos.navigation.CorrectablePoseProvider;
import team.hobbyrobot.subos.navigation.PoseCorrectionProvider;
import team.hobbyrobot.net.api.TDNAPIServer;
import team.hobbyrobot.tdn.base.*;
import team.hobbyrobot.tdn.core.*;

public class ASCSVehicle implements TDNPoseCorrectionProviderListener
{
	//@formatter:off
	public static final MenuItem[] MainMenu = new MenuItem[] 
	{
		new RobotInfoScreen(ASCSVehicleHardware.class)
	};
	//@formatter:on

	/**  Inicializovany Hardware robota */
	public static ASCSVehicleHardware Hardware = new ASCSVehicleHardware(111.7f, 56f / 2f, true, false);
	/** Inicializovany InfoBar, ktery aktualne bezi */
	public static BasicInfoBar InfoBar = null;

	public static Logger logger;

	public static TDNAPIServer api = null;
	
	public static TDNPoseCorrectionProvider _correctionProvider = null;
	
	public static void main(String[] args) throws Exception
	{
		// Starts main logger
		logger = new Logger();
		Logger.main = logger;
		SocketLoggerEndpointRegisterer loggerRegisterer = new SocketLoggerEndpointRegisterer(logger, 1111);
		loggerRegisterer.startRegisteringClients();
		//SubOSController.LoadingScreenActions.add("0:team.hobbyrobot.ascsvehicle.ASCSVehicleHardware:calibrateDefaultLifter");

		// Starts subOS
		InfoBar = SubOSController.init(Hardware, VehicleInfoBar.class, logger, "error_log.txt", "ascsVehicle.rscs");
		Resources.global().setInt("servers.APIPort", 2222);
		Resources.global().setInt("servers.poseCorrectionPort", 3333);
		initVehicle();
		BrickHardware.releasePriority(2, 0);
		//Dej najevo, že robot už je připraven k použití
		BrickHardware.setLEDPattern(1, LEDBlinkingStyle.NONE, 0);
		Sound.beepSequenceUp();
		
		_correctionProvider = (TDNPoseCorrectionProvider) ((CorrectablePoseProvider)Hardware.getPoseProvider()).getCorrectionProvider();
		_correctionProvider.addListener(new ASCSVehicle());
		
		RotateMoveController pilot = Hardware.getPilot();
		Hardware.resetDriveMotorsTachos();
		
		//pilot.forward();
		//Button.waitForAnyPress();
		
		/*boolean b = true;
		while(b)
		{
			Sound.playTone(600, 150);
			Button.waitForAnyPress();
			Hardware.resetGyroAt(0);
			pilot.travel(1000, true);
			
			while(Button.getButtons() != 0) ; 

			while(pilot.isMoving())
			{
				if(Button.getButtons() != 0)
					pilot.stop();					
			}
			while(Button.getButtons() != 0) ;
			
			//pilot.rotate(90);
			//Sound.twoBeeps();
		}*/
				
		api.setVerbosity(VerbosityLogger.OVERVIEW);
		
		//Spust menu a opakuj ho do nekonecna
		GraphicsLCD g = GraphicsController.getNewDefaultMainGraphics();
		g.clear();

		GraphicsController.refreshScreen();

		logger.log("Waiting to start corrector...");
		Hardware.startCorrector();
		
		PoseProvider poseProvider = Hardware.getPoseProvider();
		while (true)
		{
			Button.waitForAnyPress();
			//Pose pose = poseProvider.getPose();
			//logger.log("Current pose: " + pose.toString());
			Hardware.resetGyroAt(0);
			pilot.rotate(90);
			Sound.beep();
			//Hardware.moveLifterTo(100);
			//Button.waitForAnyPress();
			//Hardware.moveLifterTo(0);
		}
	}

	private static void initVehicle()
	{
		APIStaticFactory.setInfoLogger(logger);
		APIStaticFactory.setVerbosity(VerbosityLogger.DEBUGGING);
		
		APIStaticFactory.reset();
		APIStaticFactory.setAPILogger(logger);
		APIStaticFactory.setPort(Resources.global().getInt("servers.APIPort"));
		APIStaticFactory.settings.setStartServer(true);
		APIStaticFactory.settings.setStartRegisteringClients(true);
		APIStaticFactory.queueService("TestService", new TestService());
		APIStaticFactory.queueService("MovementService", new MovementService(Hardware, logger));
		
		// Calibrate robot
		new LoadingScreen("Init vehicle",
			new String[] { "0:team.hobbyrobot.ascsvehicle.os.ASCSVehicleHardware:calibrateDefaultLifter",
					"1:team.hobbyrobot.ascsvehicle.os.APIStaticFactory:createAPI"}).start();
		
		/*new LoadingScreen("Starting API", 
			new String[] {  }).start();*/
		api = APIStaticFactory.getLastAPIServer();
	}

	public static void logRoot(TDNRoot root, Logger logger)
	{
		logger.log("(");
		StringBuilder sb = new StringBuilder();
		for (Entry<String, TDNValue> val : root)
		{
			sb.append(val.getKey());
			sb.append(": ");
			if (val.getValue().value instanceof TDNRoot)
			{
				logRoot((TDNRoot) val.getValue().value, logger);
				continue;
			}
			if (val.getValue().value instanceof TDNArray)
			{
				logger.log(sb.toString() + "[");
				sb = new StringBuilder();
				TDNArray arr = (TDNArray) val.getValue().value;
				for (Object item : arr)
				{
					sb.append(",");
					if (arr.itemParser.typeKey().equals(new TDNRootParser().typeKey()))
						logRoot((TDNRoot) item, logger);
					else
					{
						logger.log(sb.toString() + item);
						sb = new StringBuilder();
					}
					continue;
				}
				logger.log(sb.toString() + "]");
				sb = new StringBuilder();
				continue;
			}
			logger.log(sb.toString() + val.getValue().value);
			sb = new StringBuilder();
		}
		logger.log(sb.toString() + ")");
	}

	boolean updateExpectedHeading = true;
    @Override
    public void correctorConnected() 
    {
    }

    @Override
    public void correctionReceived(Pose newPose) 
    {
        if(updateExpectedHeading)
        {
            logger.log("setting expected heading...");
            Hardware.setPilotExpectedHeading(Math.round(newPose.getHeading()));
            updateExpectedHeading = false;
            _correctionProvider.removeListener(this);
        }
    }

}
