package team.hobbyrobot.subos.graphics.sysgraphics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.hardware.lcd.Image;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.subos.errorhandling.ErrorLogging;
import team.hobbyrobot.subos.hardware.BrickHardware;

public class SystemImages
{
	/** Sirka chyboveho Image */
	private static final int errImgWidth = 50;
	/** Vyska chyboveho Image */
	private static final int errImgHeight = 50;

	/** Image, ve kterem je logo HobbyRobot (Potrebuje soubor ulozeny v {@link subOS.Constants.FileConstants#HRLogoFileName}) */
	public static Image HobbyRobotLogoImage = createImg("HRLogo.lni");

	private static Image createImg(String fileName)
	{
		Image img;
		try
		{
			//Vytvor image ze soubor
			img = Image.createImage(new FileInputStream(new File(fileName)));
		}
		catch (IOException e)
		{
			//
			//Pokud to neslo
			//Rekni ze error
			BrickHardware.blinkLED(2, 300, true, Integer.MAX_VALUE);
			ErrorLogging.logError("Error loading Image;" + Logger.getExceptionInfo(e));
			
			//
			//Uloz do img chybovej img
			//Priprav image
			img = Image.createImage(errImgWidth, errImgHeight);
			GraphicsLCD g = img.getGraphics();
			g.setFont(Font.getSmallFont());
			
			//Nakresli to
			g.fillRect(0, 0, g.getWidth(), g.getHeight());
			g.drawString("ERR", g.getWidth() / 2, g.getHeight() / 2, GraphicsLCD.HCENTER | GraphicsLCD.VCENTER, true);
		}

		return img;
	}
}
