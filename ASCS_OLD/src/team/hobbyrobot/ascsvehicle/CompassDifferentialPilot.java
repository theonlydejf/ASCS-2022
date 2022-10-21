package team.hobbyrobot.ascsvehicle;

import java.util.ArrayList;

import lejos.hardware.motor.MotorRegulator;
import lejos.internal.ev3.EV3MotorPort.EV3MotorRegulatorKernelModule;
import lejos.robotics.DirectionFinder;
import lejos.robotics.Gyroscope;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.ArcRotateMoveController;
import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.MoveListener;
import lejos.utility.Stopwatch;
import team.hobbyrobot.subos.SubOSController;
import team.hobbyrobot.subos.hardware.motor.EV3DCMediumRegulatedMotor;

@Deprecated
public class CompassDifferentialPilot implements ArcRotateMoveController
{
	private double minRadius = 0;
	final private WheeledChassisStateGetter chassis;
	private ArrayList<MoveListener> _listeners = new ArrayList<MoveListener>();
	private double linearSpeed;
	private double linearAcceleration;
	private double angularAcceleration;
	private double angularSpeed;
	private Monitor _monitor;
	private boolean _moveActive = false;
	private Move move = null;
	private boolean _replaceMove = false;
	private Gyroscope _gyroscope;
	private float _angleAtMoveStart = 0;

	public CompassDifferentialPilot(Wheel[] wheels, Gyroscope gyroscope)
	{
		_gyroscope = gyroscope;
		this.chassis = new WheeledChassisStateGetter(wheels, WheeledChassis.TYPE_DIFFERENTIAL);
		linearSpeed = chassis.getMaxLinearSpeed() * 0.8;
		angularSpeed = chassis.getMaxAngularSpeed() * 0.8;
		chassis.setSpeed(linearSpeed, this.angularSpeed);
		linearAcceleration = getLinearSpeed() * 4;
		angularAcceleration = getAngularSpeed() * 4;
		chassis.setAcceleration(linearAcceleration, angularAcceleration);
		minRadius = chassis.getMinRadius();
		_monitor = new Monitor();
		_monitor.start();

	}

	// Getters and setters of dynamics

	public Chassis getChassis()
	{
		return chassis;
	}

	@Override
	public void setLinearAcceleration(double acceleration)
	{
		linearAcceleration = acceleration;
		chassis.setAcceleration(linearAcceleration, angularAcceleration);
	}

	@Override
	public double getLinearAcceleration()
	{
		return linearAcceleration;
	}

	@Override
	public void setAngularAcceleration(double acceleration)
	{
		angularAcceleration = acceleration;
		chassis.setAcceleration(linearAcceleration, angularAcceleration);
	}

	@Override
	public double getAngularAcceleration()
	{
		return angularAcceleration;
	}

	@Override
	public void setLinearSpeed(double speed)
	{
		linearSpeed = speed;
		chassis.setSpeed(linearSpeed, angularSpeed);
	}

	@Override
	public double getLinearSpeed()
	{
		return linearSpeed;
	}

	@Override
	public double getMaxLinearSpeed()
	{
		return chassis.getMaxLinearSpeed();
	}

	@Override
	public void setAngularSpeed(double speed)
	{
		angularSpeed = speed;
		chassis.setSpeed(linearSpeed, angularSpeed);
	}

	@Override
	public double getAngularSpeed()
	{
		return angularSpeed;
	}

	@Override
	public double getMaxAngularSpeed()
	{
		return chassis.getMaxAngularSpeed();
	}

	@Override
	public double getMinRadius()
	{
		return minRadius;
	}

	@Override
	public void setMinRadius(double radius)
	{
		minRadius = radius;
	}

	// Moves of the travel family

	@Override
	public void forward()
	{
		travel(Double.POSITIVE_INFINITY, true);

	}

	@Override
	public void backward()
	{
		travel(Double.NEGATIVE_INFINITY, true);
	}

	@Override
	public void travel(double distance)
	{
		travel(distance, false);

	}

	@Override
	public void travel(double distance, boolean immediateReturn)
	{
		if (_moveActive)
			stop();
		move = new Move(Move.MoveType.TRAVEL, (float) distance, 0, (float) linearSpeed, (float) angularSpeed,
			chassis.isMoving());
		chassis.moveStart();
		chassis.travel(distance);
		movementStart(immediateReturn);
	}

	// Moves of the Arc family

	@Override
	public void arcForward(double radius)
	{
		arc(radius, Double.POSITIVE_INFINITY, true);
	}

	@Override
	public void arcBackward(double radius)
	{
		arc(radius, Double.NEGATIVE_INFINITY, true);
	}

	@Override
	public void arc(double radius, double angle)
	{
		arc(radius, angle, false);
	}

	@Override
	public void travelArc(double radius, double distance)
	{
		travelArc(radius, distance, false);
	}

	@Override
	public void travelArc(double radius, double distance, boolean immediateReturn)
	{
		arc(radius, distance / (2 * Math.PI), immediateReturn);
	}

	@Override
	public void rotate(double angle)
	{
		rotate(angle, false);
	}

