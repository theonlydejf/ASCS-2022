package team.hobbyrobot.ascsvehicle.os;

import java.io.IOException;
import java.util.ArrayList;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.motor.UnregulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.BaseSensor;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.robotics.DirectionFinder;
import lejos.robotics.DirectionFinderAdapter;
import lejos.robotics.EncoderMotor;
import lejos.robotics.Gyroscope;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.localization.CompassPoseProvider;
import lejos.robotics.localization.OdometryPoseProvider;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.ArcRotateMoveController;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.RotateMoveController;
import lejos.utility.Delay;
import lejos.utility.GyroDirectionFinder;
import lejos.utility.Stopwatch;
import team.hobbyrobot.subos.Referenceable;
import team.hobbyrobot.subos.Resources;
import team.hobbyrobot.subos.SubOSController;
import team.hobbyrobot.subos.hardware.BrickHardware;
import team.hobbyrobot.subos.hardware.GyroRobotHardware;
import team.hobbyrobot.subos.hardware.LEDBlinkingStyle;
import team.hobbyrobot.subos.hardware.RobotHardware;
import team.hobbyrobot.subos.hardware.motor.EV3DCMediumRegulatedMotor;
import team.hobbyrobot.subos.hardware.sensor.EV3Gyroscope;
import team.hobbyrobot.ascsvehicle.ASCSVehicle;
import team.hobbyrobot.ascsvehicle.navigation.TDNPoseCorrectionProvider;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.subos.menu.IncludeInRobotInfo;
import team.hobbyrobot.subos.navigation.CompassPilot;
import team.hobbyrobot.subos.navigation.CorrectablePoseProvider;
import team.hobbyrobot.subos.navigation.PoseCorrectionProvider;

public class ASCSVehicleHardware extends GyroRobotHardware
{
	@IncludeInRobotInfo
	public int LifterUp = 0;
	private RotateMoveController _pilot = null;
	private Chassis _chassis = null;
	private EV3Gyroscope _gyro = null;
	private PoseProvider _poseProvider = null;
	private TDNPoseCorrectionProvider _corrector = null;

	public ASCSVehicleHardware(float wheelDistance, float wheelRadius, float wheelMultiplier, boolean reverseLeft, boolean reverseRight)
	{
		super(wheelDistance, wheelRadius, wheelMultiplier, reverseLeft, reverseRight);
		// TODO Auto-generated constructor stub
	}

	public void moveLifterTo(int percent)
	{
		Motor1.rotateTo((int) (LifterUp * (percent / 100f)));
	}

	public void fltLifter()
	{
		Motor1.flt();
	}

	public void stopLifter()
	{
		Motor1.stop();
	}

	@Override
	protected BaseSensor initSensor1(Port port)
	{
		BrickHardware.setLEDPattern(3, LEDBlinkingStyle.NONE, 2);
		EV3GyroSensor sensor = new EV3GyroSensor(SensorPort.S4);
		BrickHardware.releasePriority(2, 2);
		BrickHardware.setLEDPattern(1, LEDBlinkingStyle.DOUBLEBLINK, 2);
		_gyro = new EV3Gyroscope(sensor);
		return sensor;
	}

	@Override
	protected BaseSensor initSensor2(Port port)
	{
		return null;
	}

	@Override
	protected BaseSensor initSensor3(Port port)
	{
		return null;
	}

	@Override
	protected BaseSensor initSensor4(Port port)
	{
		return null;
	}

	@Override
	protected RegulatedMotor initMotor1(Port port)
	{
		EV3DCMediumRegulatedMotor motor = new EV3DCMediumRegulatedMotor(port);
		motor.setRegulatorState(true);
		return motor;
	}

	@Override
	protected RegulatedMotor initMotor2(Port port)
	{
		return null;
	}

	@Override
	protected EncoderMotor initLeftDriveMotor(Port port)
	{
		return new EV3DCMediumRegulatedMotor(port);
	}

	@Override
	protected EncoderMotor initRightDriveMotor(Port port)
	{
		return new EV3DCMediumRegulatedMotor(port);
	}

	@Override
	protected Gyroscope initGyroscope()
	{
		return _gyro;
	}

	@Override
	protected DirectionFinder initDirectionFinder()
	{
		return _gyro;
	}
	
	public void calibrateLifter()
	{
		calibrateLifter(new Referenceable<Float>(0f), new ArrayList<String>());
	}

