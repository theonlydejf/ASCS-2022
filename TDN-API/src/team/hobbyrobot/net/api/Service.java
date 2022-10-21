package team.hobbyrobot.net.api;

import java.net.Socket;

import team.hobbyrobot.net.api.exceptions.RequestGeneralException;
import team.hobbyrobot.net.api.exceptions.RequestParamsException;
import team.hobbyrobot.net.api.exceptions.UnknownRequestException;
import team.hobbyrobot.tdn.core.TDNRoot;

public interface Service
{
	TDNRoot processRequest(String request, TDNRoot params, Socket client) throws UnknownRequestException, RequestParamsException, RequestGeneralException;
	void init();
}
