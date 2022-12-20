package team.hobbyrobot.collisiondetection;

import java.util.ArrayList;

import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;

public class LimmitedPath extends Path
{	
	public LimmitedPath()
	{
		super();
	}
	
	public LimmitedPath(ArrayList<Waypoint> _recordedPath)
	{
		for(Waypoint w : _recordedPath)
			add(w);
	}
	public int limmitedStartWaypointIndex = -1;
	public double travelLimit = Double.POSITIVE_INFINITY;
	public double rotateLimit = Double.POSITIVE_INFINITY;
}