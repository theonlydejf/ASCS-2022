package team.hobbyrobot.net;

import java.io.IOException;

public interface ClientRegisterer
{
	void startRegisteringClients() throws IOException;
	void stopRegisteringClients() throws IOException;
	int countRegisteredClients();
	void closeRegisteredClients() throws IOException;
	boolean isRegisteringClients();
}
