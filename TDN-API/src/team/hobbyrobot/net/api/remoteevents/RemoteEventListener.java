package team.hobbyrobot.net.api.remoteevents;

import java.net.Socket;

import team.hobbyrobot.tdn.core.TDNRoot;

public interface RemoteEventListener
{
	void eventReceived(String name, TDNRoot params, Socket client);
}
