package team.hobbyrobot.subos.navigation;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.Move.MoveType;
import lejos.robotics.navigation.MoveListener;
import lejos.robotics.navigation.RotateMoveController;
import lejos.utility.Stopwatch;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.subos.SubOSController;
import team.hobbyrobot.subos.hardware.GyroRobotHardware;
import temp.PIDTuner;

/**
 * Implementation of RotateMoveController using {@link team.hobbyrobot.subos.navigation.MoveHandler
 * MoveHandler}.
 * All move processors use gyro from {@link team.hobbyrobot.subos.hardware.GyroRobotHardware
 * GyroRobotHardware} to correct the
 * movement.
 * 
 * @author David Krcmar
 */
public class CompassPilot extends MoveHandler implements RotateMoveController, LimitablePilot
{
	private int _angularSpeed; // %
	private int _linearSpeed; // %
	private int _linearMinSpeed; // %
	private int _angularMinSpeed; // %
	private double _linearAccel; // %/sec
	private double _angularAccel; // %/sec

	private float _expectedHeading;

	private TravelProcessor _travelProcessor;
	private RotateProcessor _rotateProcessor;

	/**
	 * Creates instance of CompassPilot
	 * 
	 * @param hardware The RobotHardware the pilot should control
	 */
	public CompassPilot(GyroRobotHardware hardware)
	{
		super(hardware);

		_rotateProcessor = new RotateProcessor();
		_travelProcessor = new TravelProcessor();
		registerProcessor(MoveType.ROTATE, _rotateProcessor);
		registerProcessor(MoveType.TRAVEL, _travelProcessor);

		_linearSpeed = 100;
		_angularSpeed = 100;

		_linearAccel = 100;
		_angularAccel = 100;

		_linearMinSpeed = 30;
		_angularMinSpeed = 35;

		_expectedHeading = hardware.getAngle();
	}

	private void waitForMoveFinish()
	{
		while (!moveHandler.isMoving())
			Thread.yield();
		while (moveHandler.isMoving())
			Thread.yield();
	}

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
	public void stop()
	{
		moveHandler.stopMove();
	}

	@Override
	public boolean isMoving()
	{
		return moveHandler.isMoving();
	}

	@Override
	public void travel(double distance)
	{
		travel(distance, false);
	}

	@Override
	public void travel(double distance, boolean immediateReturn)
	{
		moveHandler.startNewMove(
			new Move(MoveType.TRAVEL, (float) distance, 0, _linearSpeed, _angularSpeed, moveHandler.isMoving()));
		if (immediateReturn)
			return;

		waitForMoveFinish();
	}

	@Override
	public void setLinearSpeed(double speed)
	{
		_linearSpeed = (int) speed;
	}

	@Override
	public double getLinearSpeed()
	{
		return _linearSpeed;
	}

	@Override
	public double getMaxLinearSpeed()
	{
		return 100;
	}

	@Override
	public void setLinearAcceleration(double acceleration)
	{
		_linearAccel = acceleration;
	}

	@Override
	public double getLinearAcceleration()
	{
		return _linearAccel;
	}

	@Override
	public void rotate(double angle)
	{
		rotate(angle, false);
	}

	@Override
	public void rotate(double angle, boolean immediateReturn)
	{
		moveHandler.startNewMove(
			new Move(MoveType.ROTATE, 0, (float) angle, _linearSpeed, _angularSpeed, moveHandler.isMoving()));
		if (immediateReturn)
			return;

		waitForMoveFinish();
	}

	@Override
	public void setAngularSpeed(double speed)
	{
		_angularSpeed = (int) speed;
	}

	@Override
	public double getAngularSpeed()
	{
		return _angularSpeed;
	}

	@Override
	public double getMaxAngularSpeed()
	{
		return 100;
	}

	@Override
	public void setAngularAcceleration(double acceleration)
	{
		_angularAccel = acceleration;
	}

	@Override
	public double getAngularAcceleration()
	{
		return _angularAccel;
	}

	@Override
	public void rotateRight()
	{
		rotate(Double.NEGATIVE_INFINITY, true);
	}

	@Override
	public void rotateLeft()
	{
		rotate(Double.POSITIVE_INFINITY, true);
	}

	@Override
	public void setTravelLimit(double limit)
	{
		moveHandler.setTravelLimit(limit);
	}

