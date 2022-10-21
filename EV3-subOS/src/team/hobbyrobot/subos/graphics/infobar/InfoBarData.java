package team.hobbyrobot.subos.graphics.infobar;

/**
 * Trida, ve ktere jsou ulozena data pro InfoBar
 * 
 * @author David Krcmar
 * @version 1.0
 */
public interface InfoBarData
{
	/**
	 * Funkce, ktera vraci levy text InfoBaru
	 * 
	 * @return Levy text InfoBaru
	 */
	String getLeftText();

	/**
	 * Funkce, ktera vraci prostredni text InfoBaru
	 * 
	 * @return Prostredni text InfoBaru
	 */
	String getMiddleText();

	/**
	 * Funkce, ktera vraci pravy text InfoBaru
	 * 
	 * @return Pravy text InfoBaru
	 */
	String getRightText();
}
