package team.hobbyrobot.subos.hardware;

import java.util.ArrayList;

import lejos.hardware.Sound;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.*;
import lejos.robotics.*;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.RotateMoveController;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.subos.Referenceable;
import team.hobbyrobot.subos.SystemSound;
import team.hobbyrobot.subos.errorhandling.ErrorLogging;
import team.hobbyrobot.subos.menu.IncludeInRobotInfo;

/**
 * Typ ktery obsahuje veskera data o robotovy
 * 
 * @author David Krcmar
 * @version 1.1
 */
public abstract class RobotHardware
{
	/** KONSTANTA - Vzdalenost mezi stredy kol */
	@IncludeInRobotInfo
	public float WheelDistance = -1f;
	/** KONSTANTA - Polomer kol */
	@IncludeInRobotInfo
	public float WheelRadius = -1f;
	/** KONSTANTA - Touto konstantou jsou vynasobeny vsechny vzdalenosti ktere robot ujel */
	@IncludeInRobotInfo
	public float DistanceMultiplyer;

	/** Promenna, ktera odkazuje na urcity senzor */
	public BaseSensor Sensor1 = null, Sensor2 = null, Sensor3 = null, Sensor4 = null;

	/** Promenna, ktera odkazuje na motory, se kteryma se jezdi */
	public EncoderMotor LeftDriveMotor = null, RightDriveMotor = null;

	/** Promenna, odkazujici na regulovane motory, ktere se daji pouzit na neo jineho (nastavce...) */
	public RegulatedMotor Motor1 = null, Motor2 = null;

	@IncludeInRobotInfo
	public boolean ReverseLeftMotor;
	@IncludeInRobotInfo
	public boolean ReverseRightMotor;
	
	public float getDistanceMultiplier()
	{
		return WheelRadius * (float)Math.PI * 2;
	}
	
	public int getLeftMotorDirection()
	{
		if(ReverseLeftMotor)
			return -1;
		return 1;
	}
	
	public int getRightMotorDirection()
	{
		if(ReverseRightMotor)
			return -1;
		return 1;
	}
	
	/**
	 * Indexy senzoru a motoru, ktere se <strong>nemaji</strong> inicializovat<br>
	 * 0 - Port S1<br>
	 * 1 - Port S2<br>
	 * 2 - Port S3<br>
	 * 3 - Port S4<br>
	 * 4 - Port A<br>
	 * 5 - Port B<br>
	 * 6 - Port C<br>
	 * 7 - Port D
	 */
	public static final int[] HardwareIndexesToNotInitialize = new int[] {};
	
	private Port[] harPorts;
	
	/** Abstraktni funkce, ktera ma za ukol inicializovat jeden ze senzoru */
	protected abstract BaseSensor initSensor1(Port port);

	/** Abstraktni funkce, ktera ma za ukol inicializovat jeden ze senzoru */
	protected abstract BaseSensor initSensor2(Port port);

	/** Abstraktni funkce, ktera ma za ukol inicializovat jeden ze senzoru */
	protected abstract BaseSensor initSensor3(Port port);

	/** Abstraktni funkce, ktera ma za ukol inicializovat jeden ze senzoru */
	protected abstract BaseSensor initSensor4(Port port);

	/** Abstraktni funkce, ktera ma za ukol inicializovat jeden z prebyvajicich motoru */
	protected abstract RegulatedMotor initMotor1(Port port);

	/** Abstraktni funkce, ktera ma za ukol inicializovat jeden z prebyvajicich motoru */
	protected abstract RegulatedMotor initMotor2(Port port);

	/** Abstraktni funkce, ktera ma za ukol inicializovat levy motor */
	protected abstract EncoderMotor initLeftDriveMotor(Port port);

	/** Abstraktni funkce, ktera ma za ukol inicializovat pravy motor */
	protected abstract EncoderMotor initRightDriveMotor(Port port);

