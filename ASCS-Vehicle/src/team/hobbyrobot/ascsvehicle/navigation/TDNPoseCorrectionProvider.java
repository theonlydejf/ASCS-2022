package team.hobbyrobot.ascsvehicle.navigation;

import java.io.IOException;

import lejos.robotics.navigation.MoveController;
import lejos.robotics.navigation.Pose;
import team.hobbyrobot.subos.navigation.PoseCorrectionProvider;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.logging.VerbosityLogger;
import team.hobbyrobot.net.api.streaming.*;

public class TDNPoseCorrectionProvider implements PoseCorrectionProvider, TDNReceiverListener
{
	private MoveController _controller;
	private boolean _availableWhenMoving;
	private TDNReceiver _receiver;
	private Pose _lastPose = null;
	private VerbosityLogger _logger;
	
	public TDNPoseCorrectionProvider(int serverPort, MoveController moveController, boolean availableWhenMoving, Logger logger) throws IOException
	{
		_controller = moveController;
		_availableWhenMoving = availableWhenMoving;
		_receiver = new TDNReceiver(serverPort);
		_receiver.addListener(this);
		_logger = new VerbosityLogger(logger.createSubLogger("TDN Pose Corrector"));
		_logger.setVerbosityLevel(VerbosityLogger.DEBUGGING);
	}
	
	public void setVerbosity(int v)
	{
	    _logger.setVerbosityLevel(v);
	}
	
	public void startServer()
	{
		_receiver.start();
		_logger.log("Started server...", VerbosityLogger.OVERVIEW);
	}
	
	public void stopServer() throws IOException
	{
		TDNReceiver tmp = _receiver;
		_receiver = new TDNReceiver(_receiver.getPort());
		tmp.stopReceiving();
	}
	
	@Override
	public Pose getPose()
	{
		return _lastPose;
	}

	@Override
	public void setPose(Pose aPose)
	{
		_lastPose = aPose;
	}

	@Override
	public boolean correctionAvailable()
	{
		return _lastPose != null && (_availableWhenMoving || _controller.isMoving());
	}

	long millis = System.currentTimeMillis();
	@Override
	public void rootReceived(TDNRoot root)
	{
	    _logger.log("receive took " + (millis - System.currentTimeMillis()), VerbosityLogger.DEBUGGING);
	    millis = System.currentTimeMillis();
		float x = root.get("x").as();
		float y = root.get("y").as();
		float heading = root.get("heading").as();
		
		_lastPose = new Pose(x, y, heading);

        _logger.log("Received: " + _lastPose.toString(), VerbosityLogger.DEBUGGING);
	}

}
