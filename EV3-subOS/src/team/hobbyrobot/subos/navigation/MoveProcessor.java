package team.hobbyrobot.subos.navigation;

import lejos.robotics.navigation.Move;

public interface MoveProcessor
{
	/** Gets called befor start of any movement and prepares handler for a new move. */
	void reset();

	/**
	 * Is called repeatedely while controller handles a move. This method should control motors.
	 * @return True, when a move should finish
	 */
	boolean step(Move targetMove);
}
