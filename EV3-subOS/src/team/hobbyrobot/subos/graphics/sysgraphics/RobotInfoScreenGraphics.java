package team.hobbyrobot.subos.graphics.sysgraphics;

import java.lang.reflect.Field;
import java.util.List;

import javax.sound.sampled.Port.Info;

import lejos.hardware.Brick;
import lejos.hardware.BrickInfo;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.GraphicsLCD;
import team.hobbyrobot.subos.graphics.Drawable;
import team.hobbyrobot.subos.menu.RobotInfoScreen;

public class RobotInfoScreenGraphics extends Drawable
{
	public List<RobotInfoScreen.NameValuePair> robotData = null;
	public int scroll = 0;

	String cutStringTo(String text, int number)
	{
		String temp = text.substring(0, Math.min(text.length(), number));
		return temp;
	}

	@Override
	protected void draw()
	{
		g.clear();

		//String.valueOf(robotData.get(index).Name/Value)
		// 178×128

		if (robotData != null)
		{
			Font lastFont = g.getFont();

			g.setFont(Font.getSmallFont());

			// maximalni pocet znaku pro Name
			int limitNameLength = 12;
			// maximalni pocet znaku pro vybrany Name
			int limitNameLenghtMax = 10;

			// maximalni pocet znaku pro Value
			int limitValueLength = 6;
			// maximalni pocet znaku pro vybrane Value
			int limitValueLengthMax = 5;

			// radkovani
			int yRowValue = 10;
			// odsazeni z leva
			int paddingLeft = 0;
			// odsazeni z prava
			int paddingRight = 0;
			
			// maximalni pocet radku zobrazenych malym fontem
			int rowLimit = 7;

			// y vykresleni vybraneho textu
			int yMaxRowValue = 92;

			// pocatecni index vykresleni hodnot
			int forStartIndex = 0;

			if (scroll + 1 > rowLimit)
				forStartIndex = (scroll + 1 - rowLimit);

			for (int i = forStartIndex; i < robotData.size(); i++)
			{
				if (i - forStartIndex == rowLimit)
					break;

				String infoName = String.valueOf(robotData.get(i).Name);
				String infoValue = String.valueOf(robotData.get(i).Value);

				String infoNameR = cutStringTo(infoName, limitNameLength);
				String infoValueR = cutStringTo(infoValue, limitValueLength);

				if (infoName.length() > limitNameLength)
					infoNameR = infoNameR + ".";

				if (infoValue.length() > limitValueLength)
					infoValueR = infoValueR + ".";

				if (i == scroll)
					infoNameR = "> " + infoNameR;

				g.drawString(infoNameR, paddingLeft, yRowValue, GraphicsLCD.TOP | GraphicsLCD.LEFT);
				g.drawString(infoValueR, g.getWidth() - (g.getFont().stringWidth(infoValueR)) - paddingRight,
					yRowValue, GraphicsLCD.TOP | GraphicsLCD.LEFT);

				yRowValue += 10;
			}

			g.setFont(Font.getDefaultFont());

			String infoName = String.valueOf(robotData.get(scroll).Name);
			String infoValue = String.valueOf(robotData.get(scroll).Value);

			String infoNameR = cutStringTo(infoName, limitNameLenghtMax);
			String infoValueR = cutStringTo(infoValue, limitValueLengthMax);

			if (infoName.length() > limitNameLenghtMax)
				infoNameR = infoNameR + ".";

			g.drawString(infoNameR, paddingLeft, yMaxRowValue, GraphicsLCD.TOP | GraphicsLCD.LEFT);
			g.drawString(infoValueR, g.getWidth() - (g.getFont().stringWidth(infoValueR)) - paddingRight, yMaxRowValue,
				GraphicsLCD.TOP | GraphicsLCD.LEFT);

			g.setFont(lastFont);
		}
	}

}