	@Override
	public double getTravelLimit()
	{
		return moveHandler.getTravelLimit();
	}

	@Override
	public boolean wasLastMoveLimited()
	{
		return moveHandler.wasLastMoveLimited();
	}

	@Override
	public void setRotateLimit(double limit)
	{
		moveHandler.setRotateLimit(limit);
	}

	@Override
	public double getRotateLimit()
	{
		return moveHandler.getRotateLimit();
	}

	/**
	 * Gets the current linear minimal speed of the pilot
	 * 
	 * @return The linear minimal speed, the robot is allowed to move at
	 */
	public int getLinearMinSpeed()
	{
		return _linearMinSpeed;
	}

	/**
	 * Sets the current linear minimal speed of the pilot
	 * 
	 * @param minSpeed The linear minimal speed, the robot is allowed to move at
	 */
	public void setLinearMinSpeed(int minSpeed)
	{
		_linearMinSpeed = minSpeed;
	}

	/**
	 * Gets the current angular minimal speed of the pilot
	 * 
	 * @return The angular minimal speed, the robot is allowed to move at
	 */
	public int getAngularMinSpeed()
	{
		return _angularMinSpeed;
	}

	/**
	 * Sets the current angular minimal speed of the pilot
	 * 
	 * @param minSpeed The angular minimal speed, the robot is allowed to move at
	 */
	public void setAngularMinSpeed(int minSpeed)
	{
		_angularMinSpeed = minSpeed;
	}

	/**
	 * Sets the heading, the robot is expected to travel at. The move processor for Travel
	 * then tries to stay at this heading for the duration of the travel. The move processor
	 * for Rotate automatically updates the expected heading.
	 * 
	 * @param heading The heading, robot is expected to travel at
	 */
	public void setExpectedHeading(float heading)
	{
		_expectedHeading = heading;
	}

	/**
	 * Gets the heading, the robot is expected to travel at.
	 * 
	 * @return The heading, robot is expected to travel at
	 */
	public float getExpectedHeading()
	{
		return _expectedHeading;
	}

	private static float normalizeAng(float ang)
	{
		while (ang >= 360)
			ang -= 360;
		while (ang < 0)
			ang += 360;

		return ang;
	}

	//TODO: - Calibrate PID
	//		- Better stopping - do rotate multiple times to ensure 
	//         correct heading (wait between each rotate)
	/**
	 * Implementation of rotate, corrected by a gyroscope
	 * 
	 * @author David Krcmar
	 */
	public class RotateProcessor implements MoveProcessor
	{
		public static final int PID_CONTROL_PERIOD = 30; //ms

		/** Minimal speed, the robot is allowed to rotate at */
		public float minSpeed;
		/** Constant used for accelerating */
		public float accelConst;

		/**
		 * If true, parameters are reseted to defaults from the current instance of
		 * {@link team.hobbyrobot.subos.navigation.CompassPilot CompassPilot} when
		 * the {@link team.hobbyrobot.subos.navigation.MoveProcessor#reset reset()}
		 * method is called
		 */
		public boolean resetParams;

		private PID _pid;
		private Stopwatch _pidSw;
		private Accelerator _accelerator;
		private float _angleRotatedAtMoveStart = 0;

		public RotateProcessor()
		{
			this(true);
		}

