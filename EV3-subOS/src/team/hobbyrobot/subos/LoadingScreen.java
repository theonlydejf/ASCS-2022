package team.hobbyrobot.subos;

import java.lang.reflect.Method;
import java.util.ArrayList;

import lejos.hardware.Sound;
import lejos.hardware.lcd.*;
import lejos.utility.Stopwatch;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.subos.errorhandling.ErrorLogging;
import team.hobbyrobot.subos.graphics.Drawable;
import team.hobbyrobot.subos.graphics.GraphicsController;
import team.hobbyrobot.subos.graphics.sysgraphics.LoadingScreenGraphics;
import team.hobbyrobot.subos.hardware.BrickHardware;
import team.hobbyrobot.subos.hardware.LEDBlinkingStyle;

/**
 * Trida vytvorena pro feedback pri akcich, ktere trvji dlouho<br>
 * <strong>Pred pouzitim dukladne prostudujte dokumentaci!</strong><br>
 * <p>
 * Vsechny funkce, na ktere se bude odkazovat, musi mit 2 parametry typu Object. Prvni parametr je pote
 * potreba precastovat do typu {@code Refereancable<Float>}, do ktereho budete ukladat z kolika % je akce
 * hotova (0 - 0%, 1 - 100%) a druhy do typu {@code ArrayList<String>} do ktereho budete zapisovat informace 
 * o prubehu funkce<br>
 * Deklarace funkce tedy bude vypadat takto:<br>
 * {@code public static void <jmeno>(Objcet <jmeno>, Object <jmeno>)}<br>
 * <br>
 * Prekastovani uvnitr funkce vypada takto:<br>
 * {@code Referenceable<Float> <jmeno> = (Referenceable<Float>)<jmeno 1. parametru>;}<br>
 * {@code ArrayList<String> <jmeno> = (ArrayList<String>)<jmeno 2. parametru>;}<br>
 * 
 * @author David Krcmar
 * @version 1.0
 */
public class LoadingScreen
{
	/** KONSTANTA - Pocet pokusu pro kazdou akci */
	public static final int RETRY_COUNT = 3;
	/** Doba mezi aktualizaci dat na obrazovce */
	public static final int UPDATE_GRAPHICS_PERIOD = 200; //ms

	/** Grafika pro LoadingScreen */
	public static Drawable loadingScreenGraphics;

	/**
	 * Cesty k funkcim, ktere se budou dit ve formatu:
	 * {@code <0-based index threadu>:<package>.<trida>:<funkce>[:nonFatal](<povinne>;[nepovinne])}
	 */
	public final String[] actions;
	/** Pocet threadu, ktere se budou pouzivat */
	public int threadCount;

	/**
	 * Array dlouhy stejne jako pocet threadu; Kazdy jeden index je rezervovany pro kazdy jeden Thread; Thread
	 * do nej zapisuje z kolika procent (hodnoty float 0 - 1) je bezici akce hotova (Posila se do kazde akce
	 * - Proto v Referenceable)
	 */
	public Referenceable<Float>[] threadFinishedPercentage; //Hodnoty: 0 - 1
	/** Pocet akci, ktere jsou zcela hotove */
	public int finishedActionsCount = 0;

	/**
	 * List do ktereho se ukladaji vsechny zpravy ktere akce vyvolali; Posila se do kazde akce -> Nemusi byt v
	 * Refereancable, protoze ArrayList odkazuje sam o sobe
	 */
	public ArrayList<String> messageFeed = new ArrayList<String>();

	/** Grafika na kterou se bude kreslit */
	public GraphicsLCD graphics;

	private String title;
	
	/**
	 * Vytvori instanci LoadingScreenu. LoadingScreen je pote potreba spustit funkci {@link #start()}
	 * 
	 * @param LoadingActions Array ve kterem jsou popisy akci ktere jsou potreba udelat.<br>
	 *                       Popisy jsou v tomto formatu:
	 *                       {@code <0-based index threadu>:<package>.<trida>:<funkce>[:nonFatal](<povinne>;[nepovinne])}
	 */
	public LoadingScreen(String title, String[] LoadingActions)
	{
		this.title = title;
		this.actions = LoadingActions;

		LoadingScreenGraphics ldg = new LoadingScreenGraphics();
		ldg.loadingScreen = this;
		loadingScreenGraphics = ldg;
		
		//
		//Zjisti pocet threadu
		//Najdi nejvetsi index
		int highestThreadID = 0;
		for (String ii : LoadingActions)
		{
			//Ziskej ID threadu z popisu akce
			int tID = Integer.parseInt(ii.split(":")[0]);

			if (tID > highestThreadID)
				highestThreadID = tID;

		}
		//K nejvyssimu indexu pricti 1 -> Ziska se pocet Threadu
		threadCount = highestThreadID + 1;

		//Vytvor array ktery bude dlouhy jako pocet threadu a uloz ho do te promenne
		threadFinishedPercentage = (Referenceable<Float>[]) new Referenceable[threadCount];
		//Nastav ho, aby hodnoty nebyli null
		for (int i = 0; i < threadCount; i++)
		{
			threadFinishedPercentage[i] = new Referenceable<Float>(0f);
		}
	}

