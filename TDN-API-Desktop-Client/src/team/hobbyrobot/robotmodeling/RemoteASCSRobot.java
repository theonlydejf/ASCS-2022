package team.hobbyrobot.robotmodeling;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.parser.ParseException;

import lejos.robotics.geometry.Line;
import lejos.robotics.geometry.Point;
import lejos.robotics.geometry.Point2D;
import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import team.hobbyrobot.graphics.PaintPanel;
import team.hobbyrobot.graphics.Paintable;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.net.api.*;
import team.hobbyrobot.net.api.desktop.requests.Request;
import team.hobbyrobot.net.api.desktop.requests.RequestGenerator;
import team.hobbyrobot.net.api.desktop.requests.Response;
import team.hobbyrobot.net.api.remoteevents.RemoteEventListener;
import team.hobbyrobot.net.api.remoteevents.RemoteEventListenerServer;
import team.hobbyrobot.net.api.streaming.TDNSender;
import team.hobbyrobot.robotobserver.RobotCorrector;
import team.hobbyrobot.robotobserver.RobotModel;
import team.hobbyrobot.robotobserver.RobotObserver;
import team.hobbyrobot.tdn.base.TDNArray;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;
import team.hobbyrobot.utils.ProgressReporter;

/**
 * Class that ensures connection with an ASCS Vehicle.
 * 
 * @author David Krcmar
 */
public class RemoteASCSRobot implements Closeable
{
	public static final float SIZE = 120;
	public static final int BOUNDING_BOX_VERTECIES_COUNT = 4;
	public static final double BOUNDING_BOX_ANGLE_OFFSET = Math.PI / 4;
	
	/** Event listener server, which listens to all remote events from connected robots */
	public static RemoteEventListenerServer eventServer = null;

	public static Hashtable<Integer, Move> robotMovements = new Hashtable<Integer, Move>();

	public static RobotCorrector globalCorrector;

	/** Table of all robots, which were connected with their corresponding IDs */
	private static Hashtable<Integer, RemoteASCSRobot> _robots = new Hashtable<Integer, RemoteASCSRobot>();
	/**
	 * Instance of an implementation of RemoteEventListener, which listens for all of
	 * the remote robot events
	 */
	private static RobotEventListener _robotEventListener;
	/** Map, which stores all of the movement requests, which are available on the robot */
	public static Map<String, Request> moveRequests;
	public static Map<String, Request> osRequests;
	public static Map<String, Request> vehicleRequests;

