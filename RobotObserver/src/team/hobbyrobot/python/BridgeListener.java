package team.hobbyrobot.python;

import org.json.simple.JSONObject;

public interface BridgeListener
{
	void dataReceived(JSONObject object);
}
