package team.hobbyrobot.subos.navigation;

import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.Pose;

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
	
}