	/** Promenna do ve ktere je ulozena instance RobotHardware, ktera se bude pouzivat */
	public static RobotHardware RobotHardwareToInitialize;

	public RobotHardware(float wheelDistance, float wheelRadius, boolean reverseLeft, boolean reverseRight)
	{
		ReverseLeftMotor = reverseLeft;
		ReverseRightMotor = reverseRight;
		WheelDistance = wheelDistance;
		WheelRadius = wheelRadius;
		DistanceMultiplyer = getDistanceMultiplier();
		RobotHardwareToInitialize = this;
		
		harPorts = new Port[] { SensorPort.S1, SensorPort.S2, SensorPort.S3, SensorPort.S4, 
				MotorPort.A, MotorPort.B, MotorPort.C, MotorPort.D };
	}

	public void setDriveMotorPorts(Port lMotor, Port rMotor)
	{
		harPorts[5] = lMotor;
		harPorts[6] = rMotor;
	}
	
	public void setExtraMotorPorts(Port motor1, Port motor2)
	{
		harPorts[4] = motor1;
		harPorts[7] = motor2;
	}
	
	public void setSensorPorts(Port sensor1, Port sensor2, Port sensor3, Port sensor4)
	{
		harPorts[0] = sensor1;
		harPorts[1] = sensor2;
		harPorts[2] = sensor3;
		harPorts[3] = sensor4;
	}
	
	/**
	 * Vrati vsechny senzory v robotovi jako array
	 * 
	 * @return Senzory robota jako BaseSensor[]
	 */
	public BaseSensor[] getSensors()
	{
		return new BaseSensor[] { Sensor1, Sensor2, Sensor3, Sensor4 };
	}

	/**
	 * Vrati vsechny motory v robotovi jako array
	 * 
	 * @return Motory robota jako BaseMotor[]
	 */
	public BaseMotor[] getMotors()
	{
		return new BaseMotor[] { Motor1, LeftDriveMotor, RightDriveMotor, Motor2 };
	}

	public EncoderSampleProvider[] getEncoders()
	{
		return new EncoderSampleProvider[] { new EncoderSampleProvider(Motor1),
				new EncoderSampleProvider(LeftDriveMotor), new EncoderSampleProvider(RightDriveMotor),
				new EncoderSampleProvider(Motor2) };
	}

	/**
	 * Ziska hodnotu tachometru leveho motoru
	 * 
	 * @return Kolik ujel levy motor
	 */
	public int getLeftTacho()
	{
		int tacho = LeftDriveMotor.getTachoCount();
		if (ReverseLeftMotor)
			tacho *= -1;
		return tacho;
	}

	/**
	 * Ziska hodnotu tachometru praveho motoru
	 * 
	 * @return Kolik ujel pravy motor
	 */
	public int getRightTacho()
	{
		int tacho = RightDriveMotor.getTachoCount();
		if (ReverseRightMotor)
			tacho *= -1;
		return tacho;
	}

	public float getLeftDrivenDist()
	{
		return (getLeftTacho() / 360f) * DistanceMultiplyer;
	}

	public float getRightDrivenDist()
	{
		return (getRightTacho() / 360f) * DistanceMultiplyer;
	}

	/**
	 * Ziska kolik robot ujel od posleniho resetovani motoru
	 * 
	 * @return Cislo, rikajici jakou vzdalenost robot ujel
	 */
	public float getDrivenDist()
	{
		return (getLeftDrivenDist() + getRightDrivenDist()) / 2;
	}

	/** Resetuje tachometry drive motoru */
	public void resetDriveMotorsTachos()
	{
		LeftDriveMotor.resetTachoCount();
		RightDriveMotor.resetTachoCount();
	}

	/**
	 * Nastavi rychlosti drive motoru
	 * 
	 * @param lSpeed Rychlost leveho motoru
	 * @param rSpeed Rychlost praveho motoru
	 */
	public void setDrivePowers(Integer lSpeed, Integer rSpeed)
	{
		setLeftDrivePower(lSpeed);
		setRightDrivePower(rSpeed);
	}

