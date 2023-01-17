package team.hobbyrobot.collisiondetection;

import java.util.ArrayList;

import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;

public class LimitedPath extends Path
{	
	public LimitedPath()
	{
		super();
	}
		
	public LimitedPath(ArrayList<Waypoint> _recordedPath)
	{
		for(Waypoint w : _recordedPath)
			add(w);
	}
	public int limitedStartWaypointIndex = -1;
	public double travelLimit = Double.POSITIVE_INFINITY;
	public double rotateLimit = Double.POSITIVE_INFINITY;
}