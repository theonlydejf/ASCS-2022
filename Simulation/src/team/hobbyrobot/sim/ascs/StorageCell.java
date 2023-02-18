package team.hobbyrobot.sim.ascs;

import team.hobbyrobot.sim.ascs.SimRobot.Pose;

public class StorageCell
{
	public StorageCell(Pose pose, int id)
	{
		super();
		this.pose = pose;
		this.id = id;
	}
	public Pose pose;
	public int id;
}
