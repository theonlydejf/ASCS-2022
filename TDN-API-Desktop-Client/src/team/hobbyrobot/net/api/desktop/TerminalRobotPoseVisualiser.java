package team.hobbyrobot.net.api.desktop;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.net.api.desktop.requests.Response;
import team.hobbyrobot.robotmodeling.RemoteASCSRobot;
import team.hobbyrobot.robotmodeling.RemoteASCSRobotListener;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class TerminalRobotPoseVisualiser implements RemoteASCSRobotListener
{
	private static String lastEvent = "NONE";

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException
	{
		String ip = args[0];
		System.out.println("Connecting to " + ip + "...");
		RemoteASCSRobot.initEventListenerServer(1234, new Logger());
		try (RemoteASCSRobot robot = new RemoteASCSRobot(0, ip, 1111, 2222, 3333, new Logger()))
		{
			System.out.println("Connected!");
			
			robot.addRobotListener(new TerminalRobotPoseVisualiser());
			
			char esc = 0x1B;
			String clearCmd = String.format("%c[2J", esc);
			String cursorHome = String.format("%c[H", esc);

			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
			TDNRoot getPoseRequest = RemoteASCSRobot.Requests.GET_POSE.toTDN();
			TDNRoot resetGyroRequest = RemoteASCSRobot.Requests.RESET_GYRO_AT.toTDN(new TDNValue(0f, TDNParsers.FLOAT));
			TDNRoot resetPositionRequest = RemoteASCSRobot.Requests.SET_POSITION.toTDN(new TDNValue(0f, TDNParsers.FLOAT), new TDNValue(0f, TDNParsers.FLOAT));
			TDNRoot rotateRequest = RemoteASCSRobot.Requests.ROTATE.toTDN(new TDNValue(90f, TDNParsers.FLOAT));
			TDNRoot travelRequest = RemoteASCSRobot.Requests.TRAVEL.toTDN(new TDNValue(300f, TDNParsers.FLOAT));
			TDNRoot fltRequest = RemoteASCSRobot.Requests.FLT.toTDN();

			long lastUpdate = 0;
			while(true)
			{
				if(lastUpdate + 1000 <= System.currentTimeMillis())
				{
					Response response = new Response(robot.api.rawRequest(getPoseRequest));
					
					Date now = new Date();
					System.out.print(clearCmd);
					System.out.print(cursorHome);
					System.out.println("Last update: " + sdf.format(now));
					System.out.println("Last event: " + lastEvent);
					TDNRoot pose = response.getData();
					System.out.println("X: " + pose.get("x"));
					System.out.println("Y: " + pose.get("y"));
					System.out.println("Heading: " + pose.get("heading"));
					lastUpdate = System.currentTimeMillis();
				}
				
				checkKey: if(System.in.available() > 0)
				{
					byte[] bytes = System.in.readNBytes(System.in.available());
					for(byte b : bytes)
					{
						if(b == (byte)'s')
						{
							System.out.println("stopping robot...");
							robot.api.rawRequest(fltRequest);
							break checkKey;
						}
					}
					
					lastUpdate = 0;
					System.out.println("reseting gyro...");
					robot.api.rawRequest(resetGyroRequest);
					System.out.println("reseting position...");
					robot.api.rawRequest(resetPositionRequest);
					System.out.println("moving robot...");
					robot.api.rawRequest(rotateRequest);
				}
			}
		}
	}

	@Override
	public void eventReceived(String name, TDNRoot params, Socket client)
	{
		lastEvent = name;
	}

	@Override
	public void moveStarted(int id, Move move)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveStopped(int id, Move move)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void atWaypoint(int robotID, Waypoint waypoint, Pose pose, int sequence)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pathComplete(int robotID, Waypoint waypoint, Pose pose, int sequence)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pathInterrupted(int robotID, Waypoint waypoint, Pose pose, int sequence)
	{
		// TODO Auto-generated method stub
		
	}

}
