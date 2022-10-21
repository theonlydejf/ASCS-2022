package team.hobbyrobot.robotobserver;

import org.json.simple.JSONArray;

public interface RobotObserverListener
{
	void robotsReceived(JSONArray robots);
}
