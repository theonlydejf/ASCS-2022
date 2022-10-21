package temp;


import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import lejos.hardware.Sound;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.subos.Resources;
import team.hobbyrobot.subos.errorhandling.ErrorLogging;
import team.hobbyrobot.subos.navigation.PID;

public class PIDTuner extends PID
{	
	private Tuner _tuner;
	private int _port;
	private boolean _autoSave = false;
	
	public PIDTuner(double p, double i, double d, double f, int port) throws IOException
	{
		super(p, i, d, f);
		_port = port;
		startTuner();
	}

	public static PIDTuner createPIDTunerFromRscs(String pidName, int port) throws IOException
	{
		String path = getRscsPathToPID(pidName);
		
		float p = Resources.global().getFloat(path + ".kP");
		float i = Resources.global().getFloat(path + ".kI");
		float d = Resources.global().getFloat(path + ".kD");
		float f = Resources.global().getFloat(path + ".kF");
		PIDTuner pid = new PIDTuner(p, i, d, f, port);
		
		pid.setName(pidName);
		pid.setMaxIOutput(Resources.global().getFloat(path + ".maxI"));
		pid.setOutputLimits(Resources.global().getFloat(path + ".outMin"), Resources.global().getFloat(path + ".outMax"));
		pid.setOutputFilter(Resources.global().getFloat(path + ".outFilter"));
		pid.setOutputRampRate(Resources.global().getFloat(path + ".outRampRate"));
		pid.setDirection(Resources.global().getBoolean(path + ".reversed"));
		pid.setSetpoint(Resources.global().getFloat(path + ".setpoint"));
		pid.setSetpointRange(Resources.global().getFloat(path + ".setpointRange"));
		return pid;
	}
	
	public void setAutoSave(boolean autoSave)
	{
		_autoSave = autoSave;
	}
	
	public void startTuner() throws IOException
	{
		if(_tuner != null && isTunerRunning())
			stopTuner();
		
		_tuner = new Tuner(_port);
		_tuner.setDaemon(true);
		_tuner.setPriority(Thread.MIN_PRIORITY);
		_tuner.start();
	}
	
	public boolean isTunerRunning()
	{
		return _tuner.isAlive();
	}
	
	public boolean isTunerActive()
	{
		return _tuner.active;
	}
	
	public void stopTuner() throws IOException
	{
		_tuner.stopTuner();
	}
	
	protected class Tuner extends Thread implements Closeable
	{
		public boolean active;
		
		ServerSocket _server;
		
		public Tuner(int port) throws IOException
		{
			 _server = new ServerSocket(port);
		}
		
		private void safeSave()
		{
			try
			{
				save();
				Sound.playTone(1000, 200);							
			}
			catch(IOException ex)
			{
				ErrorLogging.logError("IOException occured when autosaving PID to rscs: " + Logger.getExceptionInfo(ex));
			}
		}
		
		@Override
		public void run()
		{
			if(_autoSave)
				safeSave();

			try(Socket client = _server.accept())
			{
				BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
				PrintWriter pr = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
				pr.println(PIDTuner.this.toString());
				pr.flush();
				active = true;
				while(active)
				{
					String cmd = br.readLine();
					try
					{
						String[] parts = cmd.split("\\s");
						switch(parts[0].toLowerCase())
						{
							case "p":
								PIDTuner.this.setP(Double.parseDouble(parts[1]));
								break;
								
							case "i":
								PIDTuner.this.setI(Double.parseDouble(parts[1]));
								PIDTuner.this.reset();
								break;
								
							case "d":
								PIDTuner.this.setD(Double.parseDouble(parts[1]));
								break;
								
							case "f":
								PIDTuner.this.setF(Double.parseDouble(parts[1]));
								break;
								
							case "maxi":
								PIDTuner.this.setMaxIOutput(Double.parseDouble(parts[1]));
								break;
								
							case "limits":
								if(parts.length > 2)
									PIDTuner.this.setOutputLimits(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
								else
									PIDTuner.this.setOutputLimits(Double.parseDouble(parts[1]));
								break;
								
							case "dir":
								PIDTuner.this.setDirection(parts[1].toLowerCase().contains("t"));
								break;
								
							case "outramprate":
								PIDTuner.this.setOutputRampRate(Double.parseDouble(parts[1]));
								break;
								
							case "outfilter:":
								PIDTuner.this.setOutputFilter(Double.parseDouble(parts[1]));
								break;
								
							default:
								pr.println("Unknown command: " + parts[0]);
								break;
						}
						pr.println(PIDTuner.this.toString());
						pr.flush();
						Sound.beep();
						if(_autoSave)
							safeSave();
					}
					catch (Exception e)
					{
						pr.println(Logger.getExceptionInfo(e));
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			active = false;
		}
		
		public void stopTuner() throws IOException
		{
			active = false;
			close();
		}

		@Override
		public void close() throws IOException
		{
			_server.close();
		}
	}
}
