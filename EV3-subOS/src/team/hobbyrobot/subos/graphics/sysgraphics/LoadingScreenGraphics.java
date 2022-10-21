package team.hobbyrobot.subos.graphics.sysgraphics;

import java.util.ArrayList;
import java.util.List;

import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.GraphicsLCD;
import lejos.hardware.lcd.Image;
import lejos.utility.Stopwatch;
import team.hobbyrobot.subos.LoadingScreen;
import team.hobbyrobot.subos.Referenceable;
import team.hobbyrobot.subos.graphics.Drawable;
import team.hobbyrobot.subos.graphics.GraphicsController;

/**
 * Grafika LoadingScreenu
 * 
 * @author David Krcmar and Zdenek Langer
 * @version 1.1
 */
public class LoadingScreenGraphics extends Drawable
{
	/** Velka mezera */
	private static final int largePadding = 10;
	/** Stredni mezera */
	private static final int mediumPadding = 5;
	/** Mala mezera */
	private static final int smallPadding = 2;

	private static final int progressBarHeight = 9;
	
	public LoadingScreen loadingScreen;
	
	/**
	 * Vytvori instanci teto tridy
	 * 
	 * @param loadingScreen LoadingScreen, pro ktery tato grafika je
	 */
	/*public LoadingScreenGraphics(LoadingScreen loadingScreen)
	{
		//params = new Object[] { loadingScreen };
	}*/

	@Override
	public void draw()
	{
		//LoadingScreen loadingScreen = (LoadingScreen) params[0];

		//
		//Vypocitej z kolika % je akce hotova
		//Spocitej z kolika procent jsou probihajici akce hotovi
		float partialPercentage = 0;
		for (Referenceable<Float> f : loadingScreen.threadFinishedPercentage)
		{
			partialPercentage += f.getValue();
		}
		//Spocitej z kolika procent je vse hotove
		float percentage = ((float) loadingScreen.finishedActionsCount + partialPercentage)
			/ loadingScreen.actions.length;	
		
		g.clear();
		//Nakresli HR Logo
		g.drawImage(SystemImages.HobbyRobotLogoImage, g.getWidth() / 2, largePadding, GraphicsLCD.TOP | GraphicsLCD.HCENTER);

		//Pozice Y progressbaru
		int progressBarY = SystemImages.HobbyRobotLogoImage.getHeight() + 2 * largePadding-3;
		//Nakresli progressbar
		drawProgressBar(g, progressBarY, percentage);
		
		//Vypis procenta
		g.setFont(Font.getSmallFont());
		//g.drawString(String.valueOf((int)(percentage * 100)) + "%", g.getWidth() / 2, progressBarY + progressBarHeight + 2, GraphicsLCD.TOP | GraphicsLCD.HCENTER);
		if(((int)(percentage * 100))<10)
		{
			g.drawString(String.valueOf((int)(percentage * 100)) + "%", 167, progressBarY+2, GraphicsLCD.TOP | GraphicsLCD.HCENTER);
		}
		else if(((int)(percentage * 100))<100)
		{
			g.drawString(String.valueOf((int)(percentage * 100)) + "%", 164, progressBarY+2, GraphicsLCD.TOP | GraphicsLCD.HCENTER);
		}
		else
		{
			g.drawString(String.valueOf((int)(percentage * 100)) + "%", 161, progressBarY+2, GraphicsLCD.TOP | GraphicsLCD.HCENTER);
		}
		
		
		
		//Pozice Y msg feedu
		int msgFeedY = progressBarY + progressBarHeight + 1 + Font.getSmallFont().height + smallPadding-3;
		//Nakresli msg feed
		drawMessageFeed(g, msgFeedY, loadingScreen.messageFeed);
		
		
		
		//g.drawString(String.valueOf(percentage), 0, g.getHeight(), GraphicsLCD.BOTTOM | GraphicsLCD.LEFT);
		GraphicsController.refreshScreen();
	}
	
	private void drawMessageFeed(GraphicsLCD g, int msgFeedY, ArrayList<String> msgFeed)
	{
		int height = g.getHeight() - msgFeedY - mediumPadding;
		int width = g.getWidth() - mediumPadding * 2;
		
		//Image, na kterou se bude kreslit text
		Image txtImg = Image.createImage(width - 2, height - 2);
		GraphicsLCD txtG = txtImg.getGraphics();
		
		if (msgFeed.size() > 0)
		{
			int txtHeight = g.getFont().getHeight() + smallPadding;
			int lnCount = ((txtImg.getHeight() + smallPadding) - ((txtImg.getHeight()) % txtHeight)) / txtHeight;
			
			//Ziskej index odkud cist
			int from = msgFeed.size() - lnCount - 1;
			if (from < 0)
				from = 0;
			//Ziskej index kam az cist
			int to = (msgFeed.size());
			if (to < 0)
				to = 0;

			//Ziskej radky ktere se vejdou na obrazovku
			List<String> lnsToShow = msgFeed.subList(from, to);
			
			//Vykresli text
			txtG.setFont(Font.getSmallFont());
			try
			{
				for (int i = 0; i < lnsToShow.size(); i++)
				{
					//Spocitej Y textu
					int y = txtImg.getHeight() - ((i * Font.getSmallFont().getHeight()) + i * smallPadding);
					
					txtG.drawString(lnsToShow.get(lnsToShow.size() - i - 1), 0, y, GraphicsLCD.LEFT | GraphicsLCD.BOTTOM);
				}
			}
			catch (Exception e)
			{
				txtG.setFont(Font.getDefaultFont());
				txtG.drawString("ERROR", txtG.getWidth() / 2, txtG.getHeight() / 2, GraphicsLCD.VCENTER | GraphicsLCD.HCENTER);
				txtG.setFont(Font.getSmallFont());
			}			
		}
		
		//Vykresli vse na display
		g.drawImage(txtImg, mediumPadding + 1, msgFeedY + 2, GraphicsLCD.TOP | GraphicsLCD.LEFT);
		drawBorder(g, mediumPadding, msgFeedY, width, height);
	}

	private void drawProgressBar(GraphicsLCD g, int progressBarYStart, float percentage)
	{
		//Vykresli ohraniceni progressbaru
		drawBorder(g, (largePadding-5), progressBarYStart, g.getWidth() - 15 - largePadding * 2, progressBarHeight);
		
		int percentageBarLength = g.getWidth() - 15 - 2 * largePadding - 4;
		//Vykresli vnitrek progressbaru
		g.fillRect((largePadding-5) + 2, progressBarYStart + 2, (int)(percentageBarLength * percentage), progressBarHeight - 3);
	}
	
	private void drawBorder(GraphicsLCD g, int x, int y, int w, int h)
	{
		g.drawLine(x + 1, y, x + w - 2, y);
		g.drawLine(x + 1, y + h, x + w - 2, y + h);

		g.drawLine(x, y + 1, x, y + h - 1);
		g.drawLine(x + w - 1, y + 1, x + w - 1, y + h - 1);
	}
}
