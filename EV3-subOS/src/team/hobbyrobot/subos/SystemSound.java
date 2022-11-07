package team.hobbyrobot.subos;

import lejos.hardware.Sound;

/**
 * Sounds used by subOS
 * 
 * @author David Krcmar
 * @version 0.1
 */
public class SystemSound
{
	private SystemSound() throws Exception
	{
		throw new Exception();
	}

	/**
	 * Play fatal error sounf
	 * 
	 * @param async True, when sound shoud be payed asynchronously
	 */
	public static void playFatalErrorSound(boolean async)
	{
		//Vytvor Thread ve kterem se zvuk zahraje
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Sound.buzz();
			}
		});
		//Pokud asynchronne -> Spust zvuk v novem threadu
		if (async)
			t.start();
		else
			t.run();
	}

    /**
     * Play non fatal error sounf
     * 
     * @param async True, when sound shoud be payed asynchronously
     */
	public static void playNonFatalErrorSound(boolean async)
	{
		//Vytvor Thread ve kterem se zvuk zahraje
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Sound.playTone(330, 400);
			}
		});
		//Pokud asynchronne -> Spust zvuk v novem threadu
		if (async)
			t.start();
		else
			t.run();
	}

}
