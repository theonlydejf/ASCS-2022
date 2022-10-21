package team.hobbyrobot.subos.graphics;

import lejos.hardware.BrickFinder;
import lejos.hardware.Sound;
import lejos.hardware.lcd.*;
import lejos.utility.Stopwatch;
import team.hobbyrobot.subos.errorhandling.ErrorLogging;
import team.hobbyrobot.subos.graphics.infobar.InfoBarController;

/**
 * Trida, ktera se stara o kresleni na display.<br>
 * <strong>!POUZIVAT POUZE TUTO TRIDU PRO ZISKAVANI ODKAZU NA KRESLENI</strong><br>
 * Kvuli poctu bugu, byl v teto verzi odstranen auto refreshing
 * 
 * @author David Krcmar
 * @version 2.1
 */
public class GraphicsController
{
	/** KONSTANTA - Po jake dobe od posledniho refreshnuti displaye se ma znovu refreshovat (v ms) */
	public static final int REFRESH_DELAY = 40; //ms
	/**
	 * KONSTANTA - Maximalne jak dlouho muze trvat jedno kresleni (= doba mez zavolani funkci StartDrawing() a
	 * StopDrawing())
	 */
	/** KONSTANTA - Pocet vrstev pro kresleni */
	public static final int LAYER_COUNT = 2;
	/** KONSTANTA - Vychozi vrstva pro kresleni */
	public static final int DEFAULT_LAYER = 0;
	/**
	 * KONSTANTA - Vrstva, ktera se pouziva pro error screen (Pokud mozno nepouzivat, pokud nevite co delate)
	 */
	public static final int ERROR_LAYER = 1;

	/** FLAG - True pokud se ma automaticky updatovat obrazovka */
	public static boolean autoUpdateGraphics = false;
	/**
	 * FLAG - True pokud neni nejaka funkce uprostred vykreslovani a jsou tedy vsechny vrstvy pripraveny k
	 * vykresleni (Byla zavolana funkce StopDrawing())
	 */
	private static boolean readyForRefresh = true;
	/** FLAG - True pokud jsou vrstvy zamknute */
	private static boolean isLayerLocked = false;

	/** Pokud je true -> upozorni na neco (defaultne invertovanim displaye) */
	public static boolean alert = false;
	/**
	 * Pokud je true -> Vykresluj InfoBar; Pokud je false -> Nevykresluj InfoBar => Vetsi plocha pro kresleni
	 * a mensi zatez na kostku
	 */
	public static boolean showInfoBar = true;
	/** Aktualni vrstva, ktera se ma vykreslovat */
	private static int acGraphicsLayer = 0;

	/** GraphicsLCD ktere odkazuje primo na obdazovka na EV3 */
	private static GraphicsLCD ScreenGraphics;
	/** Sirka cele obrazovky */
	public static int ScreenWidth;
	/** Vyska cele obrazovky */
	public static int ScreenHeight;

	/** Image ve ktere je ulozene to, co se ma zobrzovat na miste InfoBaru */
	private static Image InfoBarGraphicsRegion;
	/** Array Imagu. Je v nem ulozeno to, co se ma zobrazovat na dane vrstve v hlavni casti obrazovky */
	private static Image[] MainGraphicsRegionLayers = new Image[LAYER_COUNT];

	/** Pokud je true -> Zrovna se prekresluje obrazovka */
	private static boolean refreshingScreen = false;

	/** Thread, ktery automaticky refreshuje obrazovku */
	private static Thread AutoRefreshThread;

	private GraphicsController() throws Exception
	{
		throw new Exception();
	}