	static
	{
		try
		{
			moveRequests = RequestGenerator.loadRequests("/Users/david/Documents/MAP/movement-vehicle-commands.json");
			osRequests = RequestGenerator.loadRequests("/Users/david/Documents/MAP/os-commands.json");
			vehicleRequests = RequestGenerator.loadRequests("/Users/david/Documents/MAP/vehicle-commands.json");
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** ID of the robot */
	private int _id;
	/** Logger used for logging on the local machine */
	private Logger _localLogger;
	/** All listeners, which are currently listening from the robot for any events */
	private LinkedList<RemoteASCSRobotListener> _listeners = new LinkedList<>();
	/** Last remote event which has occured on the robot */
	private Event _lastRmEvent = null;

	/** API client used for communicating with the robot */
	public TDNAPIClient api;
	/**
	 * Instance of {@link team.hobbyrobot.net.api.streaming.TDNSender TDNSender} which is
	 * used for correcting robots pose
	 */
	public TDNSender corrector;
	/** Instance of {@link java.net.Socket Socket} which receives log data from the robot */
	public Socket remoteLog;

	/** True, when robot is moving (last received simple event is moveStarted) */
	private boolean _isMoving;

	/**
	 * Initiates and starts remote event listener server, which will listen for any remote events from any of
	 * the robots
	 * 
	 * @param eventServerPort Port, on which the remote event listener server should listen for events
	 * @param logger          A locsl logger, on which the server will log its state
	 * @throws IOException
	 */
	public static void initEventListenerServer(int eventServerPort, Logger logger) throws IOException
	{
		if (logger == null)
			logger = new Logger();

		eventServer = new RemoteEventListenerServer((int) eventServerPort, logger);
		eventServer.start();
		eventServer.startRegisteringClients();

		_robotEventListener = new RobotEventListener();
		eventServer.addListener(_robotEventListener);
	}

	public static void initGlobalRobotCorrector(RobotObserver observer)
	{
		globalCorrector = new RobotCorrector(observer);
	}

	/**
	 * Gets a currently connected robot by its ID
	 * 
	 * @param id ID of the robot you're looking for
	 * @return Instance if {@link RemoteASCSRobot}, which represents the connection to the targeted robot
	 */
	public static RemoteASCSRobot getRobot(int id)
	{
		return _robots.get(id);
	}
	
	public static RemoteASCSRobot[] getRobots()
	{
		return _robots.values().toArray(new RemoteASCSRobot[0]);
	}
	
	public static Point2D[] getRobotBoundingBox(double padding, double x, double y)
	{
		Point2D[] vertecies = new Point2D[BOUNDING_BOX_VERTECIES_COUNT];
		double R = ((SIZE + padding) * 2) / Math.cos(Math.PI / vertecies.length);
		for(int i = 0; i < vertecies.length; i++)
		{
			vertecies[i] = new Point2D.Double(
				x + R * Math.cos((2 * Math.PI * i) / vertecies.length + BOUNDING_BOX_ANGLE_OFFSET),
				y + R * Math.sin((2 * Math.PI * i) / vertecies.length + BOUNDING_BOX_ANGLE_OFFSET)
				);
		}
		return vertecies;
		/*return new Point2D[] {
				new Point2D.Double(x + SIZE*2, y + SIZE*2),
				new Point2D.Double(x + SIZE*2, y - SIZE*2),
				new Point2D.Double(x - SIZE*2, y - SIZE*2),
				new Point2D.Double(x - SIZE*2, y + SIZE*2)
		};*/
	}

	public static ArrayList<Line> getLinesFromPoints(Point2D... points)
	{
		ArrayList<Line> out = new ArrayList<Line>();
		Point2D last = points[0];
		for(int i = points.length - 1; i >= 0; i--)
		{
			Point2D curr = points[i];
			out.add(new Line((float)curr.getX(), (float)curr.getY(), (float)last.getX(), (float)last.getY()));
			last = curr;
		}
		
		if(points.length < 4)
			return out;
		
		int half = points.length / 2;
		for(int i = 0; i < half; i++)
		{
			Point2D first = points[i];
			Point2D second = points[i+half];
			out.add(new Line((float)first.getX(), (float)first.getY(), (float)second.getX(), (float)second.getY()));
		}
		
		return out;
	}
	
	/**
	 * Wrapper for RemoteEventListener, which distributes the received events to the targeted robot
	 * models, based on their IDs, specified by the value in the event params root with key "id"
	 * 
	 * @author David Krcmar
	 */
	private static class RobotEventListener implements RemoteEventListener
	{
		public LinkedList<RemoteSimpleMoveEventListener> allSimpleMovesListeners = new LinkedList<>();
		public LinkedList<RemoteNavigationEventListener> allNavigationListeners = new LinkedList<>();

		@Override
		public void eventReceived(String name, TDNRoot params, Socket client)
		{
			// If ID is not present in the params -> skip the event
			TDNValue idTDN = params.get("id");
			if (idTDN == null)
				return;

			int id = idTDN.as();
			RemoteASCSRobot robot = RemoteASCSRobot.getRobot(id);

			switch (name)
			{
				case "moveStarted":
					Move simpleMove = TDNHelper.getMoveFromTDN(params.get("move").as());
					robotMovements.put(id, simpleMove);
					for (RemoteSimpleMoveEventListener l : allSimpleMovesListeners)
						l.moveStarted(id, simpleMove);
					if (robot != null)
						for (RemoteSimpleMoveEventListener l : robot._listeners)
							l.moveStarted(id, simpleMove);

					robot._isMoving = true;
				break;

				case "moveStopped":
					simpleMove = TDNHelper.getMoveFromTDN(params.get("move").as());
					robotMovements.remove(id);
					for (RemoteSimpleMoveEventListener l : allSimpleMovesListeners)
						l.moveStopped(id, simpleMove);
					if (robot != null)
						for (RemoteSimpleMoveEventListener l : robot._listeners)
							l.moveStopped(id, simpleMove);

					robot._isMoving = false;
				break;

				case "atWaypoint":
					Waypoint waypoint = TDNHelper.getWaypointFromTDN(params.get("waypoint").as());
					Pose pose = TDNHelper.getPoseFromTDN(params.get("pose").as());
					int sequence = params.get("sequence").as();
					for (RemoteNavigationEventListener l : allNavigationListeners)
						l.atWaypoint(id, waypoint, pose, sequence);
					if (robot != null)
						for (RemoteNavigationEventListener l : robot._listeners)
							l.atWaypoint(id, waypoint, pose, sequence);
				break;

				case "pathComplete":
					waypoint = TDNHelper.getWaypointFromTDN(params.get("waypoint").as());
					pose = TDNHelper.getPoseFromTDN(params.get("pose").as());
					sequence = params.get("sequence").as();
					for (RemoteNavigationEventListener l : allNavigationListeners)
						l.pathComplete(id, waypoint, pose, sequence);
					if (robot != null)
						for (RemoteNavigationEventListener l : robot._listeners)
							l.pathComplete(id, waypoint, pose, sequence);
				break;

				case "pathInterrupted":
					waypoint = TDNHelper.getWaypointFromTDN(params.get("waypoint").as());
					pose = TDNHelper.getPoseFromTDN(params.get("pose").as());
					sequence = params.get("sequence").as();
					for (RemoteNavigationEventListener l : allNavigationListeners)
						l.pathInterrupted(id, waypoint, pose, sequence);
					if (robot != null)
						for (RemoteNavigationEventListener l : robot._listeners)
							l.pathInterrupted(id, waypoint, pose, sequence);
				break;
			}

			// If a robot with the targeted id is connected, notify the robot about the event
			if (robot != null)
				robot.remoteEventReceived(name, params, client);
		}
	}

	public static class Requests
	{
		public static final Request TRAVEL;
		public static final Request ROTATE;
		public static final Request ROTATE_TO;
		public static final Request STOP;
		public static final Request FLT;
		
		public static final Request GO_TO;
		public static final Request FOLLOW_PATH;
		public static final Request CONTINUE_PATH;
		public static final Request IS_PATH_COMPLETED;
		public static final Request GET_REMAINING_PATH_COUNT;
		
		public static final Request GET_POSE;
		public static final Request SET_SPEED;
		public static final Request SET_NAV_TRAVEL_LIMIT;
		public static final Request SET_MAX_SPEED;
		public static final Request GET_SPEED;
		public static final Request RESET_GYRO_AT;
		public static final Request SET_POSITION;
		public static final Request SET_EXPECTED_HEADING;
		public static final Request GET_EXPECTED_HEADING;

		public static final Request REGISTER_MOVE_LISTENER;

		
		public static final Request GET_RESOURCES;
		public static final Request GET_RESOURCE;
		public static final Request SET_RESOURCE;
		
		public static final Request LIFTER_UP;
		public static final Request LIFTER_DOWN;
		public static final Request MOVE_LIFTER;
		public static final Request FORWARD;
		public static final Request BEEP;
		public static final Request GET_SAMPLES;
		
		static
		{
			// @formatter:off
			TRAVEL = 					moveRequests.get("travel");
			ROTATE =					moveRequests.get("rotate");
			ROTATE_TO = 				moveRequests.get("rotateTo");
			GET_POSE = 					moveRequests.get("getPose");
			STOP = 						moveRequests.get("stop");
			FLT = 						moveRequests.get("flt");
			GO_TO = 					moveRequests.get("goto");
			FOLLOW_PATH = 				moveRequests.get("followPath");
			CONTINUE_PATH = 			moveRequests.get("continuePath");
			IS_PATH_COMPLETED = 		moveRequests.get("isPathCompleted");
			GET_REMAINING_PATH_COUNT = 	moveRequests.get("getRemainingPath");
			SET_SPEED = 				moveRequests.get("setSpeed");
			SET_NAV_TRAVEL_LIMIT = 		moveRequests.get("setNavTravelLimit");
			SET_MAX_SPEED = 			moveRequests.get("setMaxSpeed");
			GET_SPEED = 				moveRequests.get("getSpeed");
			RESET_GYRO_AT = 			moveRequests.get("resetGyroAt");
			SET_POSITION = 				moveRequests.get("setPosition");
			SET_EXPECTED_HEADING = 		moveRequests.get("setExpectedHeading");
			GET_EXPECTED_HEADING = 		moveRequests.get("getExpectedHeading");
			REGISTER_MOVE_LISTENER = 	moveRequests.get("registerMoveListener");
			
			GET_RESOURCES =				osRequests.get("getResources");
			GET_RESOURCE =				osRequests.get("getResource");
			SET_RESOURCE =				osRequests.get("setResource");
			
			LIFTER_UP =					vehicleRequests.get("lifterUp");
			LIFTER_DOWN =				vehicleRequests.get("lifterDown");
			MOVE_LIFTER =				vehicleRequests.get("moveLifter");
			FORWARD =					vehicleRequests.get("forward");
			BEEP =						vehicleRequests.get("beep");
			GET_SAMPLES =				vehicleRequests.get("getSamples");

			// @formatter:on
		}
	}

	public static class RobotMovementGraphics implements Paintable, RemoteSimpleMoveEventListener
	{
		Hashtable<Integer, RobotModel> _robotModelsAtMoveStart = new Hashtable<>();

		public static final double ROBOT_SIZE = 100;

		private PaintPanel _paintPanel;
		private double _realWidth;
		private double _scale;

		public RobotMovementGraphics(PaintPanel paintPanel, double realWidth)
		{
			_paintPanel = paintPanel;
			_realWidth = realWidth;
			addAllSimpleMovesListener(this);
		}

		@Override
		public void paint(Graphics2D g)
		{
			if (robotMovements.size() <= 0)
				return;

			_scale = _paintPanel.getWidth() / _realWidth;
			for (Entry<Integer, Move> entry : robotMovements.entrySet())
			{
				int id = entry.getKey();
				Move move = entry.getValue();

				RobotModel model;
				synchronized (_robotModelsAtMoveStart)
				{
					model = _robotModelsAtMoveStart.get(id);
				}

				//model.heading = globalCorrector.getRobotModel(id).heading;
				switch (move.getMoveType())
				{
					case ARC:
						System.err.println("Idk how to draw arc moves :D");
					break;
					case ROTATE:
						drawRotate(g, model, move.getAngleTurned());
					break;
					case STOP:
						drawStop(g, model);
					break;
					case TRAVEL:
						drawTravel(g, model, move.getDistanceTraveled(), globalCorrector.getRobotModel(id).heading);
					break;
					default:
					break;

				}
			}
		}

		private void drawRotate(Graphics2D g, RobotModel model, double ang)
		{
			if (model == null)
				return;

			double x = model.x - ROBOT_SIZE / 2;
			double y = model.y - ROBOT_SIZE / 2;

			g.setColor(Color.blue);
			g.drawArc((int) (x * _scale), (int) (y * _scale), (int) (ROBOT_SIZE * _scale), (int) (ROBOT_SIZE * _scale),
				(int) -model.heading, (int) -ang);
			g.setStroke(new BasicStroke(3));
			g.drawArc((int) (x * _scale), (int) (y * _scale), (int) (ROBOT_SIZE * _scale), (int) (ROBOT_SIZE * _scale),
				(int) (-model.heading - ang/2), (int) -ang/2);

			g.setStroke(new BasicStroke(1));
		}

		private void drawStop(Graphics2D g, RobotModel model)
		{
			if (model == null)
				return;

			double x = model.x - ROBOT_SIZE / 2;
			double y = model.y - ROBOT_SIZE / 2;

			g.setColor(Color.red);
			g.drawRect((int) (x * _scale), (int) (y * _scale), (int) (ROBOT_SIZE * _scale),
				(int) (ROBOT_SIZE * _scale));
			g.drawLine((int)(x * _scale), (int)(y * _scale), (int)(x * _scale + ROBOT_SIZE * _scale), (int)(y * _scale + ROBOT_SIZE * _scale));
			g.drawLine((int)(x * _scale + ROBOT_SIZE * _scale), (int)(y * _scale), (int)(x * _scale), (int)(y * _scale + ROBOT_SIZE * _scale));

		}

		private void drawTravel(Graphics2D g, RobotModel model, double dist, double currHead)
		{
			if (model == null)
				return;

			double heading_rad = currHead / 180 * Math.PI;

			double targetX = (model.x + Math.cos(heading_rad) * dist) * _scale;
			double targetY = (model.y + Math.sin(heading_rad) * dist) * _scale;

			double x = model.x * _scale;
			double y = model.y * _scale;
			
			double arrowLeftX = x - Math.cos(heading_rad - Math.PI / 8) * 10;
			double arrowLeftY = y - Math.sin(heading_rad - Math.PI / 8) * 10;
			double arrowRightX = x - Math.cos(heading_rad + Math.PI / 8) * 10;
			double arrowRightY = y - Math.sin(heading_rad + Math.PI / 8) * 10;
			
			g.setColor(Color.blue);
			g.drawLine((int) x, (int) y, (int) targetX, (int) targetY);
			g.drawLine((int) x, (int) y, (int) arrowLeftX, (int) arrowLeftY);
			g.drawLine((int) x, (int) y, (int) arrowRightX, (int) arrowRightY);
		}

		@Override
		public void moveStarted(int id, Move move)
		{
			Thread t = new Thread()
			{
				@Override
				public void run()
				{

						/*TDNValue tdn = null;
						Response response = null;
						try
						{
							response = new Response(getRobot(id).api.rawRequest(RemoteASCSRobot.Requests.GET_EXPECTED_HEADING.toTDN()));
							if(!response.wasRequestSuccessful())
								throw new RuntimeException("GET_EXPECTED_HEADING request wasn't succesfful!");
							
							tdn = response.getData().get("expectedHeading");
						}
						catch (IOException e)
						{
						}
						
						if (tdn == null)
							throw new RuntimeException("Excpected heading wasn't found in the following root: " + 
						response == null ? "NONE" : response.getData().toString());*/
					
					synchronized (_robotModelsAtMoveStart)
					{
						_robotModelsAtMoveStart.put(id, globalCorrector.getRobotModel(id));
						_paintPanel.repaint();
					}
				}
			};
			t.setPriority(Thread.MAX_PRIORITY);
			t.start();
		}

		@Override
		public void moveStopped(int id, Move move)
		{
			_robotModelsAtMoveStart.remove(id);
			_paintPanel.repaint();
		}

	}

	public static void addAllSimpleMovesListener(RemoteSimpleMoveEventListener l)
	{
		_robotEventListener.allSimpleMovesListeners.add(l);
	}

	public static void removeAllSimpleMovesListener(RemoteSimpleMoveEventListener l)
	{
		_robotEventListener.allSimpleMovesListeners.remove(l);
	}

	public static void addAllNavigationEventsListener(RemoteNavigationEventListener l)
	{
		_robotEventListener.allNavigationListeners.add(l);
	}

	public static void removeAllNavigationEventsListener(RemoteNavigationEventListener l)
	{
		_robotEventListener.allNavigationListeners.remove(l);
	}

	public RemoteASCSRobot(int id, String ip, int loggerPort, int apiPort, int correctorPort, Logger localLogger)
		throws UnknownHostException, IOException
	{
		this(id, ip, loggerPort, apiPort, correctorPort, localLogger, null);
	}
	
	public RemoteASCSRobot(int id, String ip, int loggerPort, int apiPort, int correctorPort, Logger localLogger, ProgressReporter reporter)
		throws UnknownHostException, IOException
	{
		_id = id;

		synchronized (_robots)
		{
			// Check if remote event listener server has started
			//if (eventServer == null)
			//	throw new RuntimeException(
			//		"Instance of robot couldn't be created. Remote event listener server hasn't started yet!");
			// Check if a robot with the same id isn't registered yet
			if (_robots.containsKey(id))
			{
				if(reporter != null)
					reporter.setDone();
				throw new RuntimeException("Robot with id " + id + " is already registered");
			}
			
			if (localLogger == null)
				_localLogger = new Logger();
			else
				_localLogger = localLogger.createSubLogger("Robot_" + id);

			// Connect to the robot
			if(reporter != null)
			{
				reporter.setProgress(0);
				reporter.setMessage("Connecting logger");
				reporter.setReportChanged();
			}
			remoteLog = new Socket(ip, loggerPort);
			if(reporter != null)
			{
				reporter.setProgress(20);
				reporter.setMessage("Connecting API");
				reporter.setReportChanged();
			}
			api = new TDNAPIClient(ip, apiPort, _localLogger.createSubLogger("API"));
			if(reporter != null)
			{
				reporter.setProgress(40);
				reporter.setMessage("Connecting corrector");
				reporter.setReportChanged();
			}
			corrector = new TDNSender(ip, correctorPort);

			try
			{
				Thread.sleep(300);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(reporter != null)
			{
				reporter.setProgress(60);
				reporter.setMessage("Starting corrector");
				reporter.setReportChanged();
			}
			if(globalCorrector != null)
				globalCorrector.startCorrectingRobot(corrector, getID());
			else
				System.err.println("Robot " + id + " is not being corrected!");

			if (eventServer == null)
			{
				System.err.println("Robot " + id + " has no event server connected!");
				if(reporter != null)
					reporter.setDone();
				return;
			}
			
			//
			// Start listening for move events from the robot
			if(reporter != null)
			{
				reporter.setProgress(80);//TODO
				reporter.setMessage("Registering move listener");
				reporter.setReportChanged();
			}
			// Create params of the connect request
			TDNValue portTDNValue = new TDNValue(eventServer.getPort(), TDNParsers.INTEGER);
			TDNValue idTDNValue = new TDNValue(_id, TDNParsers.INTEGER);
			// Create the connect request
			TDNRoot connectEventRqst = moveRequests.get("registerMoveListener").toTDN(portTDNValue, idTDNValue);
			// Send the request
			TDNRoot responseTDN = api.rawRequest(connectEventRqst);
			Response response = new Response(responseTDN);

			if(reporter != null)
			{
				reporter.setProgress(100);
				reporter.setReportChanged();
				reporter.setDone();
			}
			// If connecting to remote event provider was successful -> create instance
			if (response.wasRequestSuccessful())
			{
				_robots.put(id, this);
				return;
			}

			// If connecting to the remote event provider was unsuccessful -> throw an exception
			throw new IOException(
				"Connecting to robots event provider at " + ip + " failed: " + response.getErrorDetails());
		}

	}

	/**
	 * Gets the robots ID
	 * 
	 * @return The ID of the robot
	 */
	public int getID()
	{
		return _id;
	}

	public Point2D[] getBoudingBox(double padding)
	{
		RobotModel model = globalCorrector.getRobotModel(getID());
		return getRobotBoundingBox(padding, model.x, model.y);
	}
	
	public void addRobotListener(RemoteASCSRobotListener l)
	{
		_listeners.add(l);
	}

	public void removeRobotListener(RemoteASCSRobotListener l)
	{
		_listeners.remove(l);
	}

	public Event getLastRemoteEvent()
	{
		return _lastRmEvent;
	}

	public Response goTo(Waypoint waypoint) throws IOException
	{
		TDNRoot rqst;
		if (waypoint.isHeadingRequired())
			rqst = Requests.GO_TO.toTDN(new TDNValue(waypoint.x, TDNParsers.FLOAT),
				new TDNValue(waypoint.y, TDNParsers.FLOAT),
				new TDNValue((float) waypoint.getHeading(), TDNParsers.FLOAT));
		else
			rqst = Requests.GO_TO.toTDN(new TDNValue(waypoint.x, TDNParsers.FLOAT),
				new TDNValue(waypoint.y, TDNParsers.FLOAT));

		return new Response(api.rawRequest(rqst));
	}

	public Response followPath(Waypoint... waypoints) throws IOException
	{
		TDNRoot[] pathRoots = new TDNRoot[waypoints.length];
		for (int i = 0; i < waypoints.length; i++)
		{
			Waypoint waypoint = waypoints[i];
			pathRoots[i] = new TDNRoot()
				.insertValue(Requests.GO_TO.params[0], new TDNValue(waypoint.x, TDNParsers.FLOAT))
				.insertValue(Requests.GO_TO.params[1], new TDNValue(waypoint.y, TDNParsers.FLOAT));
			if (waypoint.isHeadingRequired())
				pathRoots[i].insertValue(Requests.GO_TO.params[2],
					new TDNValue((float) waypoint.getHeading(), TDNParsers.FLOAT));
		}
		TDNArray pathArr = new TDNArray(pathRoots, TDNParsers.ROOT);

		return new Response(api.rawRequest(Requests.FOLLOW_PATH.toTDN(pathArr.asTDNValue())));
	}

	/**
	 * Gets called whenever any remote event is received
	 * 
	 * @param name   Name of the received event
	 * @param params Parameters of the remote event
	 * @param client Client which has sent the event
	 */
	void remoteEventReceived(String name, TDNRoot params, Socket client)
	{
		_lastRmEvent = new Event(this, name, params);

		for (RemoteASCSRobotListener l : _listeners)
			l.eventReceived(name, params, client);
	}

	/**
	 * Class which contains information describing ang remote event
	 * 
	 * @author David Krcmar
	 */
	public static class Event
	{
		public Event(RemoteASCSRobot provider, String name, TDNRoot params)
		{
			super();
			this.provider = provider;
			this.name = name;
			this.params = params;
		}

		/** Connection to a robot which has provided the remote event */
		public RemoteASCSRobot provider;
		/** Name of the remote event */
		public String name;
		/** Prameters of the remote event */
		public TDNRoot params;
	}

	@Override
	public void close() throws IOException
	{
		api.close();
		remoteLog.close();
		corrector.close();
	}

	public boolean isMoving()
	{
		return _isMoving;
	}
}
