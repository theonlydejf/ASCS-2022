package team.hobbyrobot.robotobserver;

import java.util.LinkedList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import team.hobbyrobot.python.*;

public class RobotObserver implements BridgeListener
{
	private Object _robotLock = new Object();
	private JSONArray _robots;
	
	private LinkedList<RobotObserverListener> _listeners = new LinkedList<RobotObserverListener>();
	
	public RobotObserver(Bridge bridge)
	{
		bridge.addListener(this);
	}
	
	public void addListener(RobotObserverListener l)
	{
		_listeners.add(l);
	}
	
	public void removeListener(RobotObserverListener l)
	{
		_listeners.remove(l);
	}
	
	public JSONArray getLastRobots()
	{
		return _robots;
	}

	@Override
	public void dataReceived(JSONObject object)
	{
		JSONArray arr = (JSONArray) object.get("robots");
		synchronized(_robotLock)
		{
			_robots = arr;
		}
		for(RobotObserverListener l : _listeners)
			l.robotsReceived(arr);
	}
}
