package team.hobbyrobot.subos.graphics.infobar;

import lejos.hardware.Sound;
import lejos.hardware.lcd.*;
import lejos.utility.Delay;
import team.hobbyrobot.subos.graphics.GraphicsController;

/**
 * Trida, ktera zarizuje fungovani InfoBaru
 * 
 * @author David Krcmar
 * @version 1.0
 */
public class InfoBarController
{
	/** KONSTANTA - Velikost mezer v InfoBaru */
	public static final int VERTICAL_PADDING = 2;
	/** KONSTANTA - Delay mezi updatovanim InfoBaru */
	public static final int UPDATE_PERIOD = 50;
	/** KONSTANTA - Vyska InfoBaru */
	private static int _height = Font.getSmallFont().getHeight() + (VERTICAL_PADDING * 2) + 1;
	/** KONSTANTA - Sirka InfoBaru */
	private static final int _width = GraphicsController.ScreenWidth;
	
	/** FLAG - True, pokud se ma zrusit updatovani InfoBaru */
	private static boolean _stopUpdating = false;
	/** Thread, ktery updatuje InfoBar */
	private static Thread _updateThread;
	/** GraphicsLCD InfoBaru */
	private static GraphicsLCD _graphics;
	
	/** Data, ktera se budou zobrazovat v InfoBaru */
	public static InfoBarData infoBarData;

	private InfoBarController() throws Exception
	{
		throw new Exception();
	}
	
	public static int getWidth()
	{
		return _width;
	}
	
	public static int getHeight()
	{
		return _height;
	}

	/**
	 * Pripravi InfoBarController<br>
	 * <strong>! JE POTREBA ZAVOLAT TUTO FUNKCI HNED PO INICIALIZACI GRAPHICSCONTROLLERU !</strong>
	 */
	public static void init()
	{
		//Priprav grafiku
		_graphics = GraphicsController.getNewInfoBarGraphics();
		_graphics.setFont(Font.getSmallFont());

		//Nakresli neco na grafiku, at tam neni prazdon
		drawStartingImage();
		GraphicsController.refreshInfoBar();
	}

	/** Zacni updatovat InfoBar */
	public static void start()
	{
		if (_updateThread != null && _updateThread.isAlive())
			return;


		//Vytvor updatovaci Thread
		_updateThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				//Opakuj, dokud se nema skoncit
				while (!_stopUpdating)
				{
					//Updatuj Grafiku
					update();
					GraphicsController.refreshInfoBar();

					Thread.yield();
					
					//Pockej nejakou dobu
					try
					{
						Thread.sleep(UPDATE_PERIOD);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
				_stopUpdating = false;
			}
		});
		
		_updateThread.setPriority(Thread.MIN_PRIORITY);
		_updateThread.setDaemon(true);
		_updateThread.start();
	}

	/** Prestan updatovat InfoBar */
	public static void stop()
	{
		if (!_updateThread.isAlive())
			return;

		_stopUpdating = true;

		while (_updateThread.isAlive());
	}

	/** Updatuj InfoBar */
	public static void update()
	{
		_graphics.clear();
		drawLeftText();
		drawRightText();
		drawMiddleText();
		_graphics.setStrokeStyle(GraphicsLCD.DOTTED);
		_graphics.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
		_graphics.setStrokeStyle(GraphicsLCD.SOLID);
	}

	/** Nakresli levy text */
	private static void drawLeftText()
	{
		_graphics.drawString(infoBarData.getLeftText(), VERTICAL_PADDING, 3, GraphicsLCD.TOP | GraphicsLCD.LEFT);
	}

	/** Nakresli pravy text */
	private static void drawRightText()
	{
		_graphics.drawString(infoBarData.getRightText(), getWidth() - VERTICAL_PADDING, 3, GraphicsLCD.TOP | GraphicsLCD.RIGHT);
	}

	/** Nakresli text uprostred */
	private static void drawMiddleText()
	{
		_graphics.drawString(infoBarData.getMiddleText(), getWidth() / 2, 3, GraphicsLCD.TOP | GraphicsLCD.HCENTER);
	}

	/** Nakresli neco, co rika ze se InfoBar teprve zapina */
	private static void drawStartingImage()
	{
		_graphics.fillRect(0, 0, getWidth(), getHeight() - 1);
		_graphics.drawString("Starting...", getWidth() / 2, 3, GraphicsLCD.TOP | GraphicsLCD.HCENTER, true);
		_graphics.setStrokeStyle(GraphicsLCD.DOTTED);
		_graphics.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
		_graphics.setStrokeStyle(GraphicsLCD.SOLID);
	}
}
