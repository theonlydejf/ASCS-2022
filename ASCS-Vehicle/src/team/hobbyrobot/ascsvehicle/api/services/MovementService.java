package team.hobbyrobot.ascsvehicle.api.services;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Hashtable;

import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.*;
import team.hobbyrobot.ascsvehicle.os.ASCSVehicleHardware;
import team.hobbyrobot.logging.Logger;
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
public class MovementService extends AbstractService implements MoveListener, NavigationListener
{
	private ASCSVehicleHardware hardware;
	private RotateMoveController pilot;
	private Logger logger;
	private PoseProvider poseProvider;
	private Navigator navigator;
	
	private RemoteMoveEventProvider moveEventProvider;

	public MovementService(ASCSVehicleHardware hardware, Logger logger)
	{
		super();
		this.logger = logger.createSubLogger("MvService");
		this.hardware = hardware;
		pilot = null;
		poseProvider = null;
	}

	@Override
	public void init()
	{
		pilot = hardware.getPilot();
		pilot.addMoveListener(this);
		
		poseProvider = hardware.getPoseProvider();
		navigator = new Navigator(pilot, poseProvider, logger);
		navigator.addNavigationListener(this);
		
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
				/** TRAVEL
				 * Makes the robot go straight certain distance
				 * request: travel
				 * params: [float] distance
				 * response: -
				 */
				put("travel", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						TDNValue dist = params.get("distance");

						if (dist == null)
							throw new RequestParamsException("Distance doesn't exist in the current root", "distance");

						pilot.travel((float) dist.as(), true);
						return new TDNRoot();
					}
				});

				/** ROTATE
				 * Makes the robot rotate by a certain angle
				 * request: rotate
				 * params: [float] angle
				 * response: -
				 */
				put("rotate", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						TDNValue ang = params.get("angle");

						if (ang == null)
							throw new RequestParamsException("Angle doesn't exist in the current root", "angle");

						pilot.rotate((float) ang.as(), true);
						return new TDNRoot();
					}
				});

				/** ROTATE TO
				 * Makes the robot rotate to a certain heading
				 * request: rotateTo
				 * params: [float] angle
				 * response: -
				 */
				put("rotateTo", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						TDNValue ang = params.get("angle");

						if (ang == null)
							throw new RequestParamsException("Angle doesn't exist in the current root", "angle");

						navigator.rotateTo((double) (float) ang.as(), true);
						return new TDNRoot();
					}
				});
				
				/** GET POSE
				 * Sends back pose, at which the robot thinks it is
				 * request: getPose
				 * params: -
				 * response: [float] x, [float] y, [float] heading
				 */
				put("getPose", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params)
					{
						Pose pose = poseProvider.getPose();
						return new TDNRoot().insertValue("x", new TDNValue(pose.getX(), TDNParsers.FLOAT))
							.insertValue("y", new TDNValue(pose.getY(), TDNParsers.FLOAT))
							.insertValue("heading", new TDNValue(pose.getHeading(), TDNParsers.FLOAT));
					}
				});

				/*put("arc", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						TDNValue radius = params.get("radius");
						TDNValue dist = params.get("distance");
						TDNValue ang = params.get("angle");

						if (radius == null)
							throw new RequestParamsException("Radius doesn't exist in the current root", "radius");

						if (dist != null)
							pilot.travelArc((float) radius.as(), (float) dist.as(), true);
						else if (ang != null)
							pilot.arc((float) radius.as(), (float) ang.as(), true);
						else
							throw new RequestParamsException("Angle nor distance doesn't exist in the current root",
								"angle", "radius");

						return new TDNRoot();
					}
				});*/

				/** STOP
				 * Makes the robot stop
				 * request: stop
				 * params: -
				 * response: -
				 */
				put("stop", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						navigator.singleStep(true);

						navigator.stop();
						return new TDNRoot();
					}
				});

				/** STOP & FLOAT 
				 * Makes the robot stop and floats the motors
				 * request: flt
				 * params: -
				 * response: -
				 */
				put("flt", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						navigator.singleStep(true);

						navigator.stop();
						hardware.LeftDriveMotor.flt();
						hardware.RightDriveMotor.flt();
						return new TDNRoot();
					}
				});

				/** GO TO POSITION
				 * Makes the robot go to a certain point or, if heading is specified, to certain pose
				 * request: goto
				 * params: [float] x, [float] y, ([float] heading)
				 * response: -
				 */
				put("goto", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						TDNValue x = params.get("x");
						TDNValue y = params.get("y");
						TDNValue heading = params.get("heading");
						if (x == null)
							throw new RequestParamsException("Full position isn't present in the current root", "x");
						if (y == null)
							throw new RequestParamsException("Full position isn't present in the current root", "y");
						
						navigator.clearPath();
						if (heading == null)
							navigator.goTo((float) x.as(), (float) y.as());
						else
							navigator.goTo((float) x.as(), (float) y.as(), (float) heading.as());
						return new TDNRoot();
					}
				});

				/** FOLLOW PATH
				 * Makes the robot follow a path, defined by multiple points or poses
				 * request: followPath
				 * params: [array of roots] path -> each root: [float] x, [float] y, ([float] heading)
				 * response: -
				 */
				put("followPath", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						TDNValue waypoints = params.get("path");
						if(waypoints == null)
							throw new RequestParamsException("No path present in the current root", "path");
						
						List<TDNRoot> path = TDNValue.asList(waypoints);
						
						navigator.singleStep(false);

						navigator.clearPath();
						for(TDNRoot pose : path)
						{
							TDNValue x = pose.get("x");
							TDNValue y = pose.get("y");
							TDNValue heading = pose.get("heading");
							if (x == null)
								throw new RequestParamsException("Full position isn't present in a waypoint root", "x");
							if (y == null)
								throw new RequestParamsException("Full position isn't present in a waypoint root", "y");
							
							if (heading == null)
								navigator.addWaypoint((float) x.as(), (float) y.as());
							else
								navigator.addWaypoint((float) x.as(), (float) y.as(), (float) heading.as());
						}
						
						logger.log("NEW PATH STARTED: ");
						for(Waypoint pt : navigator.getPath())
							logger.log("\t" + pt.getPose().toString());
						navigator.followPath();
						
						return new TDNRoot();
					}
				});
				
				/** CONTINUE PATH
				 * Makes the robot continue previously followed/loaded path, 
				 * eg. after the robot has stopped midway through
				 * request: continuePath
				 * params: -
				 * response: -
				 */
				put("continuePath", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						navigator.singleStep(false);
						navigator.followPath();
						return new TDNRoot();
					}
				});
				

				/** GET IF PATH IS COMPLETED
				 * Sends back information about whether the robot has completed following of a path
				 * request: isPathCompleted
				 * params: -
				 * response: [boolean] pathCompleted
				 */
				put("isPathCompleted", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						return new TDNRoot().insertValue("pathCompleted", new TDNValue(navigator.pathCompleted(), TDNParsers.BOOLEAN));
					}
				});
				
				/** GET HOW MANY WAYPOINTS ARE REMAINING FROM THE CURRENT PATH
				 * Sends back information about how many waypoints are remaining from the path the robot is currently following, returns 0 when path is completed
				 * request: getRemainingPathCount
				 * params: -
				 * response: [integer] pathCompleted
				 */
				put("getRemainingPathCount", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						return new TDNRoot().insertValue("remainingPathCount", new TDNValue(navigator.getPath().size(), TDNParsers.INTEGER));
					}
				});
				
				/** SET SPEED
				 * Sets the requested speed of moves
				 * request: setSpeed
				 * params: [float] speed
				 * response: -
				 */
				put("setSpeed", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						TDNValue speed = params.get("speed");
						if(speed == null)
							throw new RequestParamsException("No speed present in the current root", "speed");
						
						pilot.setLinearSpeed((float) speed.as());
						return new TDNRoot();
					}
				});
				
				put("setNavTravelLimit", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException, RequestGeneralException
					{
						if(!(pilot instanceof LimitablePilot))
							throw new RequestGeneralException("Trying to limit a pilot, which is not able to limit its movements");
						LimitablePilot pilot = (LimitablePilot)MovementService.this.pilot;
						TDNValue limit = params.get("limit");
						if(limit == null)
							throw new RequestParamsException("No limit present in the current root", "limit");
						
						pilot.setTravelLimit((float) limit.as());
						return new TDNRoot();
					}
				});
				
				put("setMaxSpeed", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						pilot.setLinearSpeed(pilot.getMaxLinearSpeed());
						return new TDNRoot();
					}
				});
				
				put("getSpeed", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						return new TDNRoot().insertValue("speed", new TDNValue((float)pilot.getLinearSpeed(), TDNParsers.FLOAT));
					}
				});
				
				put("resetGyroAt", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						TDNValue angle = params.get("angle");
						if(angle == null)
							throw new RequestParamsException("No speed present in the current root", "speed");
						
						hardware.resetGyroAt((int)((float)angle.as()));
						
						return new TDNRoot();
					}
				});
				
				put("setPosition", new RequestInvoker()
				{
					@Override
					public TDNRoot invoke(TDNRoot params) throws RequestParamsException
					{
						TDNValue x = params.get("x");
						TDNValue y = params.get("y");
						if (x == null)
							throw new RequestParamsException("Full position isn't present in the current root", "x");
						if (y == null)
							throw new RequestParamsException("Full position isn't present in the current root", "y");
						
						Pose tmp = poseProvider.getPose();
						poseProvider.setPose(new Pose((float)x.as(), (float)y.as(), tmp.getHeading()));
						return new TDNRoot();
					}
				});
				
				put("setExpectedHeading", new RequestInvoker()
				{
				    @Override
				    public TDNRoot invoke(TDNRoot params) throws RequestGeneralException, RequestParamsException
				    {
				        TDNValue heading = params.get("heading");
				        if(heading == null)
				            throw new RequestParamsException("Heading isn't present in the current root", "heading");
				        
				        if(!(pilot instanceof CompassPilot))
				            throw new RequestGeneralException("Trying to set expected heading to pilot which doesn't have one");
				        
				        ((CompassPilot)pilot).setExpectedHeading((float) heading.value);
				        
				        return new TDNRoot();
				    }
				});
				
				put("getExpectedHeading", new RequestInvoker()
				{
				    @Override
				    public TDNRoot invoke(TDNRoot params) throws RequestGeneralException, RequestParamsException
				    {
				        if(!(pilot instanceof CompassPilot))
				            throw new RequestGeneralException("Trying to set expected heading to pilot which doesn't have one");
				        
				        float heading = ((CompassPilot)pilot).getExpectedHeading();
				        
				        return new TDNRoot().insertValue("expectedHeading", new TDNValue(heading, TDNParsers.FLOAT));
				    }
				});
				
                put("registerMoveListener", new RequestInvoker()
                {
                    @Override
                    public TDNRoot invoke(TDNRoot params) throws RequestGeneralException, RequestParamsException
                    {
                        TDNValue port = params.get("port");
                        TDNValue id = params.get("id");

                        if(port == null)
                            throw new RequestParamsException("port isn't present in the current root", "port");
                        if(moveEventProvider == null && id == null)
                            throw new RequestParamsException("id isn't present in the current root", "id");
                        String ip=(((InetSocketAddress) client.getRemoteSocketAddress()).getAddress()).toString().replace("/","");
                        
                        try 
                        {
                        	if(moveEventProvider == null)
                        		moveEventProvider = new RemoteMoveEventProvider((int) id.as());
                            moveEventProvider.connectListener(ip, (int) port.as());
                    		pilot.addMoveListener(moveEventProvider);
                    		navigator.addNavigationListener(moveEventProvider);
                            logger.log("Remote move event listener connected");
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
                    }
                });
			}
		};
		
		return requests;
	}

	@Override
	public void moveStarted(Move event, MoveProvider mp)
	{
		logger.log("Started move: " + event.toString() + ". Robot is at " + poseProvider.getPose().toString());
	}

	@Override
	public void moveStopped(Move event, MoveProvider mp)
	{
		logger.log("Stopped move: " + event.toString() + ". Robot is at " + poseProvider.getPose().toString());
	}

	@Override
	public void atWaypoint(Waypoint waypoint, Pose pose, int sequence)
	{
		logger.log("At waypoint " + waypoint.getPose().toString() + ", waypoints remaining + " + navigator.getPath().size() + ". Robot is at " + pose.toString());
	}

	@Override
	public void pathComplete(Waypoint waypoint, Pose pose, int sequence)
	{
		logger.log("Path completed at waypoint " + waypoint.getPose().toString() + ", waypoints remaining: " + navigator.getPath().size() + ". Robot is at " + pose.toString());
	}

	@Override
	public void pathInterrupted(Waypoint waypoint, Pose pose, int sequence)
	{
		logger.log("Path interrupted at " + pose.toString() + ", next waypoint: " + waypoint.getPose().toString());
	}

}
