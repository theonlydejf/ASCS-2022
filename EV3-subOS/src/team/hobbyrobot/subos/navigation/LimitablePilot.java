package team.hobbyrobot.subos.navigation;

public interface LimitablePilot
{
	void setTravelLimit(double limit);
	double getTravelLimit();
	
	void setRotateLimit(double limit);
	double getRotateLimit();
	
	boolean wasLastMoveLimited();
}
