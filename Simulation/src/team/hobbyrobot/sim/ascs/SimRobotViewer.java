package team.hobbyrobot.sim.ascs;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import team.hobbyrobot.graphics.PaintPanel;
import team.hobbyrobot.graphics.Paintable;

public class SimRobotViewer extends PaintPanel
{
	private Object _robotsLock = new Object();
	private List<SimRobot> _robots = null;
	private List<StorageCell> _storageCells;

	private static final Font errFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
	private static final Font robotFont = new Font(Font.MONOSPACED, Font.PLAIN, 10);
	
	private double _scale = 1;
	private double _realWidth;
	
	public SimRobotViewer(List<SimRobot> robots, List<StorageCell> storageCells, double realWidth)
	{
		super();
		_realWidth = realWidth;
		_robots = robots;
		_storageCells = storageCells;
		addLayer(new Graphics());
	}
	
	private class Graphics implements Paintable
	{

		@Override
		public void paint(Graphics2D g)
		{
		    _scale = SimRobotViewer.this.getWidth() / _realWidth;
			if(_robots == null || _robots.size() <= 0)
			{
				drawCenteredString(g, "NO ROBOTS FOUND", new Rectangle(0, 0, SimRobotViewer.this.getWidth(), SimRobotViewer.this.getHeight()), errFont, Color.red);
				return;
			}
			
			for(SimRobot robot : _robots)
			{
				double x = (double) robot.pose.x;
				double y = (double)  robot.pose.y;
				double heading = (double) robot.pose.heading;
				long id = (long) robot.id;
				
				double heading_rad = -heading/180 * Math.PI;
				
				x *= _scale;
				y *= _scale;
				y = (double)SimRobotViewer.this.getHeight() - y;
				
				g.setColor(Color.black);
				g.fillOval((int)(x - 5), (int)(y - 5), 10, 10);
				g.drawLine((int)x, (int)y, (int)(x + 20 * Math.cos(heading_rad)), (int)(y + 20 * Math.sin(heading_rad)));
				
				FontMetrics metrics = g.getFontMetrics(robotFont);
				g.setColor(Color.red);
				g.setFont(robotFont);
				g.drawString(String.valueOf(id), 
					(int)(x - 	metrics.stringWidth(String.valueOf(id)) / 2 	- 10 * Math.cos(heading_rad)), 
					(int)(y + 	metrics.getAscent() / 2 						- 10 * Math.sin(heading_rad))
					);
			}
			
			for(StorageCell cell : _storageCells)
			{
				double x = (double) cell.pose.x;
				double y = (double)  cell.pose.y;
				double heading = (double) cell.pose.heading;
				long id = (long) cell.id;
				
				double heading_rad = -heading/180 * Math.PI;
				
				x *= _scale;
				y *= _scale;
				y = (double)SimRobotViewer.this.getHeight() - y;
				
				g.setColor(Color.magenta);
				g.fillRect((int)(x - 5), (int)(y - 5), 10, 10);
				g.drawLine((int)x, (int)y, (int)(x + 11 * Math.cos(heading_rad)), (int)(y + 11 * Math.sin(heading_rad)));
				
				FontMetrics metrics = g.getFontMetrics(robotFont);
				g.setColor(Color.blue);
				g.setFont(robotFont);
				g.drawString(String.valueOf(id), 
					(int)(x - 	metrics.stringWidth(String.valueOf(id)) / 2 	- 10 * Math.cos(heading_rad)), 
					(int)(y + 	metrics.getAscent() / 2 						- 10 * Math.sin(heading_rad))
					);
			}
		}
		
		private void drawCenteredString(java.awt.Graphics g, String text, Rectangle rect, Font font, Color color) {
		    // Get the FontMetrics
			g.setColor(color);
		    FontMetrics metrics = g.getFontMetrics(font);
		    // Determine the X coordinate for the text
		    int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
		    // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
		    int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
		    // Set the font
		    g.setFont(font);
		    // Draw the String
		    g.drawString(text, x, y);
		}
		
	}
}