	private Thread runThread(final int ID)
	{
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				//Projdi vsechny Akce
				for (String action : actions)
				{
					//Rozdel popis akce na klicova slova
					String[] splitActions = action.split(":");

					//Pokud akce nema byt spustena v tomto threadu
					int tID = Integer.parseInt(splitActions[0]);
					if (tID != ID)
						continue;

					//Ziskej cestu k tride ve ktere metoda je
					String classPath = splitActions[1];
					//Ziskej jmeno metody
					String funcName = splitActions[2];

					//Flag ktery rika jestli metoda uspesne probrhla
					boolean finished = false;

					//Nekolikrat zkus akci provest
					for (int i = 0; i < RETRY_COUNT; i++)
					{
						try
						{
							//Ziskej odkaz na tridu
							Class<?> c = Class.forName(classPath);
							//Ziskej odkaz na metodu
							Method method = c.getMethod(funcName, Object.class, Object.class);
							//Vyvolej metodu
							method.invoke(c, (Object) threadFinishedPercentage[ID], (Object) messageFeed);

							//Rekni v msg feedu ze se akce dokoncila
							messageFeed.add(funcName + ": DONE!");

							//Rekni ze se akce provedla spravne
							finished = true;
							break;
						}
						catch (Exception ex)
						{
							//
							//Pokud se neco nepovede
							//Rekni to v msg feedu
							messageFeed.add(funcName + ": ERROR! att " + i + "/" + RETRY_COUNT);
							//Blikni
							BrickHardware.blinkLED(2, 300, true, 1);
							//Loguj chybu
							ErrorLogging.logError("Error when loading; Thread ID: " + ID + "; Attempt number:" + i
								+ "; Class path: \"" + classPath + "\"; Method Name: \"" + funcName + "\"; "
								+ Logger.getExceptionInfo(ex));
						}
						//Resetuj procentualni stav aktualni akce
						threadFinishedPercentage[ID].setValue(0f);
					}

					//Pokd akce nebyla dokoncena
					if (!finished)
					{
						String msg = "Exceeded number of attempts saved in LoadingScreen.retryCount when doing action "
							+ action + "!!";

						//Pokud akce neni potreba pro beh programu -> logni to a upozorni na to
						if (splitActions.length > 3 && splitActions[3].equalsIgnoreCase(("nonfatal")))
						{
							BrickHardware.setLEDPattern(3, LEDBlinkingStyle.DOUBLEBLINK, 2);
							SystemSound.playNonFatalErrorSound(true);
							ErrorLogging.logError(msg);
						}
						//Pokud je potreba -> Fatal error
						else
						{
							//BrickHardware.SetLEDColor(2, BlinkingStyle.DOUBLEBLINK, true,
							//OnThreadCollision.OVERRIDE);
							ErrorLogging.logFatalError(msg);
							ErrorLogging.startErrorLogScreen();
						}
					}

					//Rekni ze je dokoncena dalsi ake
					finishedActionsCount++;
					//Resetuj procentualni stav aktualni akce
					threadFinishedPercentage[ID].setValue(0f);
				}
			}
		});
		t.start();

		return t;
	}

	/** Spusti Loading screen */
	public void start()
	{
		SubOSController.setViewName(getTitle());
		
		graphics = GraphicsController.getNewDefaultMainGraphics();
		
		//Nastav LED pattern
		BrickHardware.setLEDPattern(1, LEDBlinkingStyle.DOUBLEBLINK, 0);
		
		Stopwatch gUpdateSw = new Stopwatch();
		//Array vsech threadu ktere budou pouzity
		Thread[] threads = new Thread[threadCount];

		//Inicializuj thready
		for (int i = 0; i < threadCount; i++)
		{
			threads[i] = runThread(i);
		}

		gUpdateSw.reset();
		checkForFinishedThreads: while (true)
		{
			//Pokud uplynul dany cas od posledniho vykresleni na display -> vykresli
			if (gUpdateSw.elapsed() >= UPDATE_GRAPHICS_PERIOD)
			{
				updateGraphics();
				gUpdateSw.reset();
			}

			//Pokud jeste aspon jeden thread neco dela -> preskoc ten break na konci a pokracuj v loope
			for (Thread t : threads)
			{
				if (t.isAlive())
					continue checkForFinishedThreads;
			}

			break;
		}
		Sound.twoBeeps();
		updateGraphics();
		BrickHardware.releasePriority(0, 0);
	}

	/** Updatuje grafiku */
	private void updateGraphics()
	{
		loadingScreenGraphics.draw(graphics);
	}

	public void setTitle(String title)
	{
		this.title = title;
	}
	
	public String getTitle()
	{
		return title;
	}
}
