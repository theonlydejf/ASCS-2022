package team.hobbyrobot.subos;

import lejos.hardware.Sound;

/**
 * Jine typy zvuku
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
	 * Zahraj fatal error zvuk
	 * 
	 * @param async Pokud true - Zvuk bude asynchronne
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
	 *  
	 * Zahraj non fatal error zvuk
	 * 
	 * @param async Pokud true - Zvuk bude asynchronne
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
