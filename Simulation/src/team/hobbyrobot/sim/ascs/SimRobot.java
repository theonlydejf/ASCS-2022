package team.hobbyrobot.sim.ascs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.Move.MoveType;
import lejos.robotics.navigation.MoveListener;
import lejos.robotics.navigation.RotateMoveController;
import lejos.robotics.navigation.Waypoint;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.logging.SocketLoggerEndpointRegisterer;
import team.hobbyrobot.logging.VerbosityLogger;
import team.hobbyrobot.net.api.Service;
import team.hobbyrobot.net.api.TDNAPIServer;
import team.hobbyrobot.net.api.exceptions.RequestGeneralException;
import team.hobbyrobot.net.api.exceptions.RequestParamsException;
import team.hobbyrobot.net.api.exceptions.UnknownRequestException;
import team.hobbyrobot.net.api.remoteevents.RemoteEventProvider;
import team.hobbyrobot.net.api.streaming.TDNReceiver;
import team.hobbyrobot.net.api.streaming.TDNReceiverListener;
import team.hobbyrobot.subos.navigation.CompassPilot;
import team.hobbyrobot.subos.navigation.LimitablePilot;
import team.hobbyrobot.subos.navigation.Navigator;
import team.hobbyrobot.subos.net.RemoteMoveEventProvider;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class SimRobot implements TDNReceiverListener, RotateMoveController, Runnable, PoseProvider, LimitablePilot
{
	public RemoteMoveEventProvider eventProvider;
	public Logger logger;
	public Logger errorLogger;
	public TDNAPIServer api;
	public TDNReceiver receiver;
	public SocketLoggerEndpointRegisterer registerer;
	public int id;
	
	public Navigator nav;

	public Pose pose = new Pose(0, 0, 0);

	private Pose _poseAtMoveStart = null;
	private float _travelled = 0;
	private float _moveTravelTarget = 0;
	private float _moveRotateTarget = 0;
	private double _moveTravelLimit = Double.POSITIVE_INFINITY;
	private double _moveRotateLimit = Double.POSITIVE_INFINITY;
	private boolean _lastMoveLimited = false;
	
	private float _linearV = 100;
	private float _angularV = 360;
	private float _currLinearV = 0;
	private float _currAngularV = 0;
	
	private float _expectedHeading = 0;
	private float _targetAngle = 0;
	
	private Thread t = new Thread();
	
	private boolean _moving;
	
	private LinkedList<MoveListener> _listeners = new LinkedList<>(); 
	private Random rnd = new Random();
	private int angleSlippageChance = 20;
	private int travelSlippageChance = 25;
	private float angleSlippageAmount = 10;
	private float travelSlippageAmount = 30;
	private float travelKp = 1.5f;
	
	public SimRobot(int loggerPort, int apiPort, int correctorPort, Pose p) throws IOException
	{
		id = -1;
		System.out.println("Starting logger...");
		logger = new Logger("LOGGER " + loggerPort);
		Logger.main = logger;
		errorLogger = logger.createSubLogger("ERROR");
		registerer = new SocketLoggerEndpointRegisterer(logger, loggerPort);
		registerer.startRegisteringClients();
		
		eventProvider = new RemoteMoveEventProvider(id);
		
		pose = p;
		_expectedHeading = p.heading;
		_targetAngle = _expectedHeading;
		
		nav = new Navigator(SimRobot.this, SimRobot.this, logger);
		SimRobot.this.addMoveListener(eventProvider);
		nav.addNavigationListener(eventProvider);
		
		api = new TDNAPIServer(apiPort, logger, errorLogger);
		api.registerService("MovementService", new MovementService());

		api.setVerbosity(VerbosityLogger.DETAILED_OVERVIEW);
		Thread apiThread = api.createThread();
		apiThread.start();
		api.startRegisteringClients();
		
		receiver = new TDNReceiver(correctorPort);
		receiver.addListener(this);
		receiver.start();
		t = new Thread(this);
		t.setDaemon(true);
		t.start();
	}

	@Override
	public void run()
	{
		long lastMillis = System.currentTimeMillis();
		while (!Thread.interrupted())
		{
			long currMillis = System.currentTimeMillis();
			float deltaTime = (currMillis - lastMillis) / 1000f;
			if (deltaTime < .033f)
				continue;
			lastMillis = currMillis;

			boolean targetReached = false;
			boolean limitReached = false;
			//System.out.println("Updating pose: linearV=" +  + _currLinearV + "; angV=" + _currAngularV + "; robotAt: x=" + pose.x + "; y=" + pose.y + "; h=" + pose.heading);
			synchronized (pose)
			{
				// Move robot
				double lastHeadingRad = Math.toRadians(pose.heading);
				float movedForward = _currLinearV * deltaTime;
				_travelled += movedForward;
				float movedHeading = _currAngularV * deltaTime;
				if(_currAngularV != 0 || _currLinearV != 0)
				{
					// ang slip
					if(rnd.nextInt(angleSlippageChance) == angleSlippageChance / 2)
					{
						float amount = (float)rnd.nextFloat() * angleSlippageAmount;
						if(movedForward != 0 && movedHeading == 0)
						{
							amount *= rnd.nextFloat() - .5f;
							movedHeading = amount;
							System.out.println("ang slip when travelling");
						}
						else if(Math.abs(movedHeading) > amount)
							movedHeading -= Math.signum(movedHeading) * amount;
						else
							movedHeading = 0;
						
						System.out.println("ang slip: " + amount);
					}
					// travel slip
					if(rnd.nextInt(travelSlippageChance) == travelSlippageChance / 2)
					{
						float amount = (float)rnd.nextFloat() * travelSlippageAmount;
						if(movedHeading != 0 && movedForward == 0)
						{
							amount *= rnd.nextFloat() - .5f;
							movedForward = amount;
							System.out.println("fwd slip when rotating");
						}
						else if(Math.abs(movedForward) > amount)
							movedForward -= Math.signum(movedForward) * amount;
						else
							movedForward = 0;
						
						System.out.println("fwd slip: " + amount);
					}
				}
				
				if(_currAngularV == 0)
				{
					if(_currLinearV != 0)
					{
						float error = _targetAngle - pose.heading;
						movedHeading += error * travelKp * deltaTime;
					}
				}
				
				float movedX = (float)(Math.cos(lastHeadingRad) * movedForward);
				float movedY = (float)(Math.sin(lastHeadingRad) * movedForward);
				
				//System.out.println("movedX=" + movedX + "; movedY=" + movedY + "movedH=" + movedHeading);
				
				pose.x += movedX;
				pose.y += movedY;
				pose.heading += movedHeading;

				//System.out.println(id + ": dx=" + movedX + "; dy=" + movedY + "; dHeading=" + movedHeading);
				
				if(_poseAtMoveStart == null)
					continue;
				
				// Check if target is reached
				float dHeading = pose.heading - _poseAtMoveStart.heading;
				
				if(_currLinearV != 0 && _travelled >= _moveTravelTarget)
				{
					_currLinearV = 0;
				}
				if(_currAngularV != 0 && Math.abs(dHeading) >= Math.abs(_moveRotateTarget))
				{
					_currAngularV = 0;
					//pose.heading = normalizeAng(_poseAtMoveStart.heading + _moveRotateTarget);
				}
				
				if(_travelled >= _moveTravelLimit || Math.abs(dHeading) >= _moveRotateLimit)
				{
					_currLinearV = 0;
					_currAngularV = 0;
					limitReached = true;
				}
			}
			
			if(_moving && !limitReached && _currLinearV == 0 && _currAngularV == 0)
				targetReached = true;
			
			if(_moving && (limitReached || targetReached))
			{				
				_lastMoveLimited = limitReached;
				for(MoveListener l : _listeners)
					l.moveStopped(getMovement(), this);
				_moving = false;
				_expectedHeading += _moveRotateTarget;//= _poseAtMoveStart.heading + _moveRotateTarget;
			}
			
			
		}
	}
	
	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		eventProvider.setID(id);
		this.id = id;
	}

	private static float normalizeAng(float ang)
	{
		while(ang >= 360)
			ang -= 360;
		while(ang < 0)
			ang += 360;
		return ang;
	}

	public void stopRobot() throws IOException
	{
		registerer.stopRegisteringClients();
		logger.close();
		api.close();
		receiver.stopReceiving();

		t.interrupt();
	}

	@Override
	public void rootReceived(TDNRoot root)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void tdnSenderConnected()
	{
		// TODO Auto-generated method stub

	}

	private void startMove(float travel, float rotate, float travelV, float rotateV)
	{
		synchronized (pose)
		{
			_expectedHeading = normalizeAng(_expectedHeading);
			//pose.heading = normalizeAng(pose.heading);
			
            float fullRotationCount = (float)Math.floor(pose.heading / 360);
            float targetAng = fullRotationCount * 360 + _expectedHeading;

            if (targetAng - pose.heading > 180)
            	targetAng -= 360;
            if (targetAng - pose.heading < -180)
            	targetAng += 360;

            _targetAngle = targetAng;
			
			_poseAtMoveStart = pose.copy();
			_travelled = 0;
			_moveTravelTarget = travel;
			_moveRotateTarget = rotate;
			_moveTravelLimit = Double.POSITIVE_INFINITY;
			_moveRotateLimit = Double.POSITIVE_INFINITY;
			_lastMoveLimited = false;
			
			for(MoveListener l : _listeners)
				l.moveStarted(getTargetMovement(), this);
			
			_currLinearV = travelV;
			_currAngularV = rotateV;
			_moving = true;
		}
	}

	private Move getTargetMovement()
	{
		Move.MoveType type = getMoveType();
		
		switch(type) 
		{
			case ARC:
				return new Move(type, (float)_moveTravelTarget, _moveRotateTarget, _linearV, _angularV, _moving);
			case ROTATE:
				return new Move(type, 0, _moveRotateTarget, _linearV, _angularV, _moving);
			case TRAVEL:
				return new Move(type, (float)_moveTravelTarget, 0, _linearV, _angularV, _moving);
			default:
				return new Move(type, 0, 0, _linearV, _angularV, _moving);
			
		}
	}

	public class MovementService implements Service
	{
		@Override
		public TDNRoot processRequest(String request, TDNRoot params, Socket client)
			throws UnknownRequestException, RequestParamsException, RequestGeneralException
		{
			try
			{
				switch (request)
				{
					case "travel":
						float dist = params.get("distance").as();
						travel(dist, true);
						return new TDNRoot();
					case "rotate":
						float ang = params.get("angle").as();
						rotate(ang, true);
						return new TDNRoot();
					case "rotateTo":
						ang = params.get("angle").as();
						nav.rotateTo(ang, true);
						return new TDNRoot();
					case "getPose":
						return new TDNRoot().insertValue("x", new TDNValue(pose.x, TDNParsers.FLOAT))
							.insertValue("y", new TDNValue(pose.y, TDNParsers.FLOAT))
							.insertValue("heading", new TDNValue(normalizeAng(pose.heading), TDNParsers.FLOAT));
					case "registerMoveListener":
                        TDNValue port = params.get("port");
                        TDNValue id = params.get("id");

                        if(port == null)
                            throw new RequestParamsException("port isn't present in the current root", "port");
                        if(id == null)
                            throw new RequestParamsException("id isn't present in the current root", "id");
                        SimRobot.this.setId((int)id.value);
                      
                        String ip=(((InetSocketAddress) client.getRemoteSocketAddress()).getAddress()).toString().replace("/","");
                        
                        try 
                        {
                            eventProvider.connectListener(ip, (int) port.as());
                        } 
                        catch (UnknownHostException e) 
                        {
                            throw new RequestGeneralException("Exception occured when connecting remote move listener: " + Logger.getExceptionInfo(e));
                        } 
                        catch (IOException e) 
                        {
                            throw new RequestGeneralException("Exception occured when connecting remote move listener: " + Logger.getExceptionInfo(e));
                        }
                        
						return new TDNRoot();
					case "goto":
						TDNValue x = params.get("x");
						TDNValue y = params.get("y");
						TDNValue heading = params.get("heading");
						if (x == null)
							throw new RequestParamsException("Full position isn't present in the current root", "x");
						if (y == null)
							throw new RequestParamsException("Full position isn't present in the current root", "y");
						
						nav.clearPath();
						if (heading == null)
							nav.goTo((float) x.as(), (float) y.as());
						else
							nav.goTo((float) x.as(), (float) y.as(), (float) heading.as());
						return new TDNRoot();
					case "followPath":
						TDNValue waypoints = params.get("path");
						if(waypoints == null)
							throw new RequestParamsException("No path present in the current root", "path");
						
						List<TDNRoot> path = TDNValue.asList(waypoints);
						
						nav.singleStep(false);

						nav.clearPath();
						for(TDNRoot pose : path)
						{
							TDNValue x1 = pose.get("x");
							TDNValue y1 = pose.get("y");
							TDNValue heading1 = pose.get("heading");
							if (x1 == null)
								throw new RequestParamsException("Full position isn't present in a waypoint root", "x");
							if (y1 == null)
								throw new RequestParamsException("Full position isn't present in a waypoint root", "y");
							
							if (heading1 == null)
								nav.addWaypoint((float) x1.as(), (float) y1.as());
							else
								nav.addWaypoint((float) x1.as(), (float) y1.as(), (float) heading1.as());
						}
						
						logger.log("NEW PATH STARTED: ");
						for(Waypoint pt : nav.getPath())
							logger.log("\t" + pt.getPose().toString());
						nav.followPath();
						
						return new TDNRoot();
					case "continuePath":
						nav.singleStep(false);
						nav.followPath();
						return new TDNRoot();
					case "stop":
						nav.stop();
						return new TDNRoot();
					case "isPathCompleted":
						return new TDNRoot().insertValue("pathCompleted", new TDNValue(nav.pathCompleted(), TDNParsers.BOOLEAN));
					case "setNavTravelLimit":
						TDNValue limit = params.get("limit");
						if(limit == null)
							throw new RequestParamsException("No limit present in the current root", "limit");
						
						SimRobot.this.setTravelLimit((float) limit.as());
						return new TDNRoot();
					case "getExpectedHeading":				        
				        return new TDNRoot().insertValue("expectedHeading", new TDNValue(_expectedHeading, TDNParsers.FLOAT));
					case "setExpectedHeading":
						TDNValue expectedHeading = params.get("heading");
						if(expectedHeading == null)
							throw new RequestParamsException("No expectedHeading present in the current root", "limit");
						
						SimRobot.this._expectedHeading = (float) expectedHeading.as();
				        return new TDNRoot();
					default:
						throw new UnknownRequestException("Unknown request: " + request);
				}
			}
			catch (Exception e)
			{
				throw new RequestGeneralException("Exception when performing request: " + Logger.getExceptionInfo(e));
			}
		}

		@Override
		public void init()
		{

		}

	}

	public static class Pose
	{
		public Pose(float x, float y, float heading)
		{
			super();
			this.x = x;
			this.y = y;
			this.heading = heading;
		}

		public float x, y, heading;

		public Pose copy()
		{
			return new Pose(x, y, heading);
		}
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
	public boolean isMoving()
	{
		return _moving;
	}

	@Override
	public void travel(double distance)
	{
		travel(distance, false);
	}
	
	@Override
	public void travel(double distance, boolean immediateReturn)
	{
		startMove((float)distance, 0, _linearV * (float)Math.signum(distance), 0);
		logger.log("Started travel " + distance);
		if(!immediateReturn)
			waitMoveFinish();
	}

	@Override
	public void setLinearSpeed(double speed)
	{
		_linearV = (float)speed;
	}

	@Override
	public double getLinearSpeed()
	{
		return _linearV;
	}

	@Override
	public double getMaxLinearSpeed()
	{
		return 1000;
	}

	@Override
	public void setLinearAcceleration(double acceleration)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double getLinearAcceleration()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Move getMovement()
	{
		if(_poseAtMoveStart == null)
			return new Move(Move.MoveType.STOP, 0, 0, _linearV, _angularV, _moving);
		
		Move.MoveType type = getMoveType();
		
		float dx, dy;
		synchronized(pose)
		{
			dx = pose.x - _poseAtMoveStart.x;
			dy = pose.y - _poseAtMoveStart.y;			
		}
		double distance = _travelled;//Math.sqrt(dx * dx + dy * dy);
		float angle = pose.heading - _poseAtMoveStart.heading;
		
		switch(type) 
		{
			case ARC:
				return new Move(type, (float)distance, angle, _linearV, _angularV, _moving);
			case ROTATE:
				return new Move(type, 0, angle, _linearV, _angularV, _moving);
			case TRAVEL:
				return new Move(type, (float)distance, 0, _linearV, _angularV, _moving);
			default:
				return new Move(type, 0, 0, _linearV, _angularV, _moving);
			
		}			
	}

	private MoveType getMoveType()
	{
		if(_moveTravelTarget != 0 && _moveRotateTarget != 0)
			return Move.MoveType.ARC;
		else if(_moveRotateTarget != 0)
			return Move.MoveType.ROTATE;
		else if(_moveTravelTarget != 0)
			return Move.MoveType.TRAVEL;
		return Move.MoveType.STOP;
	}

	@Override
	public void addMoveListener(MoveListener listener)
	{
		_listeners.add(listener);
	}

	@Override
	public void rotate(double angle)
	{
		rotate(angle, false);
	}

	@Override
	public void rotate(double angle, boolean immediateReturn)
	{
		startMove(0, (float)angle, 0, _linearV * (float)Math.signum(angle));
		logger.log("Started rotate " + angle);
		if(!immediateReturn)
			waitMoveFinish();
	}

	@Override
	public void setAngularSpeed(double speed)
	{
		_angularV = (float)speed;
	}

	@Override
	public double getAngularSpeed()
	{
		return _angularV;
	}

	@Override
	public double getMaxAngularSpeed()
	{
		return 360;
	}

	@Override
	public void setAngularAcceleration(double acceleration)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double getAngularAcceleration()
	{
		throw new UnsupportedOperationException();
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
	
	public void waitMoveFinish()
	{
		while(!_moving)
			Thread.yield();
		
		while(_moving)
			Thread.yield();
	}

	@Override
	public void stop()
	{
		startMove(0, 0, 0, 0);
	}

	@Override
	public lejos.robotics.navigation.Pose getPose()
	{
		return new lejos.robotics.navigation.Pose(pose.x, pose.y, pose.heading);
	}

	@Override
	public void setPose(lejos.robotics.navigation.Pose aPose)
	{
		pose = new Pose(aPose.getX(), aPose.getY(), aPose.getHeading());
	}

	@Override
	public void setTravelLimit(double limit)
	{
		_moveTravelLimit = limit;
	}

	@Override
	public double getTravelLimit()
	{
		return _moveTravelLimit;
	}

	@Override
	public void setRotateLimit(double limit)
	{
		_moveRotateLimit = limit;
	}

	@Override
	public double getRotateLimit()
	{
		return _moveRotateLimit;
	}

	@Override
	public boolean wasLastMoveLimited()
	{
		return _lastMoveLimited;
	}
}