		public RotateProcessor(boolean resetParams)
		{
			this.resetParams = resetParams;
			try
			{
				//_pid = new PIDTuner(.8, .0001, 0, 0, 1234);
				//TODO: calibrate and remove tuner
				_pid = PIDTuner.createPIDTunerFromRscs("compassRotate", 1234);
				//_pid.setName("compassRotate");
				//_pid.verbal = true;
				((PIDTuner) _pid).setAutoSave(true);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			_pidSw = new Stopwatch();
			_accelerator = new Accelerator(CompassPilot.this._angularMinSpeed);
		}

		@Override
		public boolean step(Move targetMove)
		{
			// If the robot should rotate without limit -> skip regulation
			if (Double.isInfinite(targetMove.getAngleTurned()))
			{
				controlMotors((int) (targetMove.getRotateSpeed() * Math.signum(targetMove.getAngleTurned())), 100,
					false);
				return false;
			}

			// Angle travelled since the move has started
			float travelledAng = hardware.getAngle() - _angleRotatedAtMoveStart;

			// True, if the move has completed
			boolean rotateCompleted = (targetMove.getAngleTurned() - travelledAng)
				* Math.signum(targetMove.getAngleTurned()) <= 0; //Math.round(travelledAng) == Math.round(targetMove.getAngleTurned());

			// If the move has completed -> set the heading, at which the robot is expected to be
			if (rotateCompleted)
				CompassPilot.this._expectedHeading = normalizeAng(
					_angleRotatedAtMoveStart + targetMove.getAngleTurned());

			// Limit the method to control motors only once per PID_CONTROL_PERIOD miliseconds
			if (_pidSw.elapsed() < PID_CONTROL_PERIOD || rotateCompleted)
				return rotateCompleted;

			_pidSw.reset();

			// Limit the rotate rate pid to the maximum rotate speed
			_pid.setOutputLimits(targetMove.getRotateSpeed());

			// Calculate speed, based on how close the robot is to the target angle,
			// The closer it is - the slower the robot moves
			float decelSpeed = (float) _pid.getOutput(travelledAng, targetMove.getAngleTurned());
			float absDecelSpeed = Math.abs(decelSpeed);

			// If the decel speed is lower then angular min speed -> bound it to the min angular speed
			if (absDecelSpeed < minSpeed)
			{
				decelSpeed = minSpeed * Math.signum(decelSpeed);
				absDecelSpeed = minSpeed;
			}
			// Calculate the accelerating speed
			float currSpeed = (float) _accelerator.getCurrentSpeed(accelConst);

			// If robot should go slower, then the accelerator suggests, use the decelSpeed
			if (absDecelSpeed < currSpeed)
				currSpeed = absDecelSpeed;
			else
				_pid.setErrorSum(0); // If accelerating -> reset integral

			// Update motors
			controlMotors((int) currSpeed, 100 * Math.signum(decelSpeed), true);

			return rotateCompleted;
		}

		@Override
		public void reset()
		{
			resetSteeringControlLoop();

			_angleRotatedAtMoveStart = hardware.getAngle();
			_pid.reset();
			_pidSw.reset();

			if (resetParams)
			{
				_accelerator.reset(_angularMinSpeed);
				minSpeed = CompassPilot.this._angularMinSpeed;
				accelConst = (float) CompassPilot.this._angularAccel;
			}
		}

	}

	/**
	 * Implementation of travel, corrected by a gyroscope
	 * 
	 * @author David Krcmar
	 */
	public class TravelProcessor implements MoveProcessor
	{
		public static final float DECEL_CONSTANT = .25f;
		public static final int PID_CONTROL_PERIOD = 30; //ms

		/** The angle the travel processor tries to travel at */
		public float targetAngle = 0;
		public float normalizedAngleDiff = 0;
		/** The speed the travel starts at */
		public float startSpeed = 0;
		/** The speed the travel ends at */
		public float endSpeed = 0;
		/** The constant used for accelerating */
		public float accelConst = 0;
		/** The constant used for decelerating */
		public float decelConst = 0;

		/**
		 * If true, parameters are reseted to defaults from the current instance of
		 * {@link team.hobbyrobot.subos.navigation.CompassPilot CompassPilot} when
		 * the {@link team.hobbyrobot.subos.navigation.MoveProcessor#reset reset()}
		 * method is called
		 */
		public boolean resetParams = true;

		private PID _pid;
		private float _distanceTravelledAtMoveStart = 0;
		private Accelerator _accelerator;
		private Stopwatch _pidSw;
		private double _currPIDRate;

		public TravelProcessor()
		{
			this(true);
		}

