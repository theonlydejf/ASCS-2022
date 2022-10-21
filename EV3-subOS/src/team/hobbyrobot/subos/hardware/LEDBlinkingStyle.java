package team.hobbyrobot.subos.hardware;

/**
 * Zbuspob, kterym bude blikat LED
 * 
 * @author David Krcmar
 * @version 1.0
 */
public enum LEDBlinkingStyle
{
	/** Nebilka */
	NONE(0),
	/** Konstantni blikani */
	NORMAL(1),
	/** Defaultni blikani v ev3 ("dvojbliknuti") */
	DOUBLEBLINK(2);

	/** ID daneho stylu blikani */
	public final int id;

	LEDBlinkingStyle(int id)
	{
		this.id = id;
	}

	/**
	 * Vrati pattern LED
	 * 
	 * @param Color Barva, kterou LED bude svitit<br>
	 *              0 - Vypnuto<br>
	 *              1 - Zelena<br>
	 *              2 - Cervena<br>
	 *              3 - Oranzova
	 * @return Cislo, ktere rika jaka pattern se ma pouzit pri Button.LEDPattern(int)
	 */
	public int getLEDPatternID(int color)
	{
		if (color == 0)
			return 0;

		return color + 3 * this.id;
	}
}
