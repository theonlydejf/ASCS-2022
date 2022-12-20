package team.hobbyrobot.robotmodeling;

import lejos.robotics.navigation.Move;

public interface RemoteSimpleMoveEventListener
{
	void moveStarted(int id, Move move);
	void moveStopped(int id, Move move);
}