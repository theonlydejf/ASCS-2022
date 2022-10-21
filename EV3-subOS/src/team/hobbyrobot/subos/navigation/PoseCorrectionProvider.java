package team.hobbyrobot.subos.navigation;

import lejos.robotics.localization.PoseProvider;

public interface PoseCorrectionProvider extends PoseProvider
{
	boolean correctionAvailable();
}
