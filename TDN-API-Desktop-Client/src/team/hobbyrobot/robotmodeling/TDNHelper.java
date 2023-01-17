package team.hobbyrobot.robotmodeling;

import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class TDNHelper
{
	public static TDNValue[] extractTDNValues(TDNRoot root, String... keys)
	{
		TDNValue[] out = new TDNValue[keys.length];
		for (int i = 0; i < keys.length; i++)
		{
			out[i] = root.get(keys[i]);
		}

		for (TDNValue val : out)
		{
			if (val == null)
				return null;
		}

		return out;
	}
	
	public static Move getMoveFromTDN(TDNRoot root)
	{
		// @formatter:off
		TDNValue[] vals = extractTDNValues(root, 
			"type", 
			"distance", 
			"angle", 
			"travelSpeed", 
			"rotateSpeed"
		);
		// @formatter:on

		if (vals == null)
			return null;

		Move.MoveType moveType = Move.MoveType.valueOf(vals[0].as());
		float distance = vals[1].as();
		float angle = vals[2].as();
		float travelSpeed = vals[3].as();
		float rotateSpeed = vals[4].as();

		return new Move(moveType, distance, angle, travelSpeed, rotateSpeed, false);
	}

	public static Pose getPoseFromTDN(TDNRoot root)
	{
		// @formatter:off
		TDNValue[] vals = extractTDNValues(root, 
			"x", 
			"y", 
			"heading"
		);
		// @formatter:on

		if (vals == null)
			return null;

		float x = vals[0].as();
		float y = vals[1].as();
		float heading = vals[2].as();

		return new Pose(x, y, heading);
	}

	public static Waypoint getWaypointFromTDN(TDNRoot root)
	{
		// @formatter:off
		TDNValue[] vals = extractTDNValues(root, 
			"x", 
			"y",
			"headingRequired"
		);
		// @formatter:on

		if (vals == null)
			return null;

		Float x = vals[0].as();
		Float y = vals[1].as();

		if ((boolean) vals[2].value)
		{
			TDNValue headingTDN = root.get("heading");
			if (headingTDN == null)
				return null;

			return new Waypoint(x.doubleValue(), y.doubleValue(), ((Float)headingTDN.as()).doubleValue());
		}

		return new Waypoint(x, y);
	}

}