	/**
	 * Nastavi rychlost leveho drive motoru
	 * 
	 * @param speed Rychlost motoru
	 */
	public void setLeftDrivePower(Integer speed)
	{
		//aby rychlost byla v mezich motoru
		if (speed < -100)
			speed = -100;
		if (speed > 100)
			speed = 100;

		LeftDriveMotor.setPower(speed);
	}

	/**
	 * Nastavi rychlost praveho drive motoru
	 * 
	 * @param speed Rychlost motoru
	 */
	public void setRightDrivePower(Integer speed)
	{
		//aby rychlost byla v mezich motoru
		if (speed < -100)
			speed = -100;
		if (speed > 100)
			speed = 100;

		RightDriveMotor.setPower(speed);
	}

	/**
	 * Zacne otecet s drive motorama
	 * 
	 * @param forward True, pokud ma jet kladnym smerem
	 */
	public void startDriveMotors(Boolean forward)
	{
		startLeftDriveMotor(forward);
		startRightDriveMotor(forward);
	}

	/**
	 * Zacne otecet s levym drive motorem
	 * 
	 * @param forward True, pokud ma jet kladnym smerem
	 */
	public void startLeftDriveMotor(Boolean forward)
	{
		if (forward ^ ReverseLeftMotor)
			LeftDriveMotor.forward();
		else
			LeftDriveMotor.backward();
	}

	/**
	 * Zacne otecet s pravym drive motorem
	 * 
	 * @param forward True, pokud ma jet kladnym smerem
	 */
	public void startRightDriveMotor(Boolean forward)
	{
		if (forward ^ ReverseRightMotor)
			RightDriveMotor.forward();
		else
			RightDriveMotor.backward();
	}

	/**
	 * Zastavi drive motory
	 * 
	 * @param hardStop Pokud true, motory zastavi prudce
	 */
	public void stopDriveMotors(Boolean hardStop)
	{
		stopLeftDriveMotor(hardStop);
		stopRightDriveMotor(hardStop);
	}

	/**
	 * Zastavi levy drive motor
	 * 
	 * @param hardStop Pokud true, motor zastavi prudce
	 */
	public void stopLeftDriveMotor(Boolean hardStop)
	{
		if (hardStop)
			LeftDriveMotor.stop();
		else
			LeftDriveMotor.flt();
	}

	/**
	 * Zastavi pravy drive motor
	 * 
	 * @param hardStop Pokud true, motor zastavi prudce
	 */
	public void stopRightDriveMotor(Boolean hardStop)
	{
		if (hardStop)
			RightDriveMotor.stop();
		else
			RightDriveMotor.flt();
	}

	/**
	 * Funkce ziska hodnotu ze senzoru
	 * 
	 * @param sensor Senzor, ze ktereho chceme ziska hodnotu
	 * @return Sample ze senzoru
	 */
	protected float[] getSample(BaseSensor sensor)
	{
		float[] smp = new float[sensor.sampleSize()];
		sensor.fetchSample(smp, 0);
		return smp;
	}

	protected void afterInit()
	{
		// Do stuff
	}
	
	public abstract RotateMoveController getPilot();
	public abstract PoseProvider getPoseProvider();
	
