package team.hobbyrobot.net.api;

import java.net.Socket;

import team.hobbyrobot.net.api.exceptions.RequestGeneralException;
import team.hobbyrobot.net.api.exceptions.RequestParamsException;
import team.hobbyrobot.net.api.exceptions.UnknownRequestException;
import team.hobbyrobot.tdn.core.TDNRoot;

public interface Service
{
    /**
     * Gets called by the API server, when a request from this service has been received
     * @param request Name of the received request
     * @param params Parameters fo the request in the form of TDNRoot
     * @param client Client which sent the request
     * @return Response from the request, null if no response should be sent, empty TDNRoot if the 
     * client expects basic response (containing error code, parse times, etc.)
     * @throws UnknownRequestException When request with unknown name has been received
     * @throws RequestParamsException When incorrect params were received (eg. one or more parameters
     * are missing)
     * @throws RequestGeneralException General exception when performing the request
     */
	TDNRoot processRequest(String request, TDNRoot params, Socket client) throws UnknownRequestException, RequestParamsException, RequestGeneralException;
	
	/** Gets called before the service is registered */
	void init();
}
