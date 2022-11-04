package team.hobbyrobot.ascsvehicle.navigation;

import lejos.robotics.navigation.Pose;

public interface TDNPoseCorrectionProviderListener 
{
    void correctorConnected();
    void correctionReceived(Pose newPose);
}
