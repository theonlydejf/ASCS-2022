package team.hobbyrobot.collisiondetection;

import java.io.IOException;
import java.net.Socket;

import lejos.robotics.geometry.Point2D;
import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import team.hobbyrobot.net.api.desktop.requests.Response;
import team.hobbyrobot.robotmodeling.RemoteASCSRobot;
import team.hobbyrobot.robotmodeling.RemoteASCSRobotListener;
import team.hobbyrobot.robotobserver.RobotModel;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class Calibrator
{
	private static Path createPath(Waypoint... waypoints)
	{
		Path p = new Path();
		for(Waypoint w : waypoints)
			p.add(w);
		return p;
	}
	
	/**
	 * Calculates the "Delta A" value for collision avoidence. This method 
	 * should be run multiple times and than avarage the values to gain 
	 * a good estimate of the Delta A<br>
	 * The method needs 2 robots that travel at the same speed and 
	 * a free rectangular area, where the robots can freely travel.
	 * The area should have enaugh space to fit both robots when their
	 * centers are traveling along their horizontal edges. the rectangle's
	 * width should be long enaugh so the robots can accelerate to their
	 * full speed and travel at least 1/3 of the width at their maximum
	 * speed.<br>
	 * First, the algorithm aligns both robots to the two specified corners
	 * of the working area, then it sends the second robot towards the other
	 * horizontal corner, while meassuring the time it takes the robot to
	 * travel that distance. Then it sends the robot back to the bottom
	 * right corner. When the robot is 1/3 of the way ther, it also sends
	 * the first robot to the other side.
	 * @param topLeft Top left corner of the working area
	 * @param bottomRight Bottom right corner of the working area
	 * @param robot1 First robot, which will start at top left
	 * @param robot2 Second robot, which will start at bottom right
	 * @return The approximation of the Delta A value
	 * @throws IOException
	 */
	public static double CalibrateDeltaA(Point2D topLeft, Point2D bottomRight, RemoteASCSRobot robot1, RemoteASCSRobot robot2) throws IOException
	{
		//Setup robots
		System.out.println("] setting up pose of robot1");
		PathPerformer goToTopLeft = new PathPerformer(createPath(new Waypoint(topLeft.getX(), topLeft.getY(), 0)), robot1.getID());
		
		while(!Thread.interrupted())
		{
			if(goToTopLeft.isFinished())
				break;
		}
		
		if(!goToTopLeft.isAtDestination())
			throw new RuntimeException("Calibrating Delta A failed: could not position robot1 to topLeft");
		
		System.out.println("] setting up pose of robot2");
		PathPerformer goToBottomRight = new PathPerformer(createPath(new Waypoint(bottomRight.getX(), bottomRight.getY(), 180)), robot2.getID());
		
		while(!Thread.interrupted())
		{
			if(goToBottomRight.isFinished())
				break;
		}
		
		if(!goToBottomRight.isAtDestination())
			throw new RuntimeException("Calibrating Delta A failed: could not position robot2 to bottomRight");
		
		
		System.out.println("] measuring duration of travel");
		Waypoint bottomLeft = new Waypoint(topLeft.getX(), bottomRight.getY(), 0);
		TravelDurationMeasurer measurer = new TravelDurationMeasurer(robot2, bottomLeft);
		
		TDNRoot rotateTo0Request = RemoteASCSRobot.Requests.ROTATE_TO.toTDN(new TDNValue(0f, TDNParsers.FLOAT));
		Response robot1RotateTo0Response = new Response(robot1.api.rawRequest(rotateTo0Request));
		if(!robot1RotateTo0Response.wasRequestSuccessful())
		{
			robot2.removeRobotListener(measurer);
			throw new RuntimeException("Calibrating Delta A failed: could not send rotate to request");
		}
		
		System.out.println("] waiting for measurer to end");
		while(!Thread.interrupted())
		{
			if(measurer.finished)
				break;
		}
		System.out.println("] waiting for measurer ended");
		
		if(measurer.duration < 0)
			throw new RuntimeException("Calibrating Delta A failed: " + measurer.errorMsg);
		
		System.out.println("] starting travel of robot2");
		long longDuration = measurer.duration;
		double distance = bottomRight.getX() - topLeft.getX();
		TDNRoot travelRequest = RemoteASCSRobot.Requests.TRAVEL.toTDN(new TDNValue((float)distance, TDNParsers.FLOAT));
		Response robot2TravelResponse = new Response(robot2.api.rawRequest(travelRequest));
		if(!robot2TravelResponse.wasRequestSuccessful())
			throw new RuntimeException("Calibrating Delta A failed: could not send travel request to robot2");
		try
		{
			Thread.sleep(longDuration/3);
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException("Calibrating Delta A failed: Thread was interrupted");
		}
		
		RobotModel start1 = RemoteASCSRobot.globalCorrector.getRobotModel(robot1.getID());
		RobotModel start2 = RemoteASCSRobot.globalCorrector.getRobotModel(robot2.getID());
		
		System.out.println("] 1st snapshot of poses of robots done");
		System.out.println("] starting travel of robot1");
		Response robot1TravelResponse = new Response(robot1.api.rawRequest(travelRequest));
		if(!robot1TravelResponse.wasRequestSuccessful())
			throw new RuntimeException("Calibrating Delta A failed: could not send travel request to robot1");
		try
		{
			Thread.sleep(longDuration/3);
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException("Calibrating Delta A failed: Thread was interrupted");
		}
		RobotModel end1 = RemoteASCSRobot.globalCorrector.getRobotModel(robot1.getID());
		RobotModel end2 = RemoteASCSRobot.globalCorrector.getRobotModel(robot2.getID());
		
		System.out.println("] 2nd snapshot of poses of robots done");

		double dx1 = end1.x - start1.x;
		double dy1 = end1.y - start1.y;
		
		double dx2 = end2.x - start2.x;
		double dy2 = end2.y - start2.y;
		
		double dist1 = Math.sqrt(dx1*dx1 + dy1*dy1);
		double dist2 = Math.sqrt(dx2*dx2 + dy2*dy2);
		
		return dist2 - dist1;
	}
	
	private static class TravelDurationMeasurer implements RemoteASCSRobotListener
	{
		private long startTime = -1;
		
		private RemoteASCSRobot robot;
		public long duration = -1;
		public boolean finished = false;
		public String errorMsg = "NONE";
		
		public TravelDurationMeasurer(RemoteASCSRobot robot, Waypoint targetPose) throws IOException
		{
			this.robot = robot;
			robot.addRobotListener(this);
			Response calibTravelResponse = robot.followPath(targetPose);
			if(!calibTravelResponse.wasRequestSuccessful())
			{
				robot.removeRobotListener(this);
				error("Could not send goto request");
			}
			System.out.println("[MEASURER] traveling to: " + targetPose.x + " " + targetPose.y + " heading requiered: " + targetPose.isHeadingRequired() + ": " + targetPose.getHeading());
		}
		
		@Override
		public void eventReceived(String name, TDNRoot params, Socket client)
		{
		}

		@Override
		public void moveStarted(int id, Move move)
		{
			System.out.println("[MEASURER] move started: " + move.getMoveType().toString());
			if(move.getMoveType().equals(Move.MoveType.TRAVEL))
				startTime = System.currentTimeMillis();
		}

		@Override
		public void moveStopped(int id, Move move)
		{
			if(!move.getMoveType().equals(Move.MoveType.TRAVEL))
				return;
			System.out.println("[MEASURER] travel stopped");

			if(startTime < 0)
				error("Could not measure time");
			
			duration = System.currentTimeMillis() - startTime;
		}

		@Override
		public void atWaypoint(int robotID, Waypoint waypoint, Pose pose, int sequence)
		{
			System.out.println("[MEASURER] at waypoint: " + waypoint.x + " " + waypoint.y + " heading requiered: " + waypoint.isHeadingRequired() + ": " + waypoint.getHeading());
			robot.removeRobotListener(this);
			finished = true;
		}

		@Override
		public void pathComplete(int robotID, Waypoint waypoint, Pose pose, int sequence)
		{
			System.out.println("[MEASURER] path complete");
		}

		@Override
		public void pathInterrupted(int robotID, Waypoint waypoint, Pose pose, int sequence)
		{
			error("Measurement was interrupted");
		}
		
		private void error(String msg)
		{
			System.out.println("[MEASURER] ERROR");
			robot.removeRobotListener(this);
			errorMsg = msg;
			finished = true;
		}
	}
}
