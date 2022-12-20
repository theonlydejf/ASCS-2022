package team.hobbyrobot.net.api.desktop;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import team.hobbyrobot.graphics.PaintPanel;
import team.hobbyrobot.graphics.Paintable;
import team.hobbyrobot.python.BridgeListener;
import team.hobbyrobot.robotobserver.RobotObserver;
import team.hobbyrobot.robotobserver.RobotObserverListener;

public class RobotViewerGraphics implements Paintable, RobotObserverListener
{
	private Object _robotsLock = new Object();
	private JSONArray _robots = null;

	private static final Font errFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
	private static final Font robotFont = new Font(Font.MONOSPACED, Font.PLAIN, 10);

	private double _scale = 1;
	private double _realWidth;
	private PaintPanel _paintPanel;

	public RobotViewerGraphics(RobotObserver observer, PaintPanel parent, double realWidth)
	{
		super();
		_realWidth = realWidth;
		observer.addListener(this);
		_paintPanel = parent;
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
		synchronized (_robotsLock)
		{
			for (Object obj : _robots)
			{
				JSONObject robot = (JSONObject) obj;
				double x = (double) robot.get("x");
				double y = (double) robot.get("y");
				double heading = (double) robot.get("heading");
				long id = (long) robot.get("id");

				double heading_rad = heading / 180 * Math.PI;

				x *= _scale;
				y *= _scale;
				g.setColor(Color.black);
				g.fillOval((int) (x - 5), (int) (y - 5), 10, 10);
				g.drawLine((int) x, (int) y, (int) (x + 20 * Math.cos(heading_rad)),
					(int) (y + 20 * Math.sin(heading_rad)));

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
