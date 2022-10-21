package team.hobbyrobot.subos.navigation;

/**
 * Class used to calculate current speed of a accelerating robot
 * @author David Krcmar
 *
 */
public class Accelerator
{
	private double currSpeed;
	private long lastNano;
	private boolean firstTime;
	
	/**
	 * Creates instance of {@link Accelerator}
	 * @param startSpeed Speed, at which the accelerator should start
	 */
	public Accelerator(double startSpeed)
	{
		currSpeed = startSpeed;
		firstTime = true;
	}
	
	/**
	 * Gets current speed of the accelerating robot
	 * @param acceleration Acceleration the robot has been accelerating, until this  method was called (use negative values for decelerating) in speed units / sec
	 * @return Current speed in speed units / sec
	 */
	public double getCurrentSpeed(double acceleration)
	{
		if(firstTime)
		{
			lastNano = System.nanoTime();
			firstTime = false;
		}
		
		double deltaTime = (System.nanoTime() - lastNano) / 1000000000D;
		lastNano = System.nanoTime();
					
		currSpeed += acceleration * deltaTime;
		return currSpeed;
	}
	
	public void reset(double startSpeed)
	{
		currSpeed = startSpeed;
		firstTime = true;
	}
}