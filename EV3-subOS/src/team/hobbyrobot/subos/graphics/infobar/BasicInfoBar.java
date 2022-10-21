package team.hobbyrobot.subos.graphics.infobar;

import java.text.DecimalFormat;

import lejos.hardware.Battery;
import lejos.hardware.sensor.BaseSensor;
import lejos.robotics.SampleProvider;
import team.hobbyrobot.subos.SubOSController;
import team.hobbyrobot.subos.hardware.RobotHardware;

/**
 * Zakladni data pro InfoBar<br>
 * <strong>InfoBar Potom vypada takto:</strong><br>
 * {@code /--------------------------------------------------\}<br>
 * {@code |G: <Hodnota senzoru> <var:MiddleText> B: <Baterka>|}
 * 
 * @author David Krcmar
 * @version 1.0
 */
public class BasicInfoBar implements InfoBarData
{
	/**
	 * 0-based index senzoru, ktery se bude zobrazovat<br>
	 * S1 - 0<br>
	 * S2 - 1<br>
	 * S3 - 2<br>
	 * S4 - 3<br>
	 */
	public static final int SENSOR_INDEX = 0;

	public static final char SENSOR_PREFIX = 'G';

	/** Senzory robota */
	protected SampleProvider[] _hardwareInstances;

	/**
	 * Vytvori instanci
	 * 
	 * @param Hardware Hardware robota, o kterem InfoBar je
	 */
	public BasicInfoBar(RobotHardware hardware)
	{
		
		SampleProvider[] sensors = hardware.getSensors();
		SampleProvider[] motors = hardware.getEncoders();
		
		_hardwareInstances = new SampleProvider[sensors.length + motors.length];
		System.arraycopy(sensors, 0, _hardwareInstances, 0, sensors.length);
		System.arraycopy(motors, 0, _hardwareInstances, sensors.length, motors.length);
	}

	@Override
	public String getLeftText()
	{
		String data = "NULL";
		//Senzor ze ktereho se bude cist
		SampleProvider sensor = _hardwareInstances[SENSOR_INDEX];

		if (sensor != null)
		{
			//Ziskej hodnotu ze senzoru
			float[] sample = new float[sensor.sampleSize()];
			sensor.fetchSample(sample, 0);

			//Uloz do promenne data data ze senzoru
			DecimalFormat df = new DecimalFormat("#");
			data = df.format(sample[0]);
			//data = df.format(((PreciseRobotHardware)Hardware).getGyroAngle());
		}

		return String.valueOf(SENSOR_PREFIX) + ": " + data;
	}

	@Override
	public String getMiddleText()
	{
		return SubOSController.getViewName();
	}

	@Override
	public String getRightText()
	{
		DecimalFormat df = new DecimalFormat("#.##");
		return "B: " + df.format(Battery.getVoltage());
	}
}
