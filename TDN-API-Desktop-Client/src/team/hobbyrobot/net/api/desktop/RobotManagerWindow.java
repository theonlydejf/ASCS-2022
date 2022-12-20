package team.hobbyrobot.net.api.desktop;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import team.hobbyrobot.collisiondetection.LimmitedPath;
import team.hobbyrobot.collisiondetection.PathPerformer;
import team.hobbyrobot.graphics.PaintPanel;
import team.hobbyrobot.graphics.Paintable;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.net.api.TDNAPIClient;
import team.hobbyrobot.net.api.streaming.TDNSender;
import team.hobbyrobot.net.api.remoteevents.RemoteEventListenerServer;
import team.hobbyrobot.python.Bridge;
import team.hobbyrobot.robotmodeling.*;
import team.hobbyrobot.robotobserver.RobotCorrector;
import team.hobbyrobot.robotobserver.RobotObserver;
import team.hobbyrobot.tdn.core.TDNRoot;

public class RobotManagerWindow extends JFrame implements RobotCommanderListener
{
	static final String settingsPath = "/Users/david/Documents/MAP/ascs-settings.json";

	public Logger _logger = new Logger();
	public LinkedList<RobotCommanderWindow> commanderWindows = new LinkedList<>();
	public LinkedList<RemoteASCSRobot> robots = new LinkedList<>();

	private boolean _recordingPath = false;
	private Path _recordedPath = new Path();

	public static void main(String[] args) throws IOException, ParseException
	{
		RobotManagerWindow w = new RobotManagerWindow();
		w._logger.registerEndpoint(new PrintWriter(System.out));
	}

	private class PathGeneratorGraphics implements Paintable
	{
		private int realWidth;
		PaintPanel panel;

		public PathGeneratorGraphics(PaintPanel panel, int realWidth)
		{
			this.panel = panel;
			this.realWidth = realWidth;
		}

		@Override
		public void paint(Graphics2D g)
		{
			float scale = panel.getWidth() / (float) realWidth;
			for (Waypoint w : _recordedPath)
			{
				g.setColor(Color.red);
				int x = (int) (w.x * scale);
				int y = (int) (w.y * scale);

				g.fillOval(x - 5, y - 5, 10, 10);
			}
		}

	}