	/**
	 * Pripravi GraphicsController<br>
	 * <strong>! JE POTREBA ZAVOLAT TUTO FUNKCI UPLNE NA ZACATKU BEHU subOS !</strong>
	 */
	public static void init()
	{
		//Ziskej odkaz k GraphicsLCD na kresleni na display
		ScreenGraphics = BrickFinder.getDefault().getGraphicsLCD();
		ScreenWidth = ScreenGraphics.getWidth();
		ScreenHeight = ScreenGraphics.getHeight();

		ScreenGraphics.clear();

		//Inicializuj vrstvy
		for (int i = 0; i < MainGraphicsRegionLayers.length; i++)
			MainGraphicsRegionLayers[i] = Image.createImage(ScreenWidth, ScreenHeight - InfoBarController.getHeight());

		//Inicializuj Image InfoBaru
		InfoBarGraphicsRegion = Image.createImage(ScreenWidth, InfoBarController.getHeight());

		//Vytvor thred pro auto refreshovani obrazovky
		AutoRefreshThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Stopwatch refreshSw = new Stopwatch();

				//Refreshuj dokud se ma autorefreshovat
				while (autoUpdateGraphics)
				{
					//Pokud je cas refreshovat
					if (refreshSw.elapsed() >= REFRESH_DELAY)
					{
						//Rekni ze kreslis na obrazovku
						refreshingScreen = true;

						//Pockej dokud nebude mozno refreshnout
						while (!(readyForRefresh || !autoUpdateGraphics))
							Thread.yield();

						refreshScreen();
						refreshSw.reset();

						//Rekni ze uz nekreslis na obrazovku
						refreshingScreen = false;
					}
				}
			}
		});
		AutoRefreshThread.setPriority(Thread.MIN_PRIORITY);
		AutoRefreshThread.setDaemon(true);
	}

	/**
	 * Smaze obsah vychozi vrstvy hlavni casti obrazovky a vrati odkaz k GraphicsLCD pro kresleni na ni
	 * 
	 * @return GraphicsLCD vychozi vrstvy hlavni casti obrazovky
	 */
	public static GraphicsLCD getNewDefaultMainGraphics()
	{
		return getNewMainGraphics(DEFAULT_LAYER);
	}

	/**
	 * Smaze obsah dane vrstvy hlavni casti obrazovky a vrati odkaz k GraphicsLCD pro kresleni na ni
	 * 
	 * @param layerIndex 0-based index vrstvy hlavni casti obrazovky, na kterou chcete kreslit
	 * @return GraphicsLCD dane vrstvy hlavni casti obrazovky
	 */
	public static GraphicsLCD getNewMainGraphics(int layerIndex)
	{
		//Pokud je InfoBar -> Vrat cast obrazovky, pokud ne -> Vrat celou obrazovku + smaz danou vrstvu
		if (showInfoBar)
			MainGraphicsRegionLayers[layerIndex] = Image.createImage(ScreenWidth,
				ScreenHeight - InfoBarController.getHeight());
		else
			MainGraphicsRegionLayers[layerIndex] = Image.createImage(ScreenWidth, ScreenHeight);

		return MainGraphicsRegionLayers[layerIndex].getGraphics();
	}

	/**
	 * Smaze obsah InfoBaru a vrati odkaz k GraphicsLCD pro kresleni na nej
	 * 
	 * @return GraphicsLCD InfoBaru
	 */
	public static GraphicsLCD getNewInfoBarGraphics()
	{
		//Smaz InfoBar
		InfoBarGraphicsRegion = Image.createImage(ScreenWidth, InfoBarController.getHeight());

		return InfoBarGraphicsRegion.getGraphics();
	}

	/**
	 * Rekne, jaka vrstva hlavni casti obrazovky se ma vykreslovat
	 * 
	 * @param layerIndex 0-based index vrstvy, ktera se ma vykreslovat
	 */
	public static void setCurrentLayer(int layerIndex)
	{
		//Pokud je vrstva zamcena -> zaznamenj chybu
		if (isLayerLocked)
		{
			ErrorLogging.logError("Tried to change Graphics Layer from " + acGraphicsLayer + " to " + layerIndex
				+ " when layers were locked.");
			return;
		}

		acGraphicsLayer = layerIndex;
	}

	/**
	 * Zamezi dalsimu meneni vrstev, ktere se maji vykreslovat. Od zavolani teto funkce se bude vykreslovat
	 * pouze vrstva, ktera byla nastavena jako posledni<br>
	 * <strong>! NELZE VRATIT ZPET !</strong>
	 */
	public static void lockCurrentLayer()
	{
		isLayerLocked = true;
	}

	/**
	 * Pocka na to, az se nebude vykreslovat neco na obrazovku. Pokud se nic nevykresluje v dobe zavolani
	 * funkce, nebude se cekat.<br>
	 * <strong>Doporucuji zavolat tuto funkci po kazdem vykresleni v loopu</strong> (Pokud nechcete zpomalovat
	 * loop, pro vyhnuti chybam pri vykreslovani kreslete jen pouze pokud vystup z funkce
	 * isScreenRefreshing() je true)
	 */
	private static void waitForNextFrame()
	{
		while (refreshingScreen)
			Thread.yield();
	}

	/**
	 * Zacne automaticky refreshvat obrazovku.<br>
	 * Rycholst refreshovani je dana konstantou graphicsRefreshDelay.
	 */
	private static void startAutoScreenRefreshing()
	{
		if (autoUpdateGraphics)
			return;

		autoUpdateGraphics = true;
		AutoRefreshThread.start();
	}

	/** Zrusi automaticke refreshovani obrazovky. */
	private static void stopAutoScreenRefreshing()
	{
		if (!autoUpdateGraphics)
			return;

		autoUpdateGraphics = false;
		while (AutoRefreshThread.isAlive())
			Thread.yield();
	}

	/**
	 * Refreshne obrazovku (= vykresli na ni aktualni obsah InfoBaru a aktualni vrstva hlavni casti obrazovky)
	 * <strong>a to i pokud je flag readForRefresh false</strong>
	 */
	public static void refreshScreen()
	{
		if (refreshingScreen)
			return;

		refreshingScreen = true;

		Image img = MainGraphicsRegionLayers[acGraphicsLayer];
		if (alert)
		{
			img = new Image(img.getWidth(), img.getHeight(),
				invertByteArr(MainGraphicsRegionLayers[acGraphicsLayer].getData()));
		}

		//Vykresli aktualni vrstvu
		ScreenGraphics.drawImage(img, 0, showInfoBar ? InfoBarController.getHeight() : 0,
			GraphicsLCD.TOP | GraphicsLCD.LEFT);
		refreshingScreen = false;
	}

	public static void refreshInfoBar()
	{
		//Pokud mas vykreslit InfoBar -> vykresli ho
		if (showInfoBar)
		{
			Image img = InfoBarGraphicsRegion;
			if (alert)
			{
				img = new Image(img.getWidth(), img.getHeight(), invertByteArr(InfoBarGraphicsRegion.getData()));
			}

			ScreenGraphics.drawImage(img, 0, 0, GraphicsLCD.TOP | GraphicsLCD.LEFT);
		}
	}

	private static byte[] invertByteArr(byte[] array)
	{
		byte[] newArr = new byte[array.length];
		for (int i = 0; i < array.length; i++)
		{
			newArr[i] = (byte) (~array[i] & 0xff);
		}
		return newArr;
	}

	/**
	 * Refreshne asynchorne obrazovku (= vykresli na ni aktualni obsah InfoBaru a aktualni vrstva hlavni casti
	 * obrazovky) <strong>a to i pokud je flag readForRefresh false</strong>
	 */
	public static void refreshScreenAsync()
	{
		if (refreshingScreen)
			return;

		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				refreshScreen();
			}

		});
		
		t.start();
	}

	/** @return True, pokud se prave vykresluje na obrazovku */
	public static boolean isScreenRefreshing()
	{
		return refreshingScreen;
	}

	/**
	 * Pozastavi refreshovani obrazovky dokud se nezavola funkce StopDrawing(). Dobre pouzivat pokazde, nez
	 * zacnete neco kreslit na hlavni cast obrazovky.<br>
	 * Pokud po dobe dane konstantou maxDrawingTime nebude zavolana funkce StopDrawing(), automaticky se
	 * vykresli na obrazovku obsah aktualni vrstvy hlavni casti obrazovky.<br>
	 * <strong>Pred zavolanim teto funkce doporucuji zavolat funkci WaitForNextFrame().</strong> Pokud tak
	 * neucinite a pouzivate tuto funkce v loope, je mozne pozdni a neuplne vykresleni na obrazovku.<br>
	 * Projevi se to "blikanim" hlavni casti obrazovky. Spozdeni vykreslovani je dane konstantou
	 * maxDrawingTime.
	 */
	private static void startDrawing()
	{
		readyForRefresh = false;
		refreshScreen();
	}

	/** Zrusi pozastaveni refreshovni obrazovky. */
	private static void stopDrawing()
	{
		readyForRefresh = true;
	}
}
