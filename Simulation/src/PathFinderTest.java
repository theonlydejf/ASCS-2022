import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JFrame;

import lejos.robotics.geometry.Line;
import lejos.robotics.mapping.LineMap;
import lejos.robotics.navigation.DestinationUnreachableException;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.*;

import team.hobbyrobot.graphics.PaintPanel;
import team.hobbyrobot.graphics.Paintable;
import team.hobbyrobot.sim.ascs.SimRobot;
import team.hobbyrobot.sim.ascs.SimRobotViewer;

public class PathFinderTest extends JFrame
{
	private PaintPanel _canvas = new PaintPanel();

	private Dimension _size = new Dimension(750, 400);

	private LineMap _map = new LineMap(new Line[] { new Line(100, 200, 650, 200) },
		new lejos.robotics.geometry.Rectangle(0, 0, _size.width, _size.height));

	private DijkstraPathFinder _pathFinder = new DijkstraPathFinder(_map);

	private Pose _start = new Pose(750 / 2, 100, -90);
	private Pose _end = new Pose(750 / 2, 300, -90);

	public static void main(String[] args)
	{
		PathFinderTest w = new PathFinderTest();
	}

	public PathFinderTest()
	{
		setLayout(new BorderLayout());
		setTitle("Path finder");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);

		int robotViewerWidth = 500;

		_canvas.setPreferredSize(_size);
		_canvas.addLayer(new Graphics());

		getContentPane().add(_canvas);

		pack();

		setLocationRelativeTo(null);

		setVisible(true);

	}

	private class Graphics implements Paintable
	{

		@Override
		public void paint(Graphics2D g)
		{
			g.setColor(Color.red);
			for (Line l : _map.getLines())
			{
				g.drawLine((int) l.x1, (int) l.y1, (int) l.x2, (int) l.y2);
			}

			drawRobot(g, _start.getX(), _start.getY(), _start.getHeading(), 0, 1);

			g.setColor(Color.green);
			drawCross(g, (int) _end.getX(), (int) _end.getY());

			Path path = new Path();
			try
			{
				path = _pathFinder.findRoute(_start, new Waypoint(_end));
			}
			catch (DestinationUnreachableException e)
			{
				System.out.println("Destination unreachable!");
			}
			
			g.setColor(Color.black);
			
			Waypoint last = new Waypoint(_start);
			for(Waypoint w : path)
			{
				g.drawLine((int)w.x, (int)w.y, (int)last.x, (int)last.y);
				last = w;
			}
		}

		private final Font robotFont = new Font(Font.MONOSPACED, Font.PLAIN, 10);

		private void drawRobot(Graphics2D g, double x, double y, double heading, int id, double scale)
		{
			double heading_rad = -heading / 180 * Math.PI;

			x *= scale;
			y *= scale;
			y = (double) y;

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

		private void drawCross(Graphics2D g, int x, int y)
		{
			g.drawLine(x - 5, y - 5, x + 5, y + 5);
			g.drawLine(x - 5, y + 5, x + 5, y - 5);
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

	}
}
