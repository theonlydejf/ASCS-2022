package team.hobbyrobot.collisiondetection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import lejos.robotics.geometry.Line;
import lejos.robotics.geometry.Point;
import lejos.robotics.geometry.Point2D;
import lejos.robotics.navigation.DestinationUnreachableException;
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

//TODO Simulator dela tu divnou 360 otocku wtf
//TODO Cekani nefunguje kdyz roboti nezacnou ve spravnym poradi :DD
//TODO Simulator posila moc rychle veci za sebou (nejspis se pomichaj remote events a nejakej jinej request?? wtf) niz jsou chyby co to haze
/*
	Exception in thread "Thread-83" Exception in thread "Thread-81" java.lang.RuntimeException: Error when parsing TDN: "ittook:3;obj" is not known type!
		at team.hobbyrobot.tdn.core.TDNRootParser.readKeyValuePair(TDNRootParser.java:56)
		at team.hobbyrobot.tdn.core.TDNRootParser.readFromStream(TDNRootParser.java:34)
		at team.hobbyrobot.tdn.core.TDNRoot.readFromStream(TDNRoot.java:78)
		at team.hobbyrobot.net.api.TDNAPIClient.rawRequest(TDNAPIClient.java:94)
		at team.hobbyrobot.robotmodeling.RemoteASCSRobot.goTo(RemoteASCSRobot.java:715)
		at team.hobbyrobot.collisiondetection.PathPerformer.goTo(PathPerformer.java:390)
		at team.hobbyrobot.collisiondetection.PathPerformer.nextStep(PathPerformer.java:384)
		at team.hobbyrobot.collisiondetection.PathPerformer.tryNextStep(PathPerformer.java:397)
		at team.hobbyrobot.collisiondetection.PathPerformer.pathComplete(PathPerformer.java:609)
		at team.hobbyrobot.robotmodeling.RemoteASCSRobot$RobotEventListener.eventReceived(RemoteASCSRobot.java:264)
		at team.hobbyrobot.net.api.remoteevents.RemoteEventListenerServer.receiveEventFromClient(RemoteEventListenerServer.java:43)
		at team.hobbyrobot.net.api.remoteevents.RemoteEventListenerServer.access$1(RemoteEventListenerServer.java:33)
		at team.hobbyrobot.net.api.remoteevents.RemoteEventListenerServer$1.run(RemoteEventListenerServer.java:74)
	java.lang.RuntimeException: Error when parsing TDN: "n" is not known type!
		at team.hobbyrobot.tdn.core.TDNRootParser.readKeyValuePair(TDNRootParser.java:56)
		at team.hobbyrobot.tdn.core.TDNRootParser.readFromStream(TDNRootParser.java:39)
		at team.hobbyrobot.tdn.core.TDNRoot.readFromStream(TDNRoot.java:78)
		at team.hobbyrobot.net.api.TDNAPIClient.rawRequest(TDNAPIClient.java:94)
		at team.hobbyrobot.collisiondetection.PathPerformer$2.run(PathPerformer.java:558)
	Exception in thread "Thread-87" java.lang.RuntimeException: Error when parsing TDN: "0;;int" is not known type!
		at team.hobbyrobot.tdn.core.TDNRootParser.readKeyValuePair(TDNRootParser.java:56)
		at team.hobbyrobot.tdn.core.TDNRootParser.readFromStream(TDNRootParser.java:34)
		at team.hobbyrobot.tdn.core.TDNRoot.readFromStream(TDNRoot.java:78)
		at team.hobbyrobot.net.api.TDNAPIClient.rawRequest(TDNAPIClient.java:94)
		at team.hobbyrobot.collisiondetection.PathPerformer$2.run(PathPerformer.java:558)
	Exception in thread "Thread-91" java.lang.RuntimeException: Error when parsing TDN: "0;int" is not known type!
		at team.hobbyrobot.tdn.core.TDNRootParser.readKeyValuePair(TDNRootParser.java:56)
		at team.hobbyrobot.tdn.core.TDNRootParser.readFromStream(TDNRootParser.java:34)
		at team.hobbyrobot.tdn.core.TDNRoot.readFromStream(TDNRoot.java:78)
		at team.hobbyrobot.net.api.TDNAPIClient.rawRequest(TDNAPIClient.java:94)
		at team.hobbyrobot.collisiondetection.PathPerformer$2.run(PathPerformer.java:558)
 */

