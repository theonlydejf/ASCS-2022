/**
 * 
 */
package team.hobbyrobot.subos.graphics;

import lejos.hardware.lcd.GraphicsLCD;

/**
 * Interface, do ktereho se delaji grafiky. Na konci prepsane funkce Draw() neni potreba volat funkce
 * {@link subOS.Graphics.GraphicsController#RefreshScreen()}
 * 
 * @author David Krcmar
 * @version 2.0
 */
public abstract class Drawable
{
	/**  Grafika, na kterou se kresli */
	protected GraphicsLCD g = null;

	/**
	 * Funkce, ve ktere se vykresluje dana vec
	 * 
	 * @param g GraphicsLCD na ktery se kresli
	 */
	protected abstract void draw();

	/**
	 * Vykresli danou grafiku
	 * 
	 * @param g Grafika, na kterou se ma vykreslovat
	 */
	public final void draw(GraphicsLCD g)
	{
		this.g = g;
		draw();
		GraphicsController.refreshScreen();
	}

}