	public RobotManagerWindow() throws IOException, ParseException
	{
		setTitle("ASCS Vehicle manager");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(true);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		JSONParser jsonParser = new JSONParser();
		FileReader reader = new FileReader(settingsPath);
		JSONObject settings = (JSONObject) jsonParser.parse(reader);

		JSONObject observerSettings = (JSONObject) settings.get("robot-observer");

		// Connect to camera server through bridge
		JSONObject cameraServerStg = (JSONObject) observerSettings.get("camera-server");
		Bridge bridge = new Bridge((String) cameraServerStg.get("host"), (int) (long) cameraServerStg.get("port"));
		bridge.start();

		RobotObserver observer = new RobotObserver(bridge);

		long eventServerPort = (long) settings.get("event-server-port");
		RemoteASCSRobot.initEventListenerServer((int) eventServerPort, _logger);
		RemoteASCSRobot.initGlobalRobotCorrector(observer);

		JPanel robotConnectionPanel = new JPanel();
		robotConnectionPanel.setLayout(new BoxLayout(robotConnectionPanel, BoxLayout.PAGE_AXIS));

		JPanel ipSelection = new JPanel();
		ipSelection.setLayout(new FlowLayout());
		JLabel ipLbl = new JLabel("IP:");
		ipSelection.add(ipLbl);
		JTextField ipTxt = new JTextField("localhost");
		ipTxt.setColumns(8);
		ipSelection.add(ipTxt);
		ipSelection.setLayout(new FlowLayout());
		JLabel idLbl = new JLabel("ID:");
		ipSelection.add(idLbl);
		JTextField idTxt = new JTextField("5");
		idTxt.setColumns(1);
		ipSelection.add(idTxt);

		JPanel portSelection = new JPanel();
		JLabel loggerPortLbl = new JLabel("Logger Port:");
		portSelection.add(loggerPortLbl);
		JTextField loggerPortTxt = new JTextField("2111");
		loggerPortTxt.setColumns(3);
		portSelection.add(loggerPortTxt);
		JLabel apiPortLbl = new JLabel("API Port:");
		portSelection.add(apiPortLbl);
		JTextField apiPortTxt = new JTextField("2222");
		apiPortTxt.setColumns(3);
		portSelection.add(apiPortTxt);
		JLabel correctorPortLbl = new JLabel("Corrector port:");
		portSelection.add(correctorPortLbl);
		JTextField correctorPortTxt = new JTextField("2333");
		correctorPortTxt.setColumns(3);
		portSelection.add(correctorPortTxt);

		JButton connectBtn = new JButton("Connect");
		connectBtn.addActionListener(e ->
		{
			try
			{
				int id = Integer.parseInt(idTxt.getText());
				String ip = ipTxt.getText();
				int loggerPort = Integer.parseInt(loggerPortTxt.getText());
				int apiPort = Integer.parseInt(apiPortTxt.getText());
				int correctorPort = Integer.parseInt(correctorPortTxt.getText());

				RemoteASCSRobot robot = new RemoteASCSRobot(id, ip, loggerPort, apiPort, correctorPort, _logger);

				commanderWindows.add(new RobotCommanderWindow(robot));
				robots.add(robot);

				JOptionPane.showMessageDialog(commanderWindows.getLast(),
					"Connected to " + ipTxt.getText() + ":" + apiPortTxt.getText(), "Success",
					JOptionPane.INFORMATION_MESSAGE);
			}
			catch (NumberFormatException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			catch (UnknownHostException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		ipSelection.add(connectBtn);

		robotConnectionPanel.add(ipSelection);
		robotConnectionPanel.add(portSelection);
		robotConnectionPanel.setBorder(new CompoundBorder(new EmptyBorder(10, 10, 10, 10),
			BorderFactory.createTitledBorder("Connect to vehicle")));
		getContentPane().add(robotConnectionPanel);

		JSONObject calibSettings = (JSONObject) observerSettings.get("calib");
		JSONObject planeSettings = (JSONObject) calibSettings.get("rectangle");
		long planeWidth = (long) planeSettings.get("width");
		long planeHeight = (long) planeSettings.get("height");
		int robotViewerWidth = 500;

		PaintPanel robotViewer = new PaintPanel();
		robotViewer.setPreferredSize(
			new Dimension(robotViewerWidth, (int) ((robotViewerWidth / (float) planeWidth) * planeHeight)));
		Border robotViewerBorder = new CompoundBorder(new EmptyBorder(10, 10, 10, 10),
			BorderFactory.createTitledBorder("Robot viewer"));
		robotViewer.setBorder(robotViewerBorder);

		robotViewer.addLayer(new PathPerformer.PathGraphics(robotViewer, (int) planeWidth));
		
		RemoteASCSRobot.RobotMovementGraphics movementGraphcis = new RemoteASCSRobot.RobotMovementGraphics(robotViewer, planeWidth);
		robotViewer.addLayer(movementGraphcis);

		RobotViewerGraphics robotGraphics = new RobotViewerGraphics(observer, robotViewer, (int) planeWidth);
		robotViewer.addLayer(robotGraphics);

		robotViewer.addLayer(new PathGeneratorGraphics(robotViewer, (int) planeWidth));

		getContentPane().add(robotViewer);

		robotViewer.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				double scale = planeWidth / (double) robotViewerWidth;

				double x = e.getX() * scale;
				double y = e.getY() * scale;
				Waypoint w = new Waypoint(x, y);
				_recordedPath.add(w);

				if (!_recordingPath)
				{
					sendPath();
					_recordedPath.clear();
				}
			}
		});

		JButton recordBtn = new JButton("Start recording path");
		recordBtn.setAlignmentX(CENTER_ALIGNMENT);
		recordBtn.addActionListener(e ->
		{
			if (!_recordingPath)
			{
				_recordingPath = true;
				recordBtn.setText("Stop recording & send");
				return;
			}

			_recordingPath = false;
			recordBtn.setText("Start recording path");
			sendPath();
			_recordedPath.clear();
		});
		getContentPane().add(recordBtn);
		
		JButton testBtn = new JButton("Start Test");
		testBtn.addActionListener(e -> performTest());
		getContentPane().add(testBtn);
		pack();

		setLocationRelativeTo(null);

		setVisible(true);
	}

	private void sendPath()
	{
		int id;
		try
		{
			id = Integer.parseInt(JOptionPane.showInputDialog("Enter ID of the vehicle"));
		}
		catch (NumberFormatException e1)
		{
			JOptionPane.showMessageDialog(null, "Robot with this ID is not connected!", "Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		RemoteASCSRobot vehicle = RemoteASCSRobot.getRobot(id);
		if (vehicle == null)
		{
			JOptionPane.showMessageDialog(null, "Robot with this ID is not connected!", "Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		try
		{
			new PathPerformer(_recordedPath, id, -1);
		}
		catch (IOException e1)
		{
			JOptionPane.showMessageDialog(null, "Error when sending the goTo request: " + e1.toString(), "Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void performTest()
	{
		Waypoint w5 = new Waypoint(1080, 100);
		Waypoint w6 = new Waypoint(1280, 100);
		Path p5 = new Path();
		p5.add(w5);
		Path p6 = new Path();
		p6.add(w6);
		try
		{
			new PathPerformer(p5, 5, -1);
			new PathPerformer(p6, 6, -1);
		}
		catch (IOException e2)
		{
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		
		if(true)
			return;
		try
		{
			RemoteASCSRobot.getRobot(6).goTo(new Waypoint(0, 0));
		}
		catch (IOException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		LimmitedPath p = new LimmitedPath((ArrayList<Waypoint>) _recordedPath);
		p.travelLimit = 500;
		p.limmitedStartWaypointIndex = 0;
		try
		{
			new PathPerformer(p, 5, 6);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		_recordedPath.clear();
		_recordingPath = false;
	}

	@Override
	public void moveEventReceived(String name, TDNRoot params, Socket client, int robotID)
	{
		System.out.println("Robot " + robotID + " event: " + name + ": " + params.toString());
	}
}
