package team.hobbyrobot.subos.hardware;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.utility.Stopwatch;

/**
 * Trida ve ktere jsou vlasni funkce na pristup k vecem na kostce (LED, talcitka)<br>
 * Tyto funkce podporuji vse potrebne k hladkemu prubehu programu (napr. vypinani pri preruseni jizdy)
 * 
 * @author David Krcmar
 * @version 0.1
 */

public class BrickHardware
{
	private BrickHardware() throws Exception
	{
		throw new Exception();
	}
	
	private static int currBGPriority = 0;
	private static int currLEDPriority = 0;
	
	private static int currBGColor = 0;
	private static LEDBlinkingStyle currBGBlinkingStyle = LEDBlinkingStyle.NONE;
	
	/**
	 * Rozsviti LED mezi tlacitky
	 * 
	 * @param color            Barva, kterou LED bude svitit<br>
	 *                         0 - Vypnuto<br>
	 *                         1 - Zelena<br>
	 *                         2 - Cervena<br>
	 *                         3 - Oranzova
	 * @param blinkingStyle    Zpusob, jakym LED bude blikat
	 * @param wait             Jestli se ma cekat na to, az se nastavi LED pattern
	 * @param tCollisionAction Co se ma stat, pokud se neco deje s LED v jinem vlakne
	 */
	public synchronized static void setLEDPattern(int color, LEDBlinkingStyle blinkingStyle, int priority)
	{
		if(currBGPriority > priority)
			return;
		
		currBGPriority = priority;
		currBGColor = color;
		currBGBlinkingStyle = blinkingStyle;
		
		if(currLEDPriority > priority)
			return;
		
		currLEDPriority = priority;
		setLEDPattern(blinkingStyle.getLEDPatternID(color));
	}

	/**
	 * Blikni s LED ( = Na chvili zmen barvu)
	 * 
	 * @param color            Barva, kterou LED bude svitit<br>
	 *                         0 - Vypnuto<br>
	 *                         1 - Zelena<br>
	 *                         2 - Cervena<br>
	 *                         3 - Oranzova
	 * @param duration         Delka bliknuti [ms]
	 * @param async            Jestli se ma cekat na to, az bude LED dostupne
	 * @param tCollisionAction Co se ma stat, pokud se neco deje s LED v jinem vlakne
	 */
	public static synchronized void blinkLED(final int color, final int duration, final boolean async, final int priority)
	{
		if(currLEDPriority > priority)
			return;
		currLEDPriority = priority;
		
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				setLEDPattern(color);

				//Cekej danou dobu ve whilu. While protoze je potreba vyzkouset jestli neni LED overrided
				//nebo neni pozadovani prepsani LED
				Stopwatch sw = new Stopwatch();
				sw.reset();
				while (sw.elapsed() < duration)
					Thread.yield();

				//Vrat LED do puvodniho stavu
				setLEDPattern(currBGBlinkingStyle.getLEDPatternID(currBGColor));
				if(currLEDPriority <= priority)
					currLEDPriority = currBGPriority;
			}
		});

		if (async)
			t.start();
		else
			t.run();
	}
	
	public static void releasePriority(int myPriority, int requestedPriority)
	{
		if(currLEDPriority > myPriority)
			return;
		currLEDPriority = requestedPriority;
		
		if(currBGPriority > myPriority)
			return;
		currBGPriority = requestedPriority;
	}
	
	private static void setLEDPattern(int pattern)
	{
		Button.LEDPattern(pattern);
	}
}