public class PathPerformer implements RemoteASCSRobotListener
{
	public static final double SMALLEST_UNSKIPPABLE_DISTANCE_SQUARED = 400;
	public static final double SMALLEST_VALID_DISTANCE_SQUARED = SMALLEST_UNSKIPPABLE_DISTANCE_SQUARED;
	public static final double SMALLEST_TOLERABLE_DISTANCE_SQUARED = 4900;
	public static final double SAFE_DISTANCE = 30;
	
	public static final int DESTINATION_UNREACHABLE_WAIT_TIMEOUT = 10000;
	public static double DELTA_A = 0;

	public static CollisionAvoider collisionAvoider;
	private static LinkedList<PathPerformer> currentPaths = new LinkedList<>();

	public static void initCollisionAvoider(int width, int height, int[] storageCellIds)
	{
		collisionAvoider = new CollisionAvoider(new Dimension(width, height), SAFE_DISTANCE, storageCellIds);
	}

	/**
	 * Class which can draw all currently active paths and their states
	 * 
	 * @author David Krcmar
	 */
	public static class PathGraphics implements Paintable
	{
		public static final Color FINISHED_SEGMENT_COLOR = new Color(255, 200, 255);
		public static final Color SEGMENT_COLOR = new Color(255, 0, 255);
		public static final Color TRANSPARENT_YELLOW = new Color(Color.yellow.getRed(), Color.yellow.getGreen(), Color.yellow.getBlue(), 64);

		private PaintPanel panel;
		private float realWidth;

		/**
		 * Creates instance of PathGraphics
		 * 
		 * @param panel     PaintPanel on which the graphics will be drawn
		 * @param realWidth Real width of the plane, in which the robots are moving
		 */
		public PathGraphics(PaintPanel panel, int realWidth)
		{
			this.panel = panel;
			this.realWidth = realWidth;
		}

