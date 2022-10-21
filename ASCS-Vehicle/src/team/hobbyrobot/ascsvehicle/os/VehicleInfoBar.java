package team.hobbyrobot.ascsvehicle.os;

import java.text.DecimalFormat;

import lejos.robotics.DirectionFinder;
import team.hobbyrobot.subos.graphics.infobar.BasicInfoBar;
import team.hobbyrobot.subos.hardware.RobotHardware;

public class VehicleInfoBar extends BasicInfoBar
{
	private ASCSVehicleHardware hardware;
	public VehicleInfoBar(RobotHardware hardware)
	{
		super(hardware);
		if(hardware instanceof ASCSVehicleHardware)
			this.hardware = (ASCSVehicleHardware)hardware;
	}

	@Override
	public String getLeftText()
	{
		DecimalFormat format = new DecimalFormat("#.##");
		if(hardware != null && hardware.getDirectionFinder() == null)
			return "NULL";
		return "G: " + format.format(hardware.getDirectionFinder().getDegreesCartesian());
	}
}
