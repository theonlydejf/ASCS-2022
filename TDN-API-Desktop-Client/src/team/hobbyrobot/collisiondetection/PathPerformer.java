package team.hobbyrobot.collisiondetection;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import team.hobbyrobot.robotmodeling.RemoteASCSRobot;
import team.hobbyrobot.robotmodeling.RemoteASCSRobotListener;
import team.hobbyrobot.robotobserver.RobotModel;
import team.hobbyrobot.graphics.PaintPanel;
import team.hobbyrobot.graphics.Paintable;
import team.hobbyrobot.net.api.desktop.requests.Response;
import team.hobbyrobot.robotmodeling.*;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class PathPerformer implements RemoteASCSRobotListener
{
	private static CollisionAvoider collisionAvoider;
	private static LinkedList<PathPerformer> currentPaths = new LinkedList<>();

	public static void initCollisionAvoider(int width, int height)
	{
		collisionAvoider = new CollisionAvoider(new Dimension(width, height));
	}

	public static class PathGraphics implements Paintable
	{
		private PaintPanel panel;
		private float realWidth;

		public PathGraphics(PaintPanel panel, int realWidth)
		{
			this.panel = panel;
			this.realWidth = realWidth;
		}

		@Override
		public void paint(Graphics2D g)
		{
			float scale = panel.getWidth() / realWidth;
			synchronized (currentPaths)
			{
				for (PathPerformer performer : currentPaths)
				{
					g.setColor(Color.magenta);
					
					int x1 = (int) (performer._robotPoseAtMoveStart.x * scale);
					int y1 = (int) (performer._robotPoseAtMoveStart.y * scale);
					int x2 = (int) (performer._path[0].x * scale);
					int y2 = (int) (performer._path[0].y * scale);
					g.drawLine(x1, y1, x2, y2);

					for (int i = 1; i < performer._path.length; i++)
					{
						x1 = (int) (performer._path[i].x * scale);
						y1 = (int) (performer._path[i].y * scale);
						x2 = (int) (performer._path[i - 1].x * scale);
						y2 = (int) (performer._path[i - 1].y * scale);
						g.drawLine(x1, y1, x2, y2);
					}

					//int x = (int) (performer._path.get(performer._lastWaypointIndex).x * scale);
					//int y = (int) (performer._path.get(performer._lastWaypointIndex).y * scale);

					//g.fillOval(x-10, y-10, 20, 20);
				}
			}
		}

	}

	private RemoteASCSRobot _robot;
	private Waypoint[] _path;
	private double _travelLimit = Double.POSITIVE_INFINITY;
	private int _limmitedIndex = 0;

	private int _nextWaypointIndex = 0;
	private boolean _moveLimmited;
	private boolean _traveling = false;
	private RemoteASCSRobot _dangerousRobot;
	private RobotModel _robotPoseAtMoveStart;

	public PathPerformer(Path path, int robotID, int dangerousRobotID) throws IOException
	{
		_dangerousRobot = RemoteASCSRobot.getRobot(dangerousRobotID);
		_moveLimmited = false;

		_robot = RemoteASCSRobot.getRobot(robotID);
		_robotPoseAtMoveStart = RemoteASCSRobot.globalCorrector.getRobotModel(robotID);
		_path = path.toArray(new Waypoint[0]);
		_robot.addRobotListener(this);

		if (path instanceof LimmitedPath)
		{
			LimmitedPath limmitedPath = (LimmitedPath) path;
			_limmitedIndex = limmitedPath.limmitedStartWaypointIndex;
			_travelLimit = limmitedPath.travelLimit;
		}

		_robot.goTo(_path[_nextWaypointIndex]);

		synchronized (currentPaths)
		{
			currentPaths.add(this);
		}
	}

	@Override
	public void moveStarted(int robotID, Move move)
	{
		if (!move.getMoveType().equals(Move.MoveType.TRAVEL))
			return;
		_traveling = true;
		if (_nextWaypointIndex - 1 != _limmitedIndex)
			return;
		if (Double.isInfinite(_travelLimit))
			return;
		if (_moveLimmited)
			return;

		TDNRoot request = RemoteASCSRobot.Requests.SET_NAV_TRAVEL_LIMIT
			.toTDN(new TDNValue((float) _travelLimit, TDNParsers.FLOAT));

		try
		{
			Response response = new Response(_robot.api.rawRequest(request));
			if (!response.wasRequestSuccessful())
				System.err.println("Limiting travel on robot " + _robot.getID() + " failed!");
			else
				_moveLimmited = true;
		}
		catch (IOException e)
		{
			System.err.println("Limiting travel on robot " + _robot.getID() + " failed due to an exception!");
			e.printStackTrace();
		}
	}

	@Override
	public void moveStopped(int robotID, Move move)
	{
		if(move.getMoveType().equals(Move.MoveType.TRAVEL))
		{
			_traveling = false;
		}
		
		if (!_moveLimmited)
			return;
		Thread waitThread = new Thread()
		{
			@Override
			public void run()
			{
				if (_dangerousRobot == null)
					return;

				while (!Thread.interrupted())
				{
					if (!_dangerousRobot.isMoving())
						break;
				}
				try
				{
					Response response = new Response(
						_robot.api.rawRequest(RemoteASCSRobot.Requests.CONTINUE_PATH.toTDN()));
					if (!response.wasRequestSuccessful())
						System.err.println("Continuing path after limit on robot " + _robot.getID() + " failed!");
				}
				catch (IOException e)
				{
					System.err
						.println("Continuing path after limit on robot " + _robot.getID() + " failed due to an exception!");
					e.printStackTrace();
				}
			}
		};
		waitThread.setPriority(Thread.MIN_PRIORITY);
		waitThread.start();
	}

	@Override
	public void atWaypoint(int robotID, Waypoint waypoint, Pose pose, int sequence)
	{

	}

	//TODO Zopakuje posledni dva waypointy?? WTF??
	@Override
	public void pathComplete(int robotID, Waypoint waypoint, Pose pose, int sequence)
	{
		System.err.println(_nextWaypointIndex + " / " + _path.length);
		_nextWaypointIndex++;
		if(_nextWaypointIndex >= _path.length)
		{
			System.out.println("Path complete.. closing");
			close();
			return;
		}
		
		try
		{
			_robot.goTo(_path[_nextWaypointIndex]);
		}
		catch (IOException e)
		{
			System.err.println("Couldnt start move to waypoint " + _nextWaypointIndex + ". Closing...");
			e.printStackTrace();
			close();
		}
	}

	@Override
	public void pathInterrupted(int robotID, Waypoint waypoint, Pose pose, int sequence)
	{
		System.out.println("Path interrupted.. closing");
		close();
	}

	public void close()
	{
		System.out.println("Closing path on robot " + _robot.getID());
		_robot.removeRobotListener(this);
		synchronized (currentPaths)
		{
			currentPaths.remove(this);
		}
	}

	@Override
	public void eventReceived(String name, TDNRoot params, Socket client)
	{
	}
}
