package team.hobbyrobot.subos.net;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.MoveListener;
import lejos.robotics.navigation.MoveProvider;
import lejos.robotics.navigation.NavigationListener;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import team.hobbyrobot.net.api.remoteevents.RemoteEventProvider;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class RemoteMoveEventProvider extends RemoteEventProvider implements MoveListener, NavigationListener
{    
    private static final TDNRoot BASE_MOVE_TDN;
    private static final TDNRoot BASE_WAYPOINT_TDN;
    private static final TDNRoot BASE_POSE_TDN;
    private static final TDNRoot BASE_WAYPOINT_PARAMS;
    private static final TDNRoot BASE_MOVE_PARAMS;

    static
    {
        BASE_MOVE_TDN = new TDNRoot()
                .insertValue("type", new TDNValue("NULL", TDNParsers.STRING))
                .insertValue("distance", new TDNValue(0f, TDNParsers.FLOAT))
                .insertValue("angle", new TDNValue(0f, TDNParsers.FLOAT))
                .insertValue("travelSpeed", new TDNValue(0f, TDNParsers.FLOAT))
                .insertValue("rotateSpeed", new TDNValue(0f, TDNParsers.FLOAT));
        BASE_WAYPOINT_TDN = new TDNRoot()
                .insertValue("x", new TDNValue(0f, TDNParsers.FLOAT))
                .insertValue("y", new TDNValue(0f, TDNParsers.FLOAT))
                .insertValue("heading", new TDNValue(0f, TDNParsers.FLOAT))
                .insertValue("headingRequired", new TDNValue(false, TDNParsers.BOOLEAN));
        BASE_POSE_TDN = new TDNRoot()
                .insertValue("x", new TDNValue(0f, TDNParsers.FLOAT))
                .insertValue("y", new TDNValue(0f, TDNParsers.FLOAT))
                .insertValue("heading", new TDNValue(0f, TDNParsers.FLOAT));
        BASE_WAYPOINT_PARAMS = new TDNRoot()
        		.insertValue("id", new TDNValue(-1, TDNParsers.INTEGER))
                .insertValue("waypoint", new TDNValue(BASE_WAYPOINT_TDN, TDNParsers.ROOT))
                .insertValue("pose", new TDNValue(BASE_POSE_TDN, TDNParsers.ROOT));
        BASE_MOVE_PARAMS = new TDNRoot()
    		.insertValue("id", new TDNValue(-1, TDNParsers.INTEGER))
        	.insertValue("move", new TDNValue(BASE_MOVE_TDN, TDNParsers.ROOT));
    }
    
    private int _robotId;
    
    public RemoteMoveEventProvider(int robotId)
    {
    	_robotId = robotId;
    }
    
    @Override
    public void moveStarted(Move event, MoveProvider mp) 
    {
        try
        {
            newEvent("moveStarted", setParsedMoveParams(event, _robotId));
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    @Override
    public void moveStopped(Move event, MoveProvider mp) 
    {
        try 
        {
            newEvent("moveStopped", setParsedMoveParams(event, _robotId));
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    @Override
    public void atWaypoint(Waypoint waypoint, Pose pose, int sequence) 
    {
        try 
        {
            newEvent("atWaypoint", setParsedWaypointParams(waypoint, pose, _robotId)
            	.insertValue("sequence", new TDNValue(sequence, TDNParsers.INTEGER)));
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    @Override
    public void pathComplete(Waypoint waypoint, Pose pose, int sequence) 
    {
        try 
        {
            newEvent("pathComplete", setParsedWaypointParams(waypoint, pose, _robotId)
            	.insertValue("sequence", new TDNValue(sequence, TDNParsers.INTEGER)));
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    @Override
    public void pathInterrupted(Waypoint waypoint, Pose pose, int sequence) 
    {
        try 
        {
            newEvent("pathInterrupted", setParsedWaypointParams(waypoint, pose, _robotId)
            	.insertValue("sequence", new TDNValue(sequence, TDNParsers.INTEGER)));
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    public void setID(int id)
    {
    	_robotId = id;
    }
    
    public int getID(int id)
    {
    	return _robotId;
    }
    
    private static TDNRoot setParsedMove(Move mv)
    {
        TDNRoot tdn = BASE_MOVE_TDN;
        tdn.get("type").value = mv.getMoveType().toString();
        tdn.get("distance").value = mv.getDistanceTraveled();
        tdn.get("angle").value = mv.getAngleTurned();
        tdn.get("travelSpeed").value = mv.getTravelSpeed();
        tdn.get("rotateSpeed").value = mv.getRotateSpeed();

        return tdn;
    }
    
    private static TDNRoot setParsedWaypoint(Waypoint wp)
    {
        TDNRoot tdn = BASE_WAYPOINT_TDN;
        tdn.get("x").value = wp.x;
        tdn.get("y").value = wp.y;
        tdn.get("heading").value = (float)wp.getHeading();
        tdn.get("headingRequired").value = wp.isHeadingRequired();
        
        return tdn;
    }
    
    private static TDNRoot setParsedPose(Pose p)
    {
        TDNRoot tdn = BASE_POSE_TDN;
        tdn.get("x").value = p.getX();
        tdn.get("y").value = p.getY();
        tdn.get("heading").value = p.getHeading();
        
        return tdn;
    }
    
    private static TDNRoot setParsedWaypointParams(Waypoint wp, Pose p, int id)
    {
        setParsedWaypoint(wp);
        setParsedPose(p);
        BASE_WAYPOINT_PARAMS.get("id").value = id;
        
        return BASE_WAYPOINT_PARAMS;
    }
    
    private static TDNRoot setParsedMoveParams(Move m, int id)
    {
        setParsedMove(m);
        BASE_MOVE_PARAMS.get("id").value = id;
        
        return BASE_MOVE_PARAMS;
    }
}
