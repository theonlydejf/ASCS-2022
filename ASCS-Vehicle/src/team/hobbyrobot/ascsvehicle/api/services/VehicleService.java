package team.hobbyrobot.ascsvehicle.api.services;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Hashtable;

import lejos.hardware.Sound;
import lejos.hardware.sensor.BaseSensor;
import lejos.robotics.BaseMotor;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.*;
import team.hobbyrobot.ascsvehicle.os.ASCSVehicleHardware;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.subos.hardware.EncoderSampleProvider;
import team.hobbyrobot.subos.navigation.CompassPilot;
import team.hobbyrobot.subos.navigation.LimitablePilot;
import team.hobbyrobot.subos.navigation.Navigator;
import team.hobbyrobot.subos.net.RemoteMoveEventProvider;
import team.hobbyrobot.net.api.exceptions.*;
import team.hobbyrobot.net.api.services.AbstractService;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

// TODO opravit
public class VehicleService extends AbstractService
{
	private ASCSVehicleHardware hardware;
	private CompassPilot _pilot;

	public VehicleService(ASCSVehicleHardware hardware)
	{
		super();
		this.hardware = hardware;
	}

	@Override
	public void init()
	{
		RotateMoveController controller = hardware.getPilot();
		if(!(controller instanceof CompassPilot))
			throw new IllegalArgumentException("Hardware does not contain CompassPilot!");
		
		_pilot = (CompassPilot) controller;
		super.init();
	}

	@Override
	protected Hashtable<String, RequestInvoker> initRequests()
	{
		Hashtable<String, RequestInvoker> requests = new Hashtable<String, RequestInvoker>()
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 4622976006937326703L;
			{
				/**
				 * LIFTER UP
				 * Moves the lifter of the vehicle all the way up
				 * request: lifterUp
				 * params: -
				 * response: -
				 */
				put("lifterUp", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						hardware.moveLifterTo(100);
						return new TDNRoot();
					}
				});

				/**
				 * LIFTER DOWN
				 * Moves the lifter of the vehicle all the way down
				 * request: lifterDown
				 * params: -
				 * response: -
				 */
				put("lifterDown", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						hardware.moveLifterTo(0);
						return new TDNRoot();
					}
				});

				/**
				 * MOVE LIFTER
				 * Moves the lifter of the vehicle to the desired position
				 * request: lifterDown
				 * params: [int] position (0-100)
				 * response: -
				 */
				put("moveLifter", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						TDNValue position = params.get("position");
						if (position == null)
							throw new RequestParamsException("No position present in the current root", "position");

						hardware.moveLifterTo((int) position.value);
						return new TDNRoot();
					}
				});

				/**
				 * FORWARD
				 * Makes the vehicle travel forward unregulated at certain power for certain duration of time
				 * request: forward
				 * params: [flaot] duration (secs), [int] power (-100 - 100)
				 * response: -
				 */
				put("forward", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						final TDNValue duration = params.get("duration");
						if (duration == null)
							throw new RequestParamsException("No duration present in the current root", "duration");

						Thread moveThread = new Thread()
						{
							@Override
							public void run()
							{
								
								_pilot.forward();
								try
								{
									Thread.sleep((int)duration.value);
								}
								catch (Exception w)
								{
								}
								_pilot.stop();
							}
						};

						return new TDNRoot();
					}
				});

				/**
				 * BEEP
				 * Makes the vehicle beep
				 * request: beep
				 * params: -
				 * response: -
				 */
				put("beep", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						Sound.beep();
						return new TDNRoot();
					}
				});

				/**
				 * GET SAMPLES
				 * Gets samples from every sample provider on the vehicle
				 * request: getSamples
				 * params: -
				 * response: -
				 */
				put("getSamples", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						BaseSensor[] sensors = hardware.getSensors();
						float[] sensorsSamples = new float[sensors.length];
						for (int i = 0; i < sensors.length; i++)
							if (sensors[i] != null)
								sensors[i].fetchSample(sensorsSamples, i);

						EncoderSampleProvider[] encoders = hardware.getEncoders();
						float[] encodersSamples = new float[encoders.length];
						for (int i = 0; i < encoders.length; i++)
							if (encoders[i] != null)
								encoders[i].fetchSample(encodersSamples, i);

						TDNRoot response = new TDNRoot();
						for (int i = 0; i < encodersSamples.length; i++)
							response.insertValue("encoder" + i, new TDNValue(encodersSamples[i], TDNParsers.FLOAT));
						for (int i = 0; i < sensorsSamples.length; i++)
							response.insertValue("sensor" + i, new TDNValue(sensorsSamples[i], TDNParsers.FLOAT));

						return new TDNRoot();
					}
				});
			}
		};

		return requests;
	}
}
