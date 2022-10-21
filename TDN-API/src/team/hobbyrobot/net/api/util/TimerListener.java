package team.hobbyrobot.net.api.util;

/**
 * Listener used with Timer.
 * 
 * @see lejos.utility.Timer
 * @author <a href="mailto:rvbijl39<at>calvin<dot>edu">Ryan VanderBijl</a> for LeJOS
 */

public interface TimerListener
{
   /**
    * Called every time the Timer fires.
    */
   public void timedOut();
}
