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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import lejos.robotics.navigation.DestinationUnreachableException;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import team.hobbyrobot.collisiondetection.CollisionAvoider;
import team.hobbyrobot.collisiondetection.LimmitedPath;
import team.hobbyrobot.collisiondetection.PathPerformer;
import team.hobbyrobot.collisiondetection.Vector;
import team.hobbyrobot.graphics.PaintPanel;
import team.hobbyrobot.graphics.Paintable;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.net.api.TDNAPIClient;
import team.hobbyrobot.net.api.streaming.TDNSender;
import team.hobbyrobot.net.api.remoteevents.RemoteEventListenerServer;
import team.hobbyrobot.python.Bridge;
import team.hobbyrobot.robotmodeling.*;
import team.hobbyrobot.robotobserver.RobotCorrector;
import team.hobbyrobot.robotobserver.RobotModel;
import team.hobbyrobot.robotobserver.RobotObserver;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class RobotManagerWindow extends JFrame implements RobotCommanderListener
{
	static final String settingsPath = "/Users/david/Documents/MAP/ascs-settings.json";

	public Logger _logger = new Logger();
	public LinkedList<RobotCommanderWindow> commanderWindows = new LinkedList<>();
	public LinkedList<RemoteASCSRobot> robots = new LinkedList<>();

	private boolean _recordingPath = false;
	private Path _recordedPath = new Path();

	private int planeWidth, planeHeight;

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

		JButton connectDefaultBtn = new JButton("Connect default vehicles");
		connectDefaultBtn.setAlignmentX(CENTER_ALIGNMENT);
		connectDefaultBtn.addActionListener(e ->
		{
			JSONArray arr = (JSONArray) settings.get("vehicles");
			for (Object o : arr)
			{
				JSONObject json = (JSONObject) o;
				int id = (int) (long) json.get("id");
				String ip = (String) json.get("ip");
				int loggerPort = (int) (long) json.get("logger-port");
				int apiPort = (int) (long) json.get("api-port");
				int correctorPort = (int) (long) json.get("pose-corrector-port");
				connectRobot(id, ip, loggerPort, apiPort, correctorPort);
			}
		});
		robotConnectionPanel.add(connectDefaultBtn);

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
			int id = Integer.parseInt(idTxt.getText());
			String ip = ipTxt.getText();
			int loggerPort = Integer.parseInt(loggerPortTxt.getText());
			int apiPort = Integer.parseInt(apiPortTxt.getText());
			int correctorPort = Integer.parseInt(correctorPortTxt.getText());
			connectRobot(id, ip, loggerPort, apiPort, correctorPort);
			JOptionPane.showMessageDialog(commanderWindows.getLast(), "Connected to " + ip + ":" + apiPort, "Success",
				JOptionPane.INFORMATION_MESSAGE);
		});
		ipSelection.add(connectBtn);

		robotConnectionPanel.add(ipSelection);
		robotConnectionPanel.add(portSelection);
		robotConnectionPanel.setBorder(new CompoundBorder(new EmptyBorder(10, 10, 10, 10),
			BorderFactory.createTitledBorder("Connect to vehicle")));
		getContentPane().add(robotConnectionPanel);

		JSONObject calibSettings = (JSONObject) observerSettings.get("calib");
		JSONObject planeSettings = (JSONObject) calibSettings.get("rectangle");
		planeWidth = (int) (long) planeSettings.get("width");
		planeHeight = (int) (long) planeSettings.get("height");
		int robotViewerWidth = 500;

		PaintPanel robotViewer = new PaintPanel();
		robotViewer.setPreferredSize(
			new Dimension(robotViewerWidth, (int) ((robotViewerWidth / (float) planeWidth) * planeHeight)));
		Border robotViewerBorder = new CompoundBorder(new EmptyBorder(10, 10, 10, 10),
			BorderFactory.createTitledBorder("Robot viewer"));
		robotViewer.setBorder(robotViewerBorder);

		robotViewer.addLayer(new PathPerformer.PathGraphics(robotViewer, planeWidth));

		RemoteASCSRobot.RobotMovementGraphics movementGraphcis = new RemoteASCSRobot.RobotMovementGraphics(robotViewer,
			planeWidth);
		robotViewer.addLayer(movementGraphcis);

		RobotViewerGraphics robotGraphics = new RobotViewerGraphics(observer, robotViewer, planeWidth);
		robotViewer.addLayer(robotGraphics);

		robotViewer.addLayer(new PathGeneratorGraphics(robotViewer, planeWidth));

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
		testBtn.addActionListener(e ->
		{
			Thread t = new Thread(() ->
			{
				try
				{
					performTest();
				}
				catch (IOException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				catch (InterruptedException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
		});
		getContentPane().add(testBtn);
		pack();

		setLocationRelativeTo(null);

		setVisible(true);
	}

	private void connectRobot(int id, String ip, int loggerPort, int apiPort, int correctorPort)
	{
		try
		{
			RemoteASCSRobot robot = new RemoteASCSRobot(id, ip, loggerPort, apiPort, correctorPort, _logger);

			commanderWindows.add(new RobotCommanderWindow(robot));
			robots.add(robot);
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

	public void performTest() throws IOException, InterruptedException
	{
		CollisionAvoider avoider = new CollisionAvoider(new Dimension(planeWidth, planeHeight));

		RemoteASCSRobot r6 = RemoteASCSRobot.getRobot(6);
		RemoteASCSRobot r5 = RemoteASCSRobot.getRobot(5);

		//r5.goTo(new Waypoint(1080, 570, 45));
		//r6.goTo(new Waypoint(1280, 570, 90));

		//while(r5.isMoving()) System.out.println(r5.isMoving());
		
		r5.api.rawRequest(RemoteASCSRobot.Requests.ROTATE.toTDN(new TDNValue(-90f, TDNParsers.FLOAT)));
		//while(r5.isMoving() || r6.isMoving()) System.out.println(r5.isMoving()+" "+r6.isMoving());
		Thread.sleep(1000);
		//r5.api.rawRequest(RemoteASCSRobot.Requests.TRAVEL.toTDN(new TDNValue(100f, TDNParsers.FLOAT)));

		//r6.api.rawRequest(RemoteASCSRobot.Requests.ROTATE.toTDN(new TDNValue(180f, TDNParsers.FLOAT)));
		
		Waypoint w5 = new Waypoint(1080 + 800, 570);
		Waypoint w6 = new Waypoint(1280 + 801, 570);

		RobotModel s5 = RemoteASCSRobot.globalCorrector.getRobotModel(5);
		RobotModel s6 = RemoteASCSRobot.globalCorrector.getRobotModel(6);
		
		Vector A = new Vector(s6.x, s6.y);
		Vector B = new Vector(w6.x, w6.y);
		
		Vector C = new Vector(s5.x, s5.y);
		Vector D = new Vector(w5.x, w5.y);
		
		Vector E = C.plus(D).scale(.5);
		
		try
		{
			Path path = avoider.getPath(null, A, A, C, D, 0, 0f, RemoteASCSRobot.SIZE, 0);

			//r6.goTo(w6);
			r5.followPath(path.toArray(new Waypoint[path.size()]));
			for(Waypoint w : path)
			{
				System.out.print("x=" + w.x + "; y=" + w.y + "; h=" + (w.isHeadingRequired() ? w.getHeading() : "-"));
			}
			System.out.println(path.size()+"===========================================");
		}
		catch (DestinationUnreachableException e)
		{
			System.err.println("Destination unreachable");
		}

	}

	@Override
	public void moveEventReceived(String name, TDNRoot params, Socket client, int robotID)
	{
		System.out.println("Robot " + robotID + " event: " + name + ": " + params.toString());
	}
}
