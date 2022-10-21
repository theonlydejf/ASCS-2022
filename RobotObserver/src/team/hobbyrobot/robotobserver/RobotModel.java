package team.hobbyrobot.robotobserver;

import org.json.simple.JSONObject;

import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class RobotModel
{
	public long id;
	public double x;
	public double y;
	public double heading;
	
	public RobotModel(long id, double x, double y, double heading)
	{
		this.id = id;
		this.x = x;
		this.y = y;
		this.heading = heading;
	}
	
	public static RobotModel fromJSON(JSONObject obj)
	{
		return new RobotModel(
			(long)obj.get("id"),
			toDoubleJSON(obj.get("x")),
			toDoubleJSON(obj.get("y")),
			toDoubleJSON(obj.get("heading"))
			);
	}
	
	private static double toDoubleJSON(Object num)
	{
		if(num instanceof Long)
			return (long)num;
		return (double)num;
	}
	
	public String toString()
	{
		return String.format("ID %d: pos=[%f.2, %f.2], ang=%f.2", id, x, y, heading);
	}
	
	public TDNRoot toTDN()
	{
		return new TDNRoot()
			.insertValue("id", new TDNValue((int)id, TDNParsers.INTEGER))
			.insertValue("x", new TDNValue((float)x, TDNParsers.FLOAT))
			.insertValue("y", new TDNValue((float)y, TDNParsers.FLOAT))
			.insertValue("heading", new TDNValue((float)heading, TDNParsers.FLOAT));
	}
	
	public double getHeadingRad()
	{
		return heading/180 * Math.PI;
	}
}
