package team.hobbyrobot.subos.graphics.sysgraphics;

import lejos.hardware.Sound;
import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.utility.Delay;
import team.hobbyrobot.subos.menu.MenuGraphics;

public class BasicMenuGraphics extends MenuGraphics
{

	//Promenna MenuItem[] menuItems -> jsou v ni ulozene itemy ktere jsou v menu (pro ziskani jmena: menuItems[i].getName() )
	//Promenna int selectedIndex -> vybrany index

	@Override
	protected void draw()
	{ 
		g.clear();

		String[] menuNames = new String[menuItems.length];
		for (int i = 0; i < menuItems.length; i++)
		{
			menuNames[i] = menuItems[i].getName();
		}

		drawMenu(menuNames, selectedIndex);
	}

	// Kontrola a osetreni, ze index (pozice) v menu existuje
	int checkMenuPosition(int menuLength, int position)
	{
		if (position >= menuLength)
			return 0;
		else if (position == -1)
			return (menuLength - 1);
		else if (position < -1)
			return 0;
		else
			return position;
	}

	void drawRoundedRect(GraphicsLCD g, int x, int y, int width, int height, int color, int strokeStyle,
		boolean checked)
	{
		g.setColor(color, color, color);
		int lastStroke = g.getStrokeStyle();
		g.setStrokeStyle(strokeStyle);

		g.drawRect(x, y, width, height);
		g.drawRect(x + 1, y + 1, width - 2, height - 2);

		g.setColor((color == 0) ? 255 : 0, (color == 0) ? 255 : 0, (color == 0) ? 255 : 0);

		g.fillRect(x, y, 2, 2); // Top Left
		g.fillRect(x + (width) - 1, y, 2, 2); // Top Right
		g.fillRect(x, y + height - 1, 2, 2); // Bottom Left
		g.fillRect(x + (width) - 1, y + height - 1, 2, 2); // Bottom Right

		g.setColor(color, color, color);

		g.fillRect(x + 1, y + 1, 2, 2); // Top Left
		g.fillRect(x + (width) - 2, y + 1, 2, 2); // Top Right
		g.fillRect(x + 1, y + height - 2, 2, 2); // Bottom Left
		g.fillRect(x + (width) - 2, y + height - 2, 2, 2); // Bottom Right

		if (checked != false)
		{
			// checkbox
			g.setColor(255, 255, 255);
			g.fillRect((x + 20), y, 20, 12);

			g.setColor(0, 0, 0);

			g.drawLine(x + 7, y + 10, x + 14, (y + 21));
			g.drawLine(x + 8, y + 10, x + 15, (y + 21));
			g.drawLine(x + 9, y + 10, x + 16, (y + 21));

			g.drawLine(x + 14, y + 21, x + 28, y);
			g.drawLine(x + 15, y + 21, x + 29, y);
			g.drawLine(x + 16, y + 21, x + 30, y);
		}

		g.setStrokeStyle(lastStroke);
	}

	void drawTextWithBorder(String txt, int x, int y, Font font)
	{
		int btnPadding = 6;

		Font lastFont = g.getFont();

		g.setFont(font);

		int txtWidth = g.getFont().stringWidth(txt);
		int txtHeight = g.getFont().getHeight();

		drawRoundedRect(g, x - txtWidth / 2 - btnPadding, y - txtHeight / 2 - btnPadding - 1, txtWidth + btnPadding * 2,
			txtHeight + btnPadding * 2, GraphicsLCD.BLACK, GraphicsLCD.SOLID, false);

		g.drawString(txt, x, y - txtHeight / 2, GraphicsLCD.TOP | GraphicsLCD.HCENTER);

		g.setFont(lastFont);
	}

	void drawMenu(String[] menu, int position)
	{
		// odsazeni menu od Infobaru
		int menuPaddingTop = 8;

		g.setFont(Font.getDefaultFont());

		g.clear();

		int j = 1;

		if (position > 0)
		{
			// vykresleni menu o krok nahoru
			g.drawString(menu[checkMenuPosition(menu.length, (position - 1))], g.getWidth() / 2,
				(g.getHeight() / 2) + menuPaddingTop - 55 + 3, GraphicsLCD.TOP | GraphicsLCD.HCENTER);

			// sipka nahoru
			for (int i = 0; i < 6; i++)
			{
				g.drawLine((g.getWidth() / 2) - j, (g.getHeight() / 2) + menuPaddingTop - (31 - i),
					(g.getWidth() / 2) + j, (g.getHeight() / 2) + menuPaddingTop - (31 - i));
				j += 3;
			}
		}

		// aktualni poloha v menu
		drawTextWithBorder(menu[position], g.getWidth() / 2, (g.getHeight() / 2) + menuPaddingTop - 6,
			Font.getDefaultFont());

		if (position < (menu.length - 1))
		{
			// sipka dolu
			j = 1;
			for (int i = 6; i > 0; i--)
			{
				g.drawLine((g.getWidth() / 2) - j, (g.getHeight() / 2) + menuPaddingTop + (11 + i),
					(g.getWidth() / 2) + j, (g.getHeight() / 2) + menuPaddingTop + (11 + i));
				j += 3;
			}

			g.setFont(Font.getDefaultFont());

			// poloha v menu o krok vpred
			g.drawString(menu[checkMenuPosition(menu.length, (position + 1))], g.getWidth() / 2,
				(g.getHeight() / 2) + menuPaddingTop + 25, GraphicsLCD.TOP | GraphicsLCD.HCENTER);
		}
	}
}
