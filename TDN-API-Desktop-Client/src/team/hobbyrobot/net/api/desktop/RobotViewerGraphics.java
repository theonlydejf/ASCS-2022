package team.hobbyrobot.net.api.desktop;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import lejos.robotics.geometry.Line;
import lejos.robotics.geometry.Point2D;
import team.hobbyrobot.collisiondetection.PathPerformer;
import team.hobbyrobot.graphics.PaintPanel;
import team.hobbyrobot.graphics.Paintable;
import team.hobbyrobot.python.BridgeListener;
import team.hobbyrobot.robotmodeling.RemoteASCSRobot;
import team.hobbyrobot.robotobserver.RobotObserver;
import team.hobbyrobot.robotobserver.RobotObserverListener;

public class RobotViewerGraphics implements Paintable, RobotObserverListener
{
	private static final Color transparentGray = new Color(Color.gray.getRed(), Color.gray.getGreen(), Color.gray.getBlue(), 170);
	private static final Color transparentLightGray = new Color(Color.lightGray.getRed(), Color.lightGray.getGreen(), Color.lightGray.getBlue(), 128);

	private Object _robotsLock = new Object();
	private JSONArray _robots = null;

	private static final Font errFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
	private static final Font robotFont = new Font(Font.MONOSPACED, Font.PLAIN, 10);

	private double _scale = 1;
	private double _realWidth;
	private PaintPanel _paintPanel;
	
	private StorageNavigator _storageNavigator;

	public RobotViewerGraphics(StorageNavigator storageNavigator, RobotObserver observer, PaintPanel parent, double realWidth)
	{
		super();
		_realWidth = realWidth;
		observer.addListener(this);
		_paintPanel = parent;
		_storageNavigator = storageNavigator;
	}

	@Override
	public void paint(Graphics2D g)
	{

		_scale = _paintPanel.getWidth() / _realWidth;
		if (_robots == null || _robots.size() <= 0)
		{
			drawCenteredString(g, "NO ROBOTS FOUND",
				new Rectangle(0, 0, _paintPanel.getWidth(), _paintPanel.getHeight()), errFont, Color.red);
			return;
		}
		Map<Integer, Integer> storageCellTagIdMap = _storageNavigator.getCellTagIDMap();
		synchronized (_robotsLock)
		{
			// Draw bounding boxes
			for (Object obj : _robots)
			{
				JSONObject robot = (JSONObject) obj;
				long id = (long) robot.get("id");

				if(storageCellTagIdMap.containsKey((int)id))
				{
					for(Line l : PathPerformer.collisionAvoider.getStaticObstructions())
					{
						g.drawLine((int)(l.x1 * _scale), (int)(l.y1 * _scale), (int)(l.x2 * _scale), (int)(l.y2 * _scale));
					}
					continue;
				}
				
				double x = (double) robot.get("x");
				double y = (double) robot.get("y");

				Point2D[] bb = RemoteASCSRobot.getRobotBoundingBox(0, x, y);
				int[] bb_xs = new int[bb.length];
				int[] bb_ys = new int[bb.length];
				for(int i = 0; i < bb.length; i++)
				{
					bb_xs[i] = (int)(bb[i].getX() * _scale);
					bb_ys[i] = (int)(bb[i].getY() * _scale);
				}
				
				g.setColor(transparentLightGray);
				g.fillPolygon(bb_xs, bb_ys, bb.length);
				
				g.setColor(Color.lightGray);
				for(double padding : new double[] { PathPerformer.SAFE_DISTANCE, 0 })
				{
					for(Line l : RemoteASCSRobot.getLinesFromPoints(RemoteASCSRobot.getRobotBoundingBox(padding, x, y)))
					{
						g.drawLine((int)(l.x1 * _scale), (int)(l.y1 * _scale), (int)(l.x2 * _scale), (int)(l.y2 * _scale));
					}
				}
			}
			
			for (Object obj : _robots)
			{
				JSONObject robot = (JSONObject) obj;
				double x = (double) robot.get("x") * _scale;
				double y = (double) robot.get("y") * _scale;
				double heading = (double) robot.get("heading");
				long id = (long) robot.get("id");

				double heading_rad = heading / 180 * Math.PI;
				
				if(storageCellTagIdMap.containsKey((int)id))
				{
					g.setColor(Color.blue);
					id = storageCellTagIdMap.get((int)id);
				}
				else
				{
					// Draw size of the robot
					g.setColor(Color.gray);
					double robotSize = RemoteASCSRobot.SIZE * _scale;
					g.drawLine((int)(x-robotSize), (int)(y-robotSize), (int)(x+robotSize), (int)(y-robotSize));
					g.drawLine((int)(x+robotSize), (int)(y-robotSize), (int)(x+robotSize), (int)(y+robotSize));
					g.drawLine((int)(x+robotSize), (int)(y+robotSize), (int)(x-robotSize), (int)(y+robotSize));
					g.drawLine((int)(x-robotSize), (int)(y+robotSize), (int)(x-robotSize), (int)(y-robotSize));
					
					g.setColor(transparentGray);
					g.fillOval((int)(x-robotSize), (int)(y-robotSize), (int)(robotSize*2), (int)(robotSize*2));
					g.setColor(Color.black);
				}
				
				// Draw pose of the robot
				g.fillOval((int) (x - 5), (int) (y - 5), 10, 10);
				g.drawLine((int) x, (int) y, (int) (x + 20 * Math.cos(heading_rad)),
					(int) (y + 20 * Math.sin(heading_rad)));
				
				// Draw ID of the robot
				FontMetrics metrics = g.getFontMetrics(robotFont);
				g.setColor(Color.red);
				g.setFont(robotFont);
				g.drawString(String.valueOf(id),
					(int) (x - metrics.stringWidth(String.valueOf(id)) / 2 - 10 * Math.cos(heading_rad)),
					(int) (y + metrics.getAscent() / 2 - 10 * Math.sin(heading_rad)));

			}
		}
	}

	private void drawCenteredString(java.awt.Graphics g, String text, Rectangle rect, Font font, Color color)
	{
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

	@Override
	public void robotsReceived(JSONArray robots)
	{
		synchronized (_robotsLock)
		{
			_robots = robots;
		}
		_paintPanel.repaint();
	}
}
