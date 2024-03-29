package team.hobbyrobot.net.api.desktop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lejos.robotics.geometry.Point2D;
import lejos.robotics.navigation.Pose;
import team.hobbyrobot.collisiondetection.PathPerformer;
import team.hobbyrobot.net.api.desktop.requests.Response;
import team.hobbyrobot.robotmodeling.RemoteASCSRobot;
import team.hobbyrobot.robotobserver.RobotModel;
import team.hobbyrobot.subos.Referenceable;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;
import lejos.robotics.geometry.Line;

public class StorageNavigator
{
	public static final float APPROACH_DISTANCE = RemoteASCSRobot.SIZE * 2.5f;
	public static final float ALIGNEMENT_TIME_SECS = 3f;
	public static final float SECOND_ALIGNEMENT_TIME_SECS = 1.5f;
	
	public static final float STORAGE_CELL_WIDTH = 170;
	public static final float STORAGE_CELL_HEIGHT = 120;
	
	private int[] _storageCellsTags;
	private HashMap<Integer, Integer> _tagIdMap = new HashMap<>();
	
	private static LinkedList<RemoteASCSRobot> _robotsInUse = new LinkedList<RemoteASCSRobot>();
	
	public StorageNavigator(List storageCellsIds)
	{
		_storageCellsTags = new int[storageCellsIds.size()];
		for(int i = 0; i < storageCellsIds.size(); i++)
		{
			int tagId = ((Number)storageCellsIds.get(i)).intValue();
			_storageCellsTags[i] = tagId;
			_tagIdMap.put(tagId, i);
		}
	}
	
	public Map<Integer, Integer> getCellTagIDMap()
	{
		return _tagIdMap;
	}
	
	public Pose getApproachPose(int cellID)
	{
		int tag = _storageCellsTags[cellID];
		System.out.println(tag);
		RobotModel model = RemoteASCSRobot.globalCorrector.getRobotModel(tag);
		if(model == null)
			return null;
		
		return getApproachPoseFromTagPose((float)model.x, (float)model.y, (float)model.heading);
	}
	
	public static Pose getApproachPoseFromTagPose(float x, float y, float heading)
	{
		Pose p = new Pose(x, y, heading);
		
		p.moveUpdate(APPROACH_DISTANCE);
		
		p.setHeading(p.getHeading() + 180);
		if(p.getHeading() >= 360)
			p.setHeading(p.getHeading() - 360);
		return p;
	}
	