		@Override
		public void paint(Graphics2D g)
		{
			float scale = panel.getWidth() / realWidth;
			Stroke dashedStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 },
				0);
			synchronized (currentPaths)
			{
				for (PathPerformer performer : currentPaths)
				{
					RobotModel model = RemoteASCSRobot.globalCorrector.getRobotModel(performer._robot.getID());
					
					if(performer._next != null)
					{
						g.setStroke(new BasicStroke());
						g.setColor(Color.pink);
						
						PathCollider pathCollider = new PathCollider(
							new Vector(model.x, model.y),
							new Vector(performer._next.x, performer._next.y),
							RemoteASCSRobot.SIZE * 1.5);
						
						for(Line l : pathCollider.linesCollider)
						{
							g.drawLine((int)(l.x1 * scale), (int)(l.y1 * scale), (int)(l.x2 * scale), (int)(l.y2 * scale));
						}
					}
					
					if (performer._segmentPath != null && performer._segmentPath.size() > 0)
					{
						// Draw curr travel path to next waypoint
						int x1 = (int) (performer._segmentPath.get(0).x * scale);
						int y1 = (int) (performer._segmentPath.get(0).y * scale);
						g.setColor(Color.pink);
						for (int i = 1; i < performer._segmentPath.size(); i++)
						{
							int x2 = (int) (performer._segmentPath.get(i).x * scale);
							int y2 = (int) (performer._segmentPath.get(i).y * scale);
							g.drawLine(x1, y1, x2, y2);
							x1 = x2;
							y1 = y2;
						}
						if (performer._nextSegmentPathIdx < performer._segmentPath.size() && performer._nextSegmentPathIdx > 0)
						{
							int x = (int) (performer._segmentPath.get(performer._nextSegmentPathIdx-1).x * scale);
							int y = (int) (performer._segmentPath.get(performer._nextSegmentPathIdx-1).y * scale);
							g.fillOval(x - 3, y - 3, 6, 6);
						}
						if (Double.isFinite(performer._segmentPath.travelLimit)
							&& performer._segmentPath.limitedStartWaypointIndex + 1 < performer._segmentPath.size())
						{
							g.setColor(SEGMENT_COLOR);
							Waypoint startW = performer._segmentPath
								.get(performer._segmentPath.limitedStartWaypointIndex);
							Waypoint endW = performer._segmentPath
								.get(performer._segmentPath.limitedStartWaypointIndex + 1);
							Vector start = new Vector(startW.x, startW.y);
							Vector end = new Vector(endW.x, endW.y);
							Vector dir = end.minus(start).direction();
							Vector lineCenter = start.plus(dir.scale(performer._segmentPath.travelLimit)).scale(scale);

							Vector dir90 = new Vector(-dir.cartesian(1), dir.cartesian(0));
							Vector A = lineCenter.plus(dir90.scale(10));
							Vector B = lineCenter.minus(dir90.scale(10));
							g.drawLine((int) A.cartesian(0), (int) A.cartesian(1), (int) B.cartesian(0),
								(int) B.cartesian(1));
							g.fillOval((int) A.cartesian(0) - 2, (int) A.cartesian(1) - 2, 4, 4);
							g.fillOval((int) B.cartesian(0) - 2, (int) B.cartesian(1) - 2, 4, 4);
						}
					}

					// Draw path segments
					int x1 = (int) (performer._robotPoseAtMoveStart.x * scale);
					int y1 = (int) (performer._robotPoseAtMoveStart.y * scale);
					for (int i = 0; i < performer._path.length; i++)
					{
						if (performer._nextWaypointIndex > i)
							g.setColor(FINISHED_SEGMENT_COLOR);
						else
							g.setColor(SEGMENT_COLOR);

						if (i == performer._nextWaypointIndex)
							g.setStroke(dashedStroke);
						else
							g.setStroke(new BasicStroke());

						int x2 = (int) (performer._path[i].x * scale);
						int y2 = (int) (performer._path[i].y * scale);
						g.drawLine(x1, y1, x2, y2);
						
						if(performer._path[i].isHeadingRequired())
						{
							double heading_rad = performer._path[i].getHeading() / 180 * Math.PI;
							g.drawLine((int) x2, (int) y2, (int) (x2 + 15 * Math.cos(heading_rad)),
								(int) (y2 + 15 * Math.sin(heading_rad)));
						}
						
						x1 = x2;
						y1 = y2;
					}

					if (performer._nextWaypointIndex >= performer._path.length)
						return;
					// Draw currently processed path segment
					Waypoint currWaypoint = performer._path[performer._nextWaypointIndex];
					int x = (int) (currWaypoint.x * scale);
					int y = (int) (currWaypoint.y * scale);
					g.fillOval(x - 3, y - 3, 6, 6);
					
					if(performer._dangerousPerformer != null && performer._dangerousPerformer._next != null)
					{
						RemoteASCSRobot dangerousRobot = performer._dangerousPerformer._robot;
						RobotModel dangerousModel = RemoteASCSRobot.globalCorrector.getRobotModel(dangerousRobot.getID());
						
						Vector A = new Vector(dangerousModel.x, dangerousModel.y);
						Vector B = new Vector(performer._dangerousPerformer._next.x, performer._dangerousPerformer._next.y);
						Vector C = new Vector(performer._robotPoseAtMoveStart.x, performer._robotPoseAtMoveStart.y);
						
						Vector startCollision = collisionAvoider.getCollisionPoint(A, B, C, DELTA_A);
						if(startCollision != null)
						{
							g.setColor(Color.yellow);
							g.fillOval((int)(startCollision.cartesian(0) * scale) - 4, (int)(startCollision.cartesian(1) * scale) - 4, 8, 8);
						}

						C = new Vector(model.x, model.y);
						Vector currCollision = collisionAvoider.getCollisionPoint(A, B, C, DELTA_A);
						
						if(currCollision != null)
						{
							g.setColor(TRANSPARENT_YELLOW);
							g.fillRect((int)(currCollision.cartesian(0) * scale) - 8, (int)(currCollision.cartesian(1) * scale) - 8, 16, 16);
							g.setColor(Color.yellow);
							g.drawRect((int)(currCollision.cartesian(0) * scale) - 8, (int)(currCollision.cartesian(1) * scale) - 8, 16, 16);
						}
					}
				}
			}
		}

	}

	/** Robot which is performing the path */
	private RemoteASCSRobot _robot;
	/** The path which the robot should perform */
	private Waypoint[] _path;

	private LimitedPath _segmentPath = null;
	private int _nextSegmentPathIdx = 0;

	/** Index of the waypoint the robot is currently traveling towards */
	private int _nextWaypointIndex = 0;
	/** True, when the limit request has been sent to the robot */
	private boolean _alreadyLimited;
	/** True, when move travel should in progress on the robot */
	private boolean _travelling = false;

	private boolean _finished = false;

	private boolean _atDestination = false;

	private Waypoint _next;

	private long _moveStartTimestamp;

	// TODO make it better :D
	/** Instance of robot that could be potentionally dangerous */
	private PathPerformer _dangerousPerformer;

	private RobotModel _robotPoseAtMoveStart;

	private LinkedList<Thread> waitThreads = new LinkedList<Thread>();

	public PathPerformer(Pose p, int robotID) throws IOException
	{
		this(pathFromPose(p), robotID);
	}
	
	private static Path pathFromPose(Pose p)
	{
		Path path = new Path();
		path.add(new Waypoint(p));
		return path;
	}
	
	public PathPerformer(Path path, int robotID) throws IOException
	{
		_alreadyLimited = false;

		_robot = RemoteASCSRobot.getRobot(robotID);
		_robotPoseAtMoveStart = RemoteASCSRobot.globalCorrector.getRobotModel(robotID);
		_path = path.toArray(new Waypoint[0]);
		_robot.addRobotListener(this);

		synchronized (currentPaths)
		{
			currentPaths.add(this);
		}

		tryNextStep();
		
		System.out.println("Path on robot " + robotID + " started: ");
		for (Waypoint w : path)
			System.out.println(
				" - " + w.x + " " + w.y + " heading requiered: " + w.isHeadingRequired() + ": " + w.getHeading());
	}

	private void nextStep() throws IOException, DestinationUnreachableException
	{
		_moveStartTimestamp = System.currentTimeMillis();
		_segmentPath = new LimitedPath();
		Waypoint next = _path[_nextWaypointIndex];
		RemoteASCSRobot[] robots = RemoteASCSRobot.getRobots();

		if (robots.length > 2)
		{
			System.err.println("Collision avoidence for more than 2 robots is not supported!");
			_segmentPath.add(next);
			goTo(next);
			return;
		}

		if (robots.length < 2)
		{
			_segmentPath.add(next);
			goTo(next);
			return;
		}

		RobotModel otherModel;
		
		if(currentPaths.size() > 1)
		{
			int otherPerformerIdx = currentPaths.indexOf(this) == 0 ? 1 : 0;
			_dangerousPerformer = currentPaths.get(otherPerformerIdx);		
			otherModel = RemoteASCSRobot.globalCorrector.getRobotModel(_dangerousPerformer._robot.getID());
		}
		else
		{
			int thisIdx = Arrays.asList(robots).indexOf(_robot);
			int otherIdx = (thisIdx == 0) ? 1 : 0;

			otherModel = RemoteASCSRobot.globalCorrector.getRobotModel(robots[otherIdx].getID());
		}

		RobotModel thisModel = RemoteASCSRobot.globalCorrector.getRobotModel(_robot.getID());

		if (currentPaths.size() <= 1 || _dangerousPerformer._next == null)
		{
			_segmentPath = new LimitedPath(collisionAvoider.getPath(
				new Pose((float) thisModel.x, (float) thisModel.y, (float) thisModel.heading), next, new Point2D.Double(otherModel.x, otherModel.y)));
		}
		else
		{
			if (_moveStartTimestamp < _dangerousPerformer._moveStartTimestamp)
			{
				_segmentPath.add(next);
				goTo(next);
				return;
			}
			
			// @formatter:on
			/*if (false && !otherPerformer.isTraveling())
			{
				Point2D otherStart = new Point2D.Double(otherModel.x, otherModel.y);
				Point2D otherEnd = otherPerformer._next;
				PathCollider pathCollider = new PathCollider(
					new Vector(otherStart.getX(), otherStart.getY()),
					new Vector(otherEnd.getX(), otherEnd.getY()),
					RemoteASCSRobot.SIZE * 1.5);
				ArrayList<Line> obstacles = new ArrayList<Line>(Arrays.asList(pathCollider.linesCollider));
				
				obstacles.addAll(RemoteASCSRobot.getLinesFromPoints(RemoteASCSRobot.getRobotBoundingBox(otherStart.getX(), otherStart.getY())));
				
				Path pathAroundRobot = new LimitedPath(collisionAvoider.getPath(
					new Pose((float) thisModel.x, (float) thisModel.y, (float) thisModel.heading), 
					next, obstacles));
				
				int idx = 0;
				double errSq;
				do
				{
					next = pathAroundRobot.get(idx);
					double errX = next.x - thisModel.x;
					double errY = next.y - thisModel.y;
					errSq = errX * errX + errY * errY;
					idx++;

				} while (errSq < SMALLEST_VALID_DISTANCE_SQUARED && idx < pathAroundRobot.size());
			}*/
			// @formatter:on

			Vector A = new Vector(otherModel.x, otherModel.y);
			Vector B = new Vector(_dangerousPerformer._next.x, _dangerousPerformer._next.y);
			Vector C = new Vector(thisModel.x, thisModel.y);
			Vector D = new Vector(next.x, next.y);
			float headingStart = (float) thisModel.heading;
			Float headingEnd = next.isHeadingRequired() ? (float) next.getHeading() : null;
			_segmentPath = collisionAvoider.getPath(null, A, B, C, D, headingStart, headingEnd,
				RemoteASCSRobot.SIZE, DELTA_A);
			_alreadyLimited = false;
		}

		_nextSegmentPathIdx = 0;
		Waypoint w;
		double errSq;
		do
		{
			w = _segmentPath.get(_nextSegmentPathIdx);
			double errX = w.x - thisModel.x;
			double errY = w.y - thisModel.y;
			errSq = errX * errX + errY * errY;
			_nextSegmentPathIdx++;

		} while (errSq < SMALLEST_VALID_DISTANCE_SQUARED && _nextSegmentPathIdx < _segmentPath.size());
		goTo(w);
	}
	
	private void goTo(Waypoint w) throws IOException
	{
		_next = w;
		_robot.goTo(new Waypoint(w.x, w.y));
	}

	private void tryNextStep()
	{
		try
		{
			nextStep();
		}
		catch (IOException e)
		{
			System.err.println("Couldnt start move to waypoint " + _nextWaypointIndex + ". Closing...");
			e.printStackTrace();
			close();
		}
		catch (DestinationUnreachableException e)
		{
			RobotModel model = RemoteASCSRobot.globalCorrector.getRobotModel(_robot.getID());
			double dx = _path[_nextWaypointIndex].x - model.x;
			double dy = _path[_nextWaypointIndex].y - model.y;
			if(dx * dx + dy * dy < SMALLEST_UNSKIPPABLE_DISTANCE_SQUARED)
			{
				_nextWaypointIndex++;
				System.out.println("Next waypoint is now unreachable! Waypoint really close => skipping...");
				tryNextStep();
				return;
			}
			
			System.out.println("Next waypoint is now unreachable! Waiting...");
			Thread tryThread = new Thread()
			{
				@Override
				public void run()
				{
					synchronized (waitThreads)
					{
						waitThreads.add(this);
					}

					long end = System.currentTimeMillis() + DESTINATION_UNREACHABLE_WAIT_TIMEOUT;
					boolean close = true;
					while (System.currentTimeMillis() <= end && !Thread.interrupted())
					{
						try
						{
							Thread.sleep(500);
							nextStep();
							close = false;
						}
						catch (IOException e)
						{
							System.err.println("Couldnt start move to waypoint " + _nextWaypointIndex + ". Closing...");
							e.printStackTrace();
							close();
							return;
						}
						catch (DestinationUnreachableException e)
						{
						}
						catch (InterruptedException e)
						{
							break;
						}
					}

					if (close)
					{
						close();
						System.err.println("Next waypoint is still unreachable! Closing...");
						return;
					}

					synchronized (waitThreads)
					{
						waitThreads.remove(this);
					}
				}
			};
			tryThread.setPriority(Thread.MIN_PRIORITY);
			tryThread.start();
		}
	}
	
	public boolean isTraveling()
	{
		return _travelling;
	}

	public boolean isFinished()
	{
		return _finished;
	}

	public boolean isAtDestination()
	{
		return _atDestination;
	}

	private boolean shouldBeLimited()
	{
		return _nextSegmentPathIdx - 1 == _segmentPath.limitedStartWaypointIndex;
	}

	@Override
	public void moveStarted(int robotID, Move move)
	{
		if (!move.getMoveType().equals(Move.MoveType.TRAVEL))
			return;
		_travelling = true;
		if (shouldBeLimited())
			return;
		if (Double.isInfinite(_segmentPath.travelLimit))
			return;
		if (_alreadyLimited)
			return;

		TDNRoot request = RemoteASCSRobot.Requests.SET_NAV_TRAVEL_LIMIT
			.toTDN(new TDNValue((float) _segmentPath.travelLimit, TDNParsers.FLOAT));

		try
		{
			Response response = new Response(_robot.api.rawRequest(request));
			if (!response.wasRequestSuccessful())
				System.err.println("Limiting travel on robot " + _robot.getID() + " failed!");
			else
				_alreadyLimited = true;
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
		if (move.getMoveType().equals(Move.MoveType.TRAVEL))
		{
			_travelling = false;
		}

		if (!_alreadyLimited)
			return;
		Thread waitThread = new Thread()
		{
			@Override
			public void run()
			{
				if (_dangerousPerformer == null)
					return;
				
				synchronized (waitThreads)
				{
					waitThreads.add(this);
				}

				if(_dangerousPerformer._robot.isMoving())
				{
					TravelStoppedListener listener = new TravelStoppedListener(_dangerousPerformer._robot);
					while (!Thread.interrupted())
					{
						if (listener.travelStopped)
							break;
					}
				}
				try
				{
					Response response = new Response(_robot.api.rawRequest(RemoteASCSRobot.Requests.CONTINUE_PATH.toTDN()));
					if (!response.wasRequestSuccessful())
						System.err.println("Continuing path after limit on robot " + _robot.getID() + " failed!");
				}
				catch (IOException e)
				{
					System.err.println(
						"Continuing path after limit on robot " + _robot.getID() + " failed due to an exception!");
					e.printStackTrace();
				}

				synchronized (waitThreads)
				{
					waitThreads.remove(this);
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
		Waypoint target = _path[_nextWaypointIndex];

		float targetErrX = target.x - waypoint.x;
		float targetErrY = target.y - waypoint.y;
		float targetErrSq = targetErrX * targetErrX + targetErrY * targetErrY;
		
		float poseErrX = waypoint.x - pose.getX();
		float poseErrY = waypoint.y - pose.getY();
		float poseErrSq = poseErrX * poseErrX + poseErrY * poseErrY;
		
		boolean newWaypoint = false;
		
		if (poseErrSq <= SMALLEST_VALID_DISTANCE_SQUARED && targetErrSq < SMALLEST_VALID_DISTANCE_SQUARED)
		{
			_nextWaypointIndex++;
			newWaypoint = true;
		}
		
		if(!newWaypoint)
		{
			tryNextStep();
			return;
		}
		
		Thread finishWaypointThread = new Thread() 
		{
			@Override
			public void run()
			{
				synchronized (waitThreads)
				{
					waitThreads.add(this);
				}
				
				if(_next.isHeadingRequired())
				{
					TDNRoot rotateToRequest = RemoteASCSRobot.Requests.ROTATE_TO.toTDN(new TDNValue((float)_next.getHeading(), TDNParsers.FLOAT));
					try
					{
						Response response = new Response(_robot.api.rawRequest(rotateToRequest));
						if(!response.wasRequestSuccessful())
						{
							System.err.println("Rotating to waypoints orientation failed beacuse rotateTo request was not successful: " + response.getErrorDetails() + " Closing...");
							close();
							return;
						}
						while(!Thread.interrupted())
						{
							if(_robot.isMoving())
								break;
						}
						while(!Thread.interrupted())
						{
							if(!_robot.isMoving())
								break;
						}
					}
					catch (IOException e)
					{
						System.err.println("Rotating to waypoints orientation failed due to IO error. Closing...");
						e.printStackTrace();
						close();
						return;
					}
				}
				
				if (_nextWaypointIndex >= _path.length)
				{
					System.out.println("Path complete.. closing");
					_atDestination = true;
					close();
				}
				else
				{
					tryNextStep();
				}
				
				synchronized (waitThreads)
				{
					waitThreads.remove(this);
				}
			}
		};
		finishWaypointThread.start();
	}

	@Override
	public void pathInterrupted(int robotID, Waypoint waypoint, Pose pose, int sequence)
	{
		System.out.println("Path interrupted.. closing");
		close();
	}

	public void close()
	{
		_finished = true;
		System.out.println("Closing path on robot " + _robot.getID());
		_robot.removeRobotListener(this);
		synchronized (currentPaths)
		{
			currentPaths.remove(this);
		}

		synchronized (waitThreads)
		{
			for (Thread t : waitThreads)
			{
				t.interrupt();
			}
			waitThreads.clear();
		}
	}

	@Override
	public void eventReceived(String name, TDNRoot params, Socket client)
	{
	}
	
	private static class TravelStoppedListener implements RemoteASCSRobotListener
	{
		private RemoteASCSRobot _robot;
		public TravelStoppedListener(RemoteASCSRobot robot)
		{
			_robot = robot;
			_robot.addRobotListener(this);
		}
		
		public boolean travelStopped = false;
		@Override
		public void eventReceived(String name, TDNRoot params, Socket client)
		{
		}

		@Override
		public void moveStarted(int id, Move move)
		{
		}

		@Override
		public void moveStopped(int id, Move move)
		{
			if(move.getMoveType().equals(Move.MoveType.TRAVEL))
			{
				travelStopped = true;
				_robot.removeRobotListener(this);
			}
		}

		@Override
		public void atWaypoint(int robotID, Waypoint waypoint, Pose pose, int sequence)
		{
		}

		@Override
		public void pathComplete(int robotID, Waypoint waypoint, Pose pose, int sequence)
		{
		}

		@Override
		public void pathInterrupted(int robotID, Waypoint waypoint, Pose pose, int sequence)
		{
		}
	}
}