		public TravelProcessor(boolean resetParams)
		{
			this.resetParams = resetParams;
			_pid = null;
			try
			{
				//_pid = new PIDTuner(1.15f, .01f, 1.0f, 0, 1235);
				//_pid = new PIDTuner(.5f, 0, 0, 0, 1235);
				//TODO: remove tuner
				_pid = PIDTuner.createPIDTunerFromRscs("compassTravel", 1235);
				//_pid.setName("compassTravel");
				((PIDTuner) _pid).setAutoSave(true);
				_pid.setOutputLimits(100);
				_pid.setMaxIOutput(50);
				//_pid.verbal = true;
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			_pidSw = new Stopwatch();
			_accelerator = new Accelerator(CompassPilot.this._linearAccel);
		}

		@Override
		public boolean step(Move targetMove)
		{
			// Get target distance components
			float travelTarget = targetMove.getDistanceTraveled();
			float absTravelTarget = Math.abs(travelTarget);
			int sgnTravelTarget = (int) Math.signum(travelTarget);

			// Calculate remaining distance
			float travelled = hardware.getDrivenDist() - _distanceTravelledAtMoveStart;
			float absTravelled = Math.abs(travelled);
			float distanceRemaining = absTravelTarget - absTravelled;

			boolean travelCompleted = distanceRemaining <= 0;

			// Limit the method to control motors only once per PID_CONTROL_PERIOD miliseconds
			if (_pidSw.elapsed() < PID_CONTROL_PERIOD || travelCompleted)
				return travelCompleted;
			_pidSw.reset();

			// Calculate current deceleration speed
			float decelSpeed = distanceRemaining * decelConst + endSpeed;
			// Clamp current decel speed to min and max speed
			if (decelSpeed > targetMove.getTravelSpeed())
				decelSpeed = targetMove.getTravelSpeed();
			else if (decelSpeed < endSpeed)
				decelSpeed = endSpeed;

			// Calculate current acceleration speed
			float currSpeed = (float) _accelerator.getCurrentSpeed(accelConst);

			// If robot should start decelerating to get to target distance smoothly, use decel speed
			// Accel speed does not have to be clamped, beacuse min speed is set in when initing
			// Accelerator and max speed is clamped by decel speed
			if (decelSpeed < currSpeed)
				currSpeed = decelSpeed;

			// Calculate regulation values from current angle and target angle
			float currAng = hardware.getAngle();// - normalizedAngleDiff;
			_currPIDRate = _pid.getOutput(currAng, targetAngle);
			//Logger.main.log("travelling at angle: " + currAng + "\t target: " + targetAngle);

			controlMotors((int) currSpeed * sgnTravelTarget, _currPIDRate * sgnTravelTarget, false);

			return travelCompleted;
		}

		@Override
		public void reset()
		{
			resetSteeringControlLoop();

			_distanceTravelledAtMoveStart = hardware.getDrivenDist();
			//Logger.main.log("Travelling at expected heading: " + CompassPilot.this._expectedHeading);
			_pidSw.reset();
			_pid.reset();
			_currPIDRate = 0;

			if (resetParams)
			{
				targetAngle = CompassPilot.this._expectedHeading;
				//normalizedAngleDiff = hardware.getAngle() - normalizeAng(hardware.getAngle());
				targetAngle = getTargetAng(CompassPilot.this.hardware.getAngle(), CompassPilot.this._expectedHeading);
				//float startError = normalizedAngleDiff - hardware.getAngle();

				startSpeed = CompassPilot.this._linearMinSpeed;
				endSpeed = CompassPilot.this._linearMinSpeed;

				accelConst = (float) CompassPilot.this._linearAccel;
				decelConst = DECEL_CONSTANT;
				_accelerator.reset(startSpeed);
			}

		}

		// TODO zkontrolovat
		/**
		 * Calculates the target angle, NOT HEADING, at which the robot should travel. 
		 * It takes in consideration the current angle of the robot and the expected 
		 * heading and calculates the closest angle to the curRaw with the same 
		 * heading as the target. Eg:<br>
		 * curRaw=361,targetCartesian=0 => 360<br>
		 * curRaw=-95,targetCartesian=270 => -90
		 * 
		 * @param curRaw The angle, the robot is currently rotated
		 * @param targetCartesian The heading, for which the closest angle should be calculated
		 * @return The angle closest to curRaw, but with the same heading as targetCartesian
		 */
		private float getTargetAng(float curRaw, float targetCartesian)
		{
			float times360 = 0;
			float adjustedAng = curRaw;
			if (curRaw < 0)
			{
				while (adjustedAng < 0)
				{
					adjustedAng += 360;
					times360++;
				}
			}

			int remain = (int)(adjustedAng / 360);
			float newAng = remain * 360 + targetCartesian - times360 * 360;

			if (curRaw - newAng > 180)
			{
				newAng += 360;
			}

			return newAng;
		}

	}
}