	public Thread goToStorageCellAsync(RemoteASCSRobot robot, int cellID, Referenceable<Boolean> success)
	{
		success.setValue(null);
		
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					success.setValue(goToStorageCell(robot, cellID));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		};
		t.start();
		return t;
	}
	
	private RemoteASCSRobot getAvailableRobot()
	{
		RemoteASCSRobot robot = null;
		synchronized(_robotsInUse)
		{
			for(RemoteASCSRobot candidate : RemoteASCSRobot.getRobots())
			{
				if(_robotsInUse.contains(candidate))
					continue;
				
				_robotsInUse.add(candidate);
				robot = candidate;
				break;
			}
		}
		return robot;
	}
	
	public boolean putItemTo(int cellID) throws IOException
	{
		RemoteASCSRobot robot = getAvailableRobot();
		if(robot == null)
		{
			System.out.println("All robots in use!");
			return false;
		}
		
		if(!goToStorageCell(robot, cellID))
		{
			_robotsInUse.remove(robot);
			return false;
		}
		
		boolean success = putItem(robot);
			
		_robotsInUse.remove(robot);
		return success;
	}
	
	public boolean takeItemFrom(int cellID) throws IOException
	{
		RemoteASCSRobot robot = getAvailableRobot();
		if(robot == null)
		{
			System.out.println("All robots in use!");
			return false;
		}
		
		if(!goToStorageCell(robot, cellID))
		{
			_robotsInUse.remove(robot);
			return false;
		}
		
		boolean success = takeItem(robot);
			
		_robotsInUse.remove(robot);
		return success;
	}
	
	public boolean goToStorageCell(RemoteASCSRobot robot, int cellID) throws IOException
	{
		
		Pose approachPose = getApproachPose(cellID);
		if(approachPose == null)
		{
			System.err.println("Unknown cell ID");
			return false;
		}
		PathPerformer performer = new PathPerformer(approachPose, robot.getID());
		while(!Thread.currentThread().isInterrupted())
		{
			if(performer.isAtDestination())
				break;
		}
		// Interrupted
		if(Thread.currentThread().isInterrupted())
			return false;
		
		RobotModel currModel = RemoteASCSRobot.globalCorrector.getRobotModel(robot.getID());
		
		double tanAlpha = Math.tan(Math.toRadians(approachPose.getHeading()));
		
		
		double hError = Math.abs(tanAlpha * currModel.x - currModel.y - tanAlpha * approachPose.getX() + approachPose.getY()) / Math.sqrt(tanAlpha * tanAlpha + 1);
		
		TDNRoot correctHDiffRequest = RemoteASCSRobot.Requests.CORRECT_HORIZONTAL_ERROR.toTDN
		(
			new TDNValue((float)hError, TDNParsers.FLOAT), 
			new TDNValue(approachPose.getHeading(), TDNParsers.FLOAT)
		);
		Response correctHDiffResponse = new Response(robot.api.rawRequest(correctHDiffRequest));
		if(!correctHDiffResponse.wasRequestSuccessful())
			return false;
		
		TDNRoot isCorrectionDoneRequest = RemoteASCSRobot.Requests.IS_HORIZONTAL_ERROR_CORRECTED.toTDN();
		
		while(!Thread.currentThread().isInterrupted())
		{
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e)
			{
				return false;
			}
			Response isCorrectionDoneResponse = new Response(robot.api.rawRequest(isCorrectionDoneRequest));
			if(!isCorrectionDoneResponse.wasRequestSuccessful())
				return false;
			TDNValue done = isCorrectionDoneResponse.getData().get(RemoteASCSRobot.Requests.IS_HORIZONTAL_ERROR_CORRECTED.response[0]);
			if(done == null)
				return false;
			if((boolean)done.value)
				break;
		}
		// Interrupted
		if(Thread.currentThread().isInterrupted())
			return false;
		
		// Position precisely
		
		// Get close to the storage cell
		TDNRoot travel0Request = RemoteASCSRobot.Requests.TRAVEL.toTDN(new TDNValue(APPROACH_DISTANCE * 2, TDNParsers.FLOAT));
		Response travel0Response = new Response(robot.api.rawRequest(travel0Request));
		if(!travel0Response.wasRequestSuccessful())
			return false;
		try
		{
			Thread.sleep((int)(ALIGNEMENT_TIME_SECS * 1000));
		}
		catch (InterruptedException e)
		{
			return false;
		}
		
		TDNRoot stopRequest = RemoteASCSRobot.Requests.STOP.toTDN();
		Response stopResponse = new Response(robot.api.rawRequest(stopRequest));
		if(!stopResponse.wasRequestSuccessful())
			return false;
		
		TDNRoot getPoseRequest = RemoteASCSRobot.Requests.GET_POSE.toTDN();
		Response getPoseResponse = new Response(robot.api.rawRequest(getPoseRequest));
		if(!getPoseResponse.wasRequestSuccessful())
			return false;
		
		TDNValue headingTDN = getPoseResponse.getData().get("heading");
		if(headingTDN == null)
			return false;
		TDNRoot setExpectedHeadingRequest = RemoteASCSRobot.Requests.SET_EXPECTED_HEADING.toTDN(headingTDN);
		Response setExpectedHeadingResponse = new Response(robot.api.rawRequest(setExpectedHeadingRequest));
		if(!setExpectedHeadingResponse.wasRequestSuccessful())
			return false;
		
		// Get close to the storage cell
		TDNRoot travel1Request = RemoteASCSRobot.Requests.TRAVEL.toTDN(new TDNValue(-APPROACH_DISTANCE / 2, TDNParsers.FLOAT));
		Response travel1Response = new Response(robot.api.rawRequest(travel1Request));
		if(!travel1Response.wasRequestSuccessful())
			return false;
		waitMoveEnd(robot);
		
		TDNRoot travel2Request = RemoteASCSRobot.Requests.TRAVEL.toTDN(new TDNValue(APPROACH_DISTANCE, TDNParsers.FLOAT));
		Response travel2Response = new Response(robot.api.rawRequest(travel2Request));
		if(!travel2Response.wasRequestSuccessful())
			return false;
		
		try
		{
			Thread.sleep((int)(SECOND_ALIGNEMENT_TIME_SECS * 1000));
		}
		catch (InterruptedException e)
		{
			return false;
		}
		
		stopResponse = new Response(robot.api.rawRequest(stopRequest));
		if(!stopResponse.wasRequestSuccessful())
			return false;
		
		return true;
	}

	public boolean takeItem(RemoteASCSRobot robot) throws IOException
	{
		TDNRoot lifterUpRequest = RemoteASCSRobot.Requests.LIFTER_UP.toTDN();
		Response lifterUpResponse = new Response(robot.api.rawRequest(lifterUpRequest));
		if(!lifterUpResponse.wasRequestSuccessful())
			return false;
		
		TDNRoot travelRequest = RemoteASCSRobot.Requests.TRAVEL.toTDN(new TDNValue(-APPROACH_DISTANCE, TDNParsers.FLOAT));
		Response travelResponse = new Response(robot.api.rawRequest(travelRequest));
		if(!travelResponse.wasRequestSuccessful())
			return false;
		waitMoveEnd(robot);
		
		return true;
	}
	
	public boolean putItem(RemoteASCSRobot robot) throws IOException
	{
		TDNRoot lifterDownRequest = RemoteASCSRobot.Requests.LIFTER_DOWN.toTDN();
		Response lifterDownResponse = new Response(robot.api.rawRequest(lifterDownRequest));
		if(!lifterDownResponse.wasRequestSuccessful())
			return false;
		
		TDNRoot travelRequest = RemoteASCSRobot.Requests.TRAVEL.toTDN(new TDNValue(-APPROACH_DISTANCE, TDNParsers.FLOAT));
		Response travelResponse = new Response(robot.api.rawRequest(travelRequest));
		if(!travelResponse.wasRequestSuccessful())
			return false;
		waitMoveEnd(robot);
		
		return true;
	}
	
	public static ArrayList<Line> getStorageCellBoundingBox(double x, double y, double alpha, double padding)
	{
		ArrayList<Line> out = new ArrayList<Line>();
		
		double width = STORAGE_CELL_WIDTH + padding;
		double height = STORAGE_CELL_HEIGHT + padding;
		
		Point2D[] object = getRotatedRect(x, y, width, height, alpha + Math.PI/2);
		out.addAll(RemoteASCSRobot.getLinesFromPoints(object));
				
		//Pose approachPose = getApproachPoseFromTagPose((float)x, (float)y, (float)Math.toDegrees(alpha));
		//approachPose.moveUpdate(-RemoteASCSRobot.SIZE);
		
		//double centerX = (approachPose.getX() + x) / 2;
		//double centerY = (approachPose.getY() + y) / 2;
		
		//Point2D[] block = getRotatedRect(centerX, centerY, RemoteASCSRobot.SIZE * 2, RemoteASCSRobot.SIZE + APPROACH_DISTANCE, alpha + Math.PI/2);
		//out.addAll(RemoteASCSRobot.getLinesFromPoints(block));

		return out;
		/*Point2D[] vertecies = new Point2D[4];
		
		double width = STORAGE_CELL_WIDTH + padding;
		double height = STORAGE_CELL_HEIGHT + padding;
		alpha += Math.PI/2;
		
		vertecies[0] = new Point2D.Double(
			x - (width / 2) * Math.cos(alpha) - (height / 2) * Math.sin(alpha),
			y - (width / 2) * Math.sin(alpha) + (height / 2) * Math.cos(alpha)
			);
		vertecies[1] = new Point2D.Double(
			x + (width / 2) * Math.cos(alpha) - (height / 2) * Math.sin(alpha),
			y + (width / 2) * Math.sin(alpha) + (height / 2) * Math.cos(alpha)
			);
		vertecies[2] = new Point2D.Double(
			x - (width / 2) * Math.cos(alpha) + (height / 2) * Math.sin(alpha),
			y - (width / 2) * Math.sin(alpha) - (height / 2) * Math.cos(alpha)
			);
		vertecies[3] = new Point2D.Double(
			x + (width / 2) * Math.cos(alpha) + (height / 2) * Math.sin(alpha),
			y + (width / 2) * Math.sin(alpha) - (height / 2) * Math.cos(alpha)
			);
		
		return vertecies;*/
	}
	
	private static Point2D[] getRotatedRect(double x, double y, double width, double height, double alpha)
	{
		Point2D[] vertecies = new Point2D[4];
		
		vertecies[0] = new Point2D.Double(
			x - (width / 2) * Math.cos(alpha) - (height / 2) * Math.sin(alpha),
			y - (width / 2) * Math.sin(alpha) + (height / 2) * Math.cos(alpha)
			);
		vertecies[1] = new Point2D.Double(
			x + (width / 2) * Math.cos(alpha) - (height / 2) * Math.sin(alpha),
			y + (width / 2) * Math.sin(alpha) + (height / 2) * Math.cos(alpha)
			);
		vertecies[2] = new Point2D.Double(
			x - (width / 2) * Math.cos(alpha) + (height / 2) * Math.sin(alpha),
			y - (width / 2) * Math.sin(alpha) - (height / 2) * Math.cos(alpha)
			);
		vertecies[3] = new Point2D.Double(
			x + (width / 2) * Math.cos(alpha) + (height / 2) * Math.sin(alpha),
			y + (width / 2) * Math.sin(alpha) - (height / 2) * Math.cos(alpha)
			);
		
		return vertecies;
	}
	
	private static void waitMoveEnd(RemoteASCSRobot robot)
	{
		while(!Thread.currentThread().isInterrupted())
		{
			if(robot.isMoving())
				break;
		}
		while(!Thread.currentThread().isInterrupted())
		{
			if(!robot.isMoving())
				break;
		}
	}

	public int[] getCellTags()
	{
		return _storageCellsTags;
	}
}