	public void calibrateLifter(Object _percentage, Object _msgFeed)
	{
		Referenceable<Float> percentage = (Referenceable<Float>) _percentage;
		final ArrayList<String> msgFeed = (ArrayList<String>) _msgFeed;
		// Home lifting mechanism
		SubOSController.mainLogger.log("homing lifter...");
		Motor1.setSpeed(360);
		Motor1.setStallThreshold(5, 200);
		Motor1.forward();
		Stopwatch sw = new Stopwatch();
		while (!Motor1.isStalled() && sw.elapsed() < 750)
			Thread.yield();
		Motor1.flt();
		SubOSController.mainLogger.log("lifter home!");
		Sound.playTone(1000, 50);

		// Wait a bit so motor can move to its resting position
		Delay.msDelay(500);
		Motor1.resetTachoCount();

		percentage.setValue(.3f);

		// Move lifting mechanism to the other side
		SubOSController.mainLogger.log("moving lifter to far end...");
		sw.reset();
		Motor1.backward();
		Delay.msDelay(1000);
		Motor1.flt();
		percentage.setValue(.5f);
		SubOSController.mainLogger.log("lifter at far end!");
		Sound.playTone(1000, 50);

		// Wait a bit so motor can move to its resting position
		SubOSController.mainLogger.log("waiting...");
		Delay.msDelay(500);

		percentage.setValue(.6f);

		// Calculate the middle, ie the highest point of the lifting mechanism
		LifterUp = Motor1.getTachoCount() / 2;
		percentage.setValue(.9f);

		// Home the lifting mechanism
		SubOSController.mainLogger.log("homing lifter...");
		Motor1.rotateTo(0);
		Motor1.flt();

		// Move lifter up and down to make sure it works
		Motor1.setSpeed((int) Motor1.getMaxSpeed());

		percentage.setValue(1f);
		msgFeed.add("lifter calib: done!");
	}

	public static void calibrateDefaultLifter(Object _percentage, Object _msgFeed)
	{
		((ASCSVehicleHardware) RobotHardware.RobotHardwareToInitialize).calibrateLifter(_percentage, _msgFeed);
	}

	public RotateMoveController getPilot()
	{
		if (_pilot == null)
			createPilot();
		return _pilot;
	}

	public Chassis getChassis()
	{
		if(_chassis == null)
			createChassis();
		return _chassis;
	}

	public PoseProvider getPoseProvider()
	{
		if (_poseProvider == null)
		{
		    try
		    {
		        createPoseProvider();		    		        
		    }
		    catch (IOException ex)
		    {
		        _poseProvider = null;
		    }
		}
		return _poseProvider;
	}

	public void resetGyroAt(int angle)
	{
		_gyro.resetAt(angle);
	}
	
	public void startCorrector()
	{
	    _corrector.startServer();
	}
	
    public void stopCorrector() throws IOException
    {
        _corrector.stopServer();
    }
    
    public void setPilotExpectedHeading(int heading)
    {
        ((CompassPilot)_pilot).setExpectedHeading(heading);
    }
	
	private void createChassis()
	{
		Wheel lWheel = WheeledChassis.modelWheel((EV3DCMediumRegulatedMotor) LeftDriveMotor, WheelRadius)
			.offset(WheelDistance / 2f).invert(ReverseLeftMotor);
		Wheel rWheel = WheeledChassis.modelWheel((EV3DCMediumRegulatedMotor) RightDriveMotor, WheelRadius)
			.offset(-WheelDistance / 2f).invert(ReverseRightMotor);
		//TODO
		//pilot = new CompassDifferentialPilot(new Wheel[] { lWheel, rWheel }, _gyro);
		_chassis = new WheeledChassis(new Wheel[] { lWheel, rWheel }, WheeledChassis.TYPE_DIFFERENTIAL);
	}
	
	private void createPilot()
	{
		//pilot = new MovePilot(getChassis());
		_pilot = new CompassPilot(this);
		_pilot.setAngularAcceleration(150);
		_pilot.setLinearAcceleration(100);
	}
	
	private void createPoseProvider() throws IOException
	{
		//_poseProvider = new OdometryPoseProvider(getPilot());
		if (getPilot() != null && getDirectionFinder() != null)
		{
		    PoseProvider main = new CorrectableCompassPoseProvider(getPilot(), _gyro);
		    _corrector = new TDNPoseCorrectionProvider(Resources.global().getInt("servers.poseCorrectionPort"), getPilot(), false, ASCSVehicle.logger, 300, 400);
		    
		    _poseProvider = new CorrectablePoseProvider(main, _corrector);
		}
	}
}
