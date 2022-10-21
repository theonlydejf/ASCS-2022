package team.hobbyrobot.subos.menu;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.GraphicsLCD;
import team.hobbyrobot.subos.SubOSController;
import team.hobbyrobot.subos.graphics.GraphicsController;
import team.hobbyrobot.subos.graphics.sysgraphics.BasicMenuGraphics;

public class MenuScreen
{
	protected String Name = "Main Menu";
	
	public MenuItem[] menuItems;
	
	protected MenuGraphics menuGraphics = new BasicMenuGraphics();
	
	public MenuScreen(MenuItem[] menuItems)
	{
		this.menuItems = menuItems.clone();
		menuGraphics.menuItems = this.menuItems;
	}
	
	public int select()
	{
		SubOSController.setViewName(Name);
		
		GraphicsLCD graphics = GraphicsController.getNewDefaultMainGraphics();
				
		int selectedIndex = 0;
		
		while(true)
		{
			menuGraphics.draw(graphics);
			Button.waitForAnyPress();
			int buttonData = Button.getButtons();
			switch(buttonData)
			{
				case (Button.ID_LEFT | Button.ID_RIGHT | Button.ID_ENTER):
					return -1;
				
				case Button.ID_LEFT:
				case Button.ID_UP:
					selectedIndex--;
					if(selectedIndex < 0)
					{
						selectedIndex = 0;
						// Sound.playTone(400, 150);
					}
					
					menuGraphics.selectedIndex = selectedIndex;
					break;
					
				case Button.ID_RIGHT:
				case Button.ID_DOWN:
					selectedIndex++;
					if(selectedIndex >= menuItems.length)
					{
						selectedIndex = menuItems.length - 1;
						// Sound.playTone(400, 150);
					}
						
					menuGraphics.selectedIndex = selectedIndex;
					break;
					
				case Button.ID_ENTER:
					SubOSController.setViewName(menuItems[selectedIndex].getName());
					menuItems[selectedIndex].open();
					return selectedIndex;
			}
		}
	}
}
