package team.hobbyrobot.robotmodeling;

import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;

public interface RemoteNavigationEventListener
{
	public void atWaypoint(int robotID, Waypoint waypoint, Pose pose, int sequence);

	public void pathComplete(int robotID, Waypoint waypoint, Pose pose, int sequence);

	public void pathInterrupted(int robotID, Waypoint waypoint, Pose pose, int sequence);
}
