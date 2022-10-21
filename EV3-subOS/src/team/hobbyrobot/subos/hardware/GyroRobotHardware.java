package team.hobbyrobot.subos.hardware;

import lejos.hardware.port.Port;
import lejos.hardware.sensor.BaseSensor;
import lejos.robotics.DirectionFinder;
import lejos.robotics.EncoderMotor;
import lejos.robotics.Gyroscope;
import lejos.robotics.RegulatedMotor;
import team.hobbyrobot.subos.hardware.sensor.EV3Gyroscope;

public abstract class GyroRobotHardware extends RobotHardware
{
	private Gyroscope _gyro = null;
	private DirectionFinder _directionFinder = null;
	
	public GyroRobotHardware(float wheelDistance, float wheelRadius, boolean reverseLeft, boolean reverseRight)
	{
		super(wheelDistance, wheelRadius, reverseLeft, reverseRight);
		// TODO Auto-generated constructor stub
	}

	public Gyroscope getGyroscope()
	{
		return _gyro;
	}
	
	public DirectionFinder getDirectionFinder()
	{
		return _directionFinder;
	}
	
	protected abstract Gyroscope initGyroscope();
	protected abstract DirectionFinder initDirectionFinder();
	
	@Override
	protected void afterInit()
	{
		super.afterInit();
		
		_gyro = initGyroscope();
		_directionFinder = initDirectionFinder();
	}
	
	public float getAngle()
	{
		return _gyro.getAngle();
	}
	
	public float getHeading()
	{
		return _directionFinder.getDegreesCartesian();
	}
	
	public void resetHeading()
	{
		_gyro.reset();
		_directionFinder.resetCartesianZero();
	}
}