	/**
	 * <strong>Funkce delana na spousteni skrz {@link FLLMainProgram.subOS.LoadingScreen}</strong><br>
	 * Inicializuje senzory robota podle instance RobotHardware v promenne
	 * {@link #RobotHardwareToInitialize}<br>
	 * K inicializaci se pouzivaji abstraktni metody zacinajici na "Init" (napr. {@link #initSensor1()})
	 * 
	 * @param _percentage Z kolika procent je inicializace hotova
	 * @param _msgFeed    Message feed pro LoadingScreen
	 */
	public static final void initRobotHardware(Object _percentage, Object _msgFeed)
	{
		Referenceable<Float> percentage = (Referenceable<Float>) _percentage;
		final ArrayList<String> msgFeed = (ArrayList<String>) _msgFeed;

		//Thready ve kterych se budou inicializovat senzory
		Thread[] initThreads = new Thread[8];

		final RobotHardware har = RobotHardwareToInitialize;
		
		final Port[] ports = RobotHardwareToInitialize.harPorts;
		
		//Funkce pro inicializace
		// @formatter:off
		final Runnable[] initFuncs = new Runnable[]
		{ 
			new Runnable() { public void run() { har.Sensor1 = har.initSensor1(ports[0]); }},
			new Runnable() { public void run() { har.Sensor2 = har.initSensor2(ports[1]); }},
			new Runnable() { public void run() { har.Sensor3 = har.initSensor3(ports[2]); }},
			new Runnable() { public void run() { har.Sensor4 = har.initSensor4(ports[3]); }},
			
			new Runnable() { public void run() { har.Motor1 = har.initMotor1(ports[4]); }},
			new Runnable() { public void run() { har.LeftDriveMotor = har.initLeftDriveMotor(ports[5]); }},
			new Runnable() { public void run() { har.RightDriveMotor = har.initRightDriveMotor(ports[6]); }},
			new Runnable() { public void run() { har.Motor2 = har.initMotor2(ports[7]); }}
		};
		// @formatter:on

		//Inicializuj thready
		for (int i = 0; i < initThreads.length; i++)
		{
			final int _i = i;
			//Funkce ktera pobezi v threadu
			Runnable threadAction = new Runnable()
			{
				@Override
				public void run()
				{
					for (int i : HardwareIndexesToNotInitialize)
					{
						if (i == _i)
							return;
					}

					Exception finalException = new Exception();
					//Zkus 3x inicializovat senzor
					for (int j = 0; j < 3; j++)
					{
						try
						{
							//Spusti inicializacni funkci
							initFuncs[_i].run();
							//Dej najevo ze se senzor inicializoval
							msgFeed.add("harInit ID: " + _i + ": DONE!");
							Sound.playTone(1000, 150, 10);
							return;
						}
						catch (Exception e)
						{
							//
							//Pokud se inicializace nepovedla
							SystemSound.playNonFatalErrorSound(true);
							//
							//Pokud se neco nepovede
							//Rekni to v msg feedu
							msgFeed.add("harInit ERR; HAR ID: " + _i);
							//Blikni
							BrickHardware.blinkLED(2, 300, true, 10);
							//Loguj chybu
							ErrorLogging
								.logError("Error when loading; Attempt number:" + j + ";" + Logger.getExceptionInfo(e));
						}
					}

					//Pokud se inicializace 3x nepovedla -> FATAL ERROR
					ErrorLogging.logFatalError(Logger.getExceptionInfo(finalException));
					ErrorLogging.startErrorLogScreen();

				}
			};

			//Vytvor a spust thread
			initThreads[i] = new Thread(threadAction);
			initThreads[i].start();
		}

		ArrayList<Integer> finishedIndexes = new ArrayList<Integer>();
		boolean allThreadsFinished = false;

		while (true)
		{
			allThreadsFinished = true;

			//Zkontroluj jestli vsechny thready dobehli
			for (int i = 0; i < initThreads.length; i++)
			{
				if (finishedIndexes.contains(i))
					continue;
				if (initThreads[i].isAlive())
					allThreadsFinished = false;
				else
					finishedIndexes.add(i);
			}

			//Rekni z kolika procent je inicializace hotova
			percentage.setValue((float) finishedIndexes.size() / 8f);

			//Pokud jsou vsechny thready hotove -> KONEC
			if (allThreadsFinished)
				break;
		}
		
		RobotHardwareToInitialize.afterInit();
	}
}
