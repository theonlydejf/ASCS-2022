package team.hobbyrobot.net.api.desktop;

import java.net.Socket;

import team.hobbyrobot.tdn.core.TDNRoot;

public interface RobotCommanderListener
{
	void moveEventReceived(String name, TDNRoot params, Socket client, int robotID);
}