	@Override
	public void rotate(double angle, boolean immediateReturn)
	{
		//TODO
		if (_moveActive)
			stop();

		move = new Move(Move.MoveType.ROTATE, (float) 0, (float) angle, (float) linearSpeed, (float) angularSpeed,
			chassis.isMoving());
		chassis.moveStart();
		Rotator rotator = new Rotator(_gyroscope.getAngle() + angle);
		rotator.start();
		movementStart(immediateReturn);
	}

	private class Rotator extends Thread
	{
		private double _target;

		public Rotator(double target)
		{
			_target = target;
		}

		@Override
		public void run()
		{
			chassis.rotate(_target);

			SubOSController.mainLogger.log("Waiting for update");
			Stopwatch sw = new Stopwatch();
			while (chassis.isMoving() && !_replaceMove)
			{
				if(sw.elapsed() < 100)
					continue;
				
				sw.reset();
				double error = _target - _gyroscope.getAngle();
				SubOSController.mainLogger.log("turn err: " + error);

				if (Math.abs(error) < 1)
					break;

				chassis.rotate(error);
				SubOSController.mainLogger.log("Waiting for update");
			}
			SubOSController.mainLogger.log("end");
			chassis.stop();
		}
	}

	public void rotateLeft()
	{
		rotate(Double.POSITIVE_INFINITY, true);
	}

	public void rotateRight()
	{
		rotate(Double.NEGATIVE_INFINITY, true);
	}

	@Override
	public void arc(double radius, double angle, boolean immediateReturn)
	{
		if (Math.abs(radius) < minRadius)
		{
			throw new RuntimeException("Turn radius too small.");
		}
		if (_moveActive)
		{
			stop();
		}
		if (radius == 0)
		{
			move = new Move(Move.MoveType.ROTATE, 0, (float) angle, (float) linearSpeed, (float) angularSpeed,
				chassis.isMoving());
		}
		else
		{
			move = new Move(Move.MoveType.ARC, (float) (Math.toRadians(angle) * radius), (float) angle,
				(float) linearSpeed, (float) angularSpeed, chassis.isMoving());
		}
		chassis.moveStart();
		chassis.arc(radius, angle);
		movementStart(immediateReturn);
	}

	// Stops. Stops must be blocking!

	@Override
	public void stop()
	{
		// This method must be blocking
		chassis.stop();
		_replaceMove = true;
		while (_moveActive)
			Thread.yield();
	}

	// State
	@Override
	public boolean isMoving()
	{
		return chassis.isMoving();
	}

	// Methods dealing the start and end of a move
	private void movementStart(boolean immediateReturn)
	{
		for (MoveListener ml : _listeners)
			ml.moveStarted(move, this);
		_angleAtMoveStart = _gyroscope.getAngle();
		_moveActive = true;
		synchronized (_monitor)
		{
			_monitor.notifyAll();
		}
		if (immediateReturn)
			return;
		while (_moveActive)
			Thread.yield();
	}

	private void movementStop()
	{
		if (!_listeners.isEmpty())
		{
			chassis.getDisplacement(move);
			move.setValues(move.getMoveType(), move.getDistanceTraveled(), _gyroscope.getAngle() - _angleAtMoveStart,
				chassis.isMoving());
			for (MoveListener ml : _listeners)
				ml.moveStopped(move, this);
		}
		_moveActive = false;
	}

	@Override
	public Move getMovement()
	{
		if (_moveActive)
		{
			return chassis.getDisplacement(move);
		}
		else
		{
			return new Move(Move.MoveType.STOP, 0, 0, false);
		}
	}

	@Override
	public void addMoveListener(MoveListener listener)
	{
		_listeners.add(listener);

	}

	/**
	 * The monitor class detects end-of-move situations when non blocking move
	 * call were made and makes sure these are dealt with.
	 */
	private class Monitor extends Thread
	{
		public boolean more = true;

		public Monitor()
		{
			setDaemon(true);
		}

		public synchronized void run()
		{
			while (more)
			{
				if (_moveActive)
				{
					if (chassis.isStalled())
						CompassDifferentialPilot.this.stop();
					if (!chassis.isMoving())
					{
						movementStop();
						_moveActive = false;
						_replaceMove = false;
					}
				}
				// wait for an event
				try
				{
					wait(_moveActive ? 1 : 100);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	protected static class WheeledChassisStateGetter extends WheeledChassis
	{
		public static final int STATE_IDLE = 0;
		public static final int STATE_STALL = 1;
		public static final int STATE_HOLD = 2;
		public static final int STATE_START = 3;
		public static final int STATE_ACCEL = 4;
		public static final int STATE_MOVE = 5;
		public static final int STATE_DECEL = 6;
		public static final int STATE_UNKNOWN = 7;

		private EV3DCMediumRegulatedMotor castMaster;

		public WheeledChassisStateGetter(Wheel[] wheels, int dim)
		{
			super(wheels, dim);
			if (master instanceof EV3DCMediumRegulatedMotor)
				castMaster = (EV3DCMediumRegulatedMotor) master;
			else
				castMaster = null;
		}

		public int getMasterState()
		{
			if (castMaster == null)
				return WheeledChassisStateGetter.STATE_UNKNOWN;

			return 7;//castMaster.getRegulatorState();
		}
	}

}
