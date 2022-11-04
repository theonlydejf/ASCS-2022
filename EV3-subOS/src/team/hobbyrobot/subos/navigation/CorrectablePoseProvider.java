package team.hobbyrobot.subos.navigation;

import lejos.robotics.DirectionFinder;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.Pose;
import team.hobbyrobot.logging.Logger;

public class CorrectablePoseProvider implements PoseProvider
{
	public CorrectablePoseProvider(PoseProvider main, PoseCorrectionProvider corrector)
	{
		_corrector = corrector;
		_main = main;
	}

	private PoseCorrectionProvider _corrector;
	private PoseProvider _main;
	
	public void correct()
	{
	   Logger.main.log("Trying to correcto pose. Available: " + _corrector.correctionAvailable());
       if(_corrector.correctionAvailable())
            _main.setPose(_corrector.getPose());
	}
	
	@Override
	public Pose getPose()
	{
	    correct();
		
		return _main.getPose();
	}

	@Override
	public void setPose(Pose aPose)
	{
		_main.setPose(aPose);
		_corrector.setPose(aPose);
	}
	
	public PoseProvider getMainProvider()
	{
	    return _main;
	}
	
	public PoseCorrectionProvider getCorrectionProvider()
	{
	    return _corrector;
	}
}
