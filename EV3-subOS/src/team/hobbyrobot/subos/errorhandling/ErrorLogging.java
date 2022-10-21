package team.hobbyrobot.subos.errorhandling;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.*;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.subos.SubOSController;
import team.hobbyrobot.subos.SystemSound;
import team.hobbyrobot.subos.graphics.GraphicsController;
import team.hobbyrobot.subos.hardware.BrickHardware;
import team.hobbyrobot.subos.hardware.LEDBlinkingStyle;

/**
 * Logovani erroru
 * 
 * @author David Krcmar
 * @version 1.0
 */
//TODO predelat na errorlogger .jnm
public class ErrorLogging
{
	/** KONSTANTA - Mezera mezi radky v error log screenu */
	public static final int LINE_SPACING = 2;
	/** KONSTANTA - "Rychlost" scrollování v errorLogScreen (X v pixelech, Y v radcich) */
	public static final int X_MOVE = 10, Y_MOVE = 1;

	/** Pozice "scrollovani" v errorLogScreen (X v pixelech, Y v radcich) */
	static int x = 0, y = 0;

	//Aby se nedala vytvorit instance teto tridy
	private ErrorLogging() throws Exception
	{
		throw new Exception();
	}

	/**
	 * Spusti ErrorLogScreen<br>
	 * <strong>! NELZE VRATIT !<br>
	 * ! PREPISE VSE NA OBRAZOVCE KROM INFOBARU !</strong>
	 */
	public static void startErrorLogScreen()
	{
		GraphicsController.setCurrentLayer(GraphicsController.ERROR_LAYER);
		GraphicsController.lockCurrentLayer();

		while (true)
		{
			//Updatuj grafiku
			updateErrorLogScreenGraphics();

			//Cekej nez se zmackne tlacitko
			int btn = Button.waitForAnyPress();
			//Podle toho jake se zmacklo -> pohni tak s textem
			if (btn == Button.ID_LEFT)
				x += X_MOVE;
			else if (btn == Button.ID_RIGHT)
				x -= X_MOVE;
			else if (btn == Button.ID_UP)
				y += Y_MOVE;
			else if (btn == Button.ID_DOWN)
				y -= Y_MOVE;
		}
	}

	private static void updateErrorLogScreenGraphics()
	{
		//Ziskej grafiku na kreselni
		GraphicsLCD g = GraphicsController.getNewMainGraphics(GraphicsController.ERROR_LAYER);

		String[] log = SubOSController.errorLogger.getLog();
		
		//Pokud nejsou zadne errory -> KONEC
		if (log.length <= 0)
			return;

		int SH = g.getHeight();

		g.setFont(Font.getSmallFont());

		//Dulezita cislo
		int fontHeight = Font.getSmallFont().getHeight();
		int lnCount = (SH - (SH % fontHeight)) / fontHeight + 1;

		//Uprav hodnotu scollovani tak aby byla platna
		if (y < 0)
			y = 0;
		if (y > log.length)
			y = log.length - 1;

		//Ziskej index odkud cist
		int from = (log.length - y - 1) - lnCount;
		if (from < 0)
			from = 0;
		//Ziskej index kam az cist
		int to = (log.length - y);
		if (to < 0)
			to = 0;

		//Ziskej radky ktere se vejdou na obrazovku
		String[] lnsToShow = Arrays.copyOfRange(log, from, to);

		if (lnCount > lnsToShow.length)
			lnCount = lnsToShow.length;

		//Vykresli vsechny radky
		for (int i = 0; i < lnCount; i++)
		{
			//Y pozice radku
			int y = SH - ((i * fontHeight) + i * LINE_SPACING);

			g.drawString(lnsToShow[lnsToShow.length - i - 1], x, y, GraphicsLCD.LEFT | GraphicsLCD.BOTTOM);
		}

		GraphicsController.refreshScreen();
	}

	/**
	 * Zaznamenej chybu
	 * 
	 * @param errorMsg Popis chyby
	 */
	public static void logError(String errorMsg)
	{
		SubOSController.errorLogger.log(errorMsg);
	}

	/**
	 * Zaznamenej fatalni chybu chybu
	 * 
	 * @param errorMsg Popis chyby
	 */
	public static void logFatalError(String errorMsg)
	{
		BrickHardware.setLEDPattern(2, LEDBlinkingStyle.DOUBLEBLINK, Integer.MAX_VALUE);
		SystemSound.playFatalErrorSound(true);
		logError("FATAL ERROR: " + errorMsg);
	}
}
