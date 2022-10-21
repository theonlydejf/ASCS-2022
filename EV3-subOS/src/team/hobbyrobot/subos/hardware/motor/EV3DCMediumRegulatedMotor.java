package team.hobbyrobot.subos.hardware.motor;

import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.hardware.motor.MotorRegulator;
import lejos.hardware.port.BasicMotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.TachoMotorPort;
import lejos.hardware.sensor.EV3SensorConstants;
import lejos.internal.ev3.EV3MotorPort;
import lejos.internal.ev3.EV3MotorPort.EV3MotorRegulatorKernelModule;
import lejos.robotics.EncoderMotor;
import team.hobbyrobot.subos.SubOSController;

/**
 * Abstraction for a Medium Lego EV3/NXT motor.
 */
@SuppressWarnings("restriction")
public class EV3DCMediumRegulatedMotor extends BaseRegulatedMotor implements EncoderMotor
{
	protected static int INVALID_MODE = -1;
	protected int mode = INVALID_MODE;

	public static float MOVE_P = 8f;
	public static float MOVE_I = 0.04f;
	public static float MOVE_D = 8f;
	public static float HOLD_P = 8f;
	public static float HOLD_I = 0.02f;
	public static float HOLD_D = 0f;
	public static int OFFSET = 1000;

	private static final int MAX_SPEED = 260 * 360 / 60;

	private int power;
	private boolean regulate = false;

	/**
	 * Use this constructor to assign a variable of type motor connected to a particular port.
	 * 
	 * @param port to which this motor is connected
	 */
	private EV3DCMediumRegulatedMotor(TachoMotorPort port)
	{
		//(port instanceof EV3MotorPort) ? new MotorRegulatorStateGetter((EV3MotorPort)port, port) : 
		super(port, null, EV3SensorConstants.TYPE_MINITACHO, MOVE_P, MOVE_I, MOVE_D, HOLD_P, HOLD_I, HOLD_D, OFFSET,
			MAX_SPEED);
	}

	/**
	 * Use this constructor to assign a variable of type motor connected to a particular port.
	 * 
	 * @param port to which this motor is connected
	 */
	public EV3DCMediumRegulatedMotor(Port port)
	{
		this(port.open(TachoMotorPort.class));
		releaseOnClose(tachoPort);
	}

	@Override
	public void backward()
	{
		if (regulate)
			super.backward();
		else
			tachoPort.controlMotor(power, TachoMotorPort.BACKWARD);
	}

	@Override
	public void forward()
	{
		if (regulate)
			super.forward();
		else
			tachoPort.controlMotor(power, TachoMotorPort.FORWARD);
	}

	@Override
	public void setPower(int power)
	{
		this.power = power;
		setSpeed(getMaxSpeed() * (power / 100f));
	}

	@Override
	public int getPower()
	{
		return power;
	}

	public MotorRegulator getRegulator()
	{
		return tachoPort.getRegulator();
	}

	public void setRegulatorState(boolean regulate)
	{
		this.regulate = regulate;
		if(!regulate)
			suspendRegulation();
	}
	
	@Override
	public void stop()
	{
		if (regulate)
			super.forward();
		else
			tachoPort.controlMotor(power, TachoMotorPort.STOP);
	}

	@Override
	public void flt()
	{
		if (regulate)
			super.flt();
		else
			tachoPort.controlMotor(power, TachoMotorPort.FLOAT);
	}

	@Override
	public boolean isMoving()
	{
		if(regulate)
			return super.isMoving();
		
		return (mode == BasicMotorPort.FORWARD || mode == BasicMotorPort.BACKWARD);
	}

	protected void updateState(int newMode)
	{
		if (newMode == mode)
			return;
		mode = newMode;
		tachoPort.controlMotor(power, newMode);
	}
	
    public int getTachoCount()
    {
    	if(regulate)
    		return super.getTachoCount();
    	
        return tachoPort.getTachoCount();
    }

    public void resetTachoCount()
    {
    	if(regulate)
    		super.resetTachoCount();
    	else
    		tachoPort.resetTachoCount();
    }
}
