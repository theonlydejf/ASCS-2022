package team.hobbyrobot.subos.menu;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.GraphicsLCD;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.subos.errorhandling.ErrorLogging;
import team.hobbyrobot.subos.graphics.GraphicsController;
import team.hobbyrobot.subos.graphics.sysgraphics.RobotInfoScreenGraphics;
import team.hobbyrobot.subos.hardware.RobotHardware;

public class RobotInfoScreen implements MenuItem
{
	//TODO: zobecnit na DataDispayerScreen
	
	public static final RobotInfoScreenGraphics screenGraphics = new RobotInfoScreenGraphics();
	
	private List<Class<?>> robotHardwareClasses = new ArrayList<Class<?>>();
	
	public RobotInfoScreen(Class<? extends RobotHardware> robotHardwareClass)
	{
		robotHardwareClasses.add(robotHardwareClass);
		Class<?>[] constantClasses = robotHardwareClass.getClasses();
		for(Class<?> clazz : constantClasses)
		{
			if(clazz.isAnnotationPresent(IncludeInRobotInfo.class))
			{
				robotHardwareClasses.add(clazz);
			}
		}
	}

	@Override
	public String getName()
	{
		return "Robot Info";
	}

	@Override
	public void open()
	{
		GraphicsLCD graphics = GraphicsController.getNewDefaultMainGraphics();
		graphics.setFont(Font.getSmallFont());
		graphics.drawString("Loading data...", graphics.getWidth() / 2, graphics.getHeight() / 2, GraphicsLCD.HCENTER | GraphicsLCD.VCENTER);
		GraphicsController.refreshScreen();
		
		Object instance = robotHardwareClasses.get(0).cast(RobotHardware.RobotHardwareToInitialize);
		List<NameValuePair> values = new ArrayList<NameValuePair>();
		
		for(Class<?> clazz : robotHardwareClasses)
		{
			for (Class<?> c = clazz; c != null; c = c.getSuperclass())
			{
				for (Field f : c.getDeclaredFields())
				{
					if (f.isAnnotationPresent(IncludeInRobotInfo.class) && (instance != null || Modifier.isStatic(f.getModifiers())))
					{
						try
						{
							values.add(new NameValuePair(f.getName(), f.get(instance)));
						}
						catch (IllegalArgumentException | IllegalAccessException e)
						{
							Sound.buzz();
							ErrorLogging.logError(Logger.getExceptionInfo(e));
							return;
						} 
					}
				}
			}
			
			if(instance != null)
				instance = null;
		}
		
		screenGraphics.robotData = values;

		while(true)
		{
			screenGraphics.draw(graphics);
			int buttonData = Button.getButtons();
			switch(buttonData)
			{
				case Button.ID_ESCAPE:
				case (Button.ID_LEFT | Button.ID_RIGHT | Button.ID_ENTER):
					return;
				
				case Button.ID_UP:
					screenGraphics.scroll--;
					if(screenGraphics.scroll < 0)
						screenGraphics.scroll = values.size() - 1;
					Sound.playTone(Button.getKeyClickTone(Button.ID_UP), Button.getKeyClickLength(), Button.getKeyClickVolume());
					while((Button.getButtons() & Button.ID_UP) != 0) ;
					break;
					
				case Button.ID_DOWN:
					screenGraphics.scroll++;
					if(screenGraphics.scroll >= values.size())
						screenGraphics.scroll = 0;
					Sound.playTone(Button.getKeyClickTone(Button.ID_DOWN), Button.getKeyClickLength(), Button.getKeyClickVolume());
					while((Button.getButtons() & Button.ID_DOWN) != 0) ;
					break;
			}
		}
	}
	
	public class NameValuePair
	{
		public String Name;
		public Object Value;
		
		public NameValuePair(String name, Object value)
		{
			Name = name;
			Value = value;
		}
	}
}
