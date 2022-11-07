package team.hobbyrobot.subos.hardware.sensor;

import lejos.robotics.DirectionFinder;
import lejos.robotics.Gyroscope;
import lejos.robotics.SampleProvider;
import team.hobbyrobot.subos.SubOSController;
import lejos.hardware.sensor.EV3GyroSensor;

public class EV3Gyroscope implements Gyroscope, SampleProvider, DirectionFinder
{
	private float[] _lastSample;
	private SampleProvider _gyroProvider;
	private EV3GyroSensor _sensor;
	private int _angleZero = 0;

	public EV3Gyroscope(EV3GyroSensor sensor)
	{
		_sensor = sensor;
		_gyroProvider = sensor.getAngleAndRateMode();
		_lastSample = new float[_gyroProvider.sampleSize()];
	}

	private void fetchSample()
	{
		_gyroProvider.fetchSample(_lastSample, 0);
	}

	@Override
	public float getAngularVelocity()
	{
		synchronized (_lastSample)
		{
			fetchSample();
			return _lastSample[1];
		}
	}

	@Override
	public void recalibrateOffset()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getAngle()
	{
		synchronized (_lastSample)
		{
			fetchSample();
			return (int) _lastSample[0] - _angleZero;
		}
	}

	@Override
	public void reset()
	{
		SubOSController.mainLogger.log("reseting gyro");
		_angleZero = getAngle();
	}

	public void resetAt(int angle)
	{
		synchronized (_lastSample)
		{
			fetchSample();
			_angleZero = (int) (_lastSample[0] - angle);
		}
	}

	@Override
	public int sampleSize()
	{
		return 1;
	}

	@Override
	public void fetchSample(float[] sample, int offset)
	{
		sample[offset] = getAngle();
	}

	@Override
	public void startCalibration()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void stopCalibration()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public float getDegreesCartesian()
	{
		return normalizeAng(getAngle());
	}

	@Override
	public void resetCartesianZero()
	{
		reset();
	}

	   private static float normalizeAng(float ang)
	    {
	        while(ang >= 360)
	            ang -= 360;
	        while(ang < 0)
	            ang += 360;
	        
	        return ang;
	    }
}
