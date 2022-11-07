package team.hobbyrobot.ascsvehicle.navigation;

import java.io.IOException;
import java.util.LinkedList;

import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.MoveController;
import lejos.robotics.navigation.MoveListener;
import lejos.robotics.navigation.MoveProvider;
import lejos.robotics.navigation.Pose;
import lejos.utility.Stopwatch;
import team.hobbyrobot.subos.navigation.PoseCorrectionProvider;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.logging.VerbosityLogger;
import team.hobbyrobot.net.api.streaming.*;

public class TDNPoseCorrectionProvider implements PoseCorrectionProvider, TDNReceiverListener, MoveListener
{
	private MoveController _controller;
	private boolean _availableWhenMoving;
	private boolean _isMoving = false;
	private TDNReceiver _receiver;
	private Pose _lastPose = null;
	private VerbosityLogger _logger;
	private Stopwatch _poseTimeoutSw;
	private Stopwatch _moveTimeoutSw;
	private int _poseMillisTimeout;
	private int _moveMillisTimeout;
	
	LinkedList<TDNPoseCorrectionProviderListener> _listeners = new LinkedList<TDNPoseCorrectionProviderListener>();
	
	public TDNPoseCorrectionProvider(int serverPort, MoveController moveController, boolean availableWhenMoving, Logger logger, int poseMillisTimeout, int moveMillisTimeout) throws IOException
	{
		_controller = moveController;
		_controller.addMoveListener(this);
		_availableWhenMoving = availableWhenMoving;
		_receiver = new TDNReceiver(serverPort);
		_receiver.addListener(this);
		_logger = new VerbosityLogger(logger.createSubLogger("TDN Pose Corrector"));
		_logger.setVerbosityLevel(VerbosityLogger.DETAILED_OVERVIEW);
		_poseTimeoutSw = new Stopwatch();
		_poseMillisTimeout = poseMillisTimeout;
		_moveTimeoutSw = new Stopwatch();
		_moveMillisTimeout = moveMillisTimeout;
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
		return _lastPose != null && _poseTimeoutSw.elapsed() <= _poseMillisTimeout && 
		        (_availableWhenMoving || 
	                (!_controller.isMoving() 
                    && !_isMoving 
                    && _moveTimeoutSw.elapsed() >= _moveMillisTimeout));
	}

	long millis = System.currentTimeMillis();
	@Override
	public void rootReceived(TDNRoot root)
	{
	    _poseTimeoutSw.reset();
	    _logger.log("receive took " + (millis - System.currentTimeMillis()), VerbosityLogger.DEBUGGING);
	    millis = System.currentTimeMillis();
		float x = root.get("x").as();
		float y = root.get("y").as();
		float heading = root.get("heading").as();
		
		_lastPose = new Pose(x, y, heading);
		synchronized(_listeners)
        {
            for(TDNPoseCorrectionProviderListener l : _listeners)
                l.correctionReceived(_lastPose);
        }
		
        _logger.log("Received: " + _lastPose.toString(), VerbosityLogger.DEBUGGING);
	}
	
	public void addListener(TDNPoseCorrectionProviderListener l)
	{
	    synchronized(_listeners)
	    {	        
	        _listeners.add(l);
	    }
	}
	
	public void removeListener(TDNPoseCorrectionProviderListener l)
	{synchronized(_listeners)
        {
	        _listeners.remove(l);
        }
	}

    @Override
    public void tdnSenderConnected() 
    {
        synchronized(_listeners)
        {
            for(TDNPoseCorrectionProviderListener l : _listeners)
                l.correctorConnected();
    
        }
    }

    @Override
    public void moveStarted(Move event, MoveProvider mp) 
    {
        _isMoving = true;
    }

    @Override
    public void moveStopped(Move event, MoveProvider mp) 
    {
        _moveTimeoutSw.reset();
        _isMoving = false;
    }
}
