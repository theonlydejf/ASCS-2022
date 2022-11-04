package team.hobbyrobot.ascsvehicle.os;

import lejos.robotics.DirectionFinder;
import lejos.robotics.localization.CompassPoseProvider;
import lejos.robotics.localization.OdometryPoseProvider;
import lejos.robotics.navigation.MoveProvider;
import lejos.robotics.navigation.Pose;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.subos.hardware.sensor.EV3Gyroscope;

/**
 * Pose Provider using an instance of EV3Gyroscope to provide 
 * location and heading data.
 * 
 * @author David Krcmar
 *
 */
public class CorrectableCompassPoseProvider extends OdometryPoseProvider
{
    private EV3Gyroscope _gyro;
    
    public CorrectableCompassPoseProvider(MoveProvider mp, EV3Gyroscope gyro) 
    {
        super(mp);
        _gyro = gyro;
    }
    
    @Override
    public Pose getPose()
    {
        Pose temp = super.getPose();
        temp.setHeading(_gyro.getDegreesCartesian());
        return temp;
    }
    
    @Override
    public void setPose(Pose aPose)
    {
        super.setPose(aPose);
        _gyro.resetAt((int) Math.round(aPose.getHeading()));
        Logger.main.log("Heading corrected using pose: " + aPose);
    }
}
