package team.hobbyrobot.net.api.desktop;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
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

import lejos.robotics.geometry.Point2D;
import lejos.robotics.navigation.DestinationUnreachableException;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import team.hobbyrobot.collisiondetection.Calibrator;
import team.hobbyrobot.collisiondetection.CollisionAvoider;
import team.hobbyrobot.collisiondetection.LimitedPath;
import team.hobbyrobot.collisiondetection.PathPerformer;
import team.hobbyrobot.collisiondetection.Vector;
import team.hobbyrobot.graphics.DashedBorder;
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
import team.hobbyrobot.subos.Referenceable;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;
import team.hobbyrobot.utils.ProgressReporter;

public class RobotManagerWindow extends JFrame implements RobotCommanderListener
{
	static final String settingsPath = "/Users/david/Documents/MAP/ascs-settings.json";

	public Logger _logger = new Logger();
	public LinkedList<RobotCommanderWindow> commanderWindows = new LinkedList<>();
	public LinkedList<RemoteASCSRobot> robots = new LinkedList<>();
	private boolean _recordingPath = false;
	private Path _recordedPath = new Path();
	
	private StorageNavigator storageNavigator;

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
			Thread t = new Thread()
			{
				@Override
				public void run()
				{
					connectDefaultBtn.setEnabled(false);
					JSONArray arr = (JSONArray) settings.get("vehicles");
					StringBuilder exceptionDetails = new StringBuilder();
					for (Object o : arr)
					{
						int id = -1;
						try
						{
							JSONObject json = (JSONObject) o;
							id = (int) (long) json.get("id");
							String ip = (String) json.get("ip");
							int loggerPort = (int) (long) json.get("logger-port");
							int apiPort = (int) (long) json.get("api-port");
							int correctorPort = (int) (long) json.get("pose-corrector-port");
							ProgressReporter reporter = connectRobot(id, ip, loggerPort, apiPort, correctorPort);
							ProgressWindow progressWin = new ProgressWindow("Connecting robot " + id, reporter);
							progressWin.setAlwaysOnTop(true);
							progressWin.requestFocus();
							while(!reporter.isDone()) Thread.yield();
						}
						catch(Exception ex)
						{
							exceptionDetails.append("Exception on robot " + id + ": ");
							exceptionDetails.append(ex.getMessage());
							exceptionDetails.append("\n");
						}
					}
					
					if(exceptionDetails.length() > 0)
						JOptionPane.showMessageDialog(null, exceptionDetails.toString(), "Error",
							JOptionPane.ERROR_MESSAGE);
					connectDefaultBtn.setEnabled(true);
				}
			};
			t.start();
		});
		robotConnectionPanel.add(connectDefaultBtn);

		JPanel ipSelection = new JPanel();
		ipSelection.setLayout(new FlowLayout());
		JLabel ipLbl = new JLabel("IP:");
		ipSelection.add(ipLbl);
		JTextField ipTxt = new JTextField("192.168.1.101");
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
		JTextField loggerPortTxt = new JTextField("1111");
		loggerPortTxt.setColumns(3);
		portSelection.add(loggerPortTxt);
		JLabel apiPortLbl = new JLabel("API Port:");
		portSelection.add(apiPortLbl);
		JTextField apiPortTxt = new JTextField("2222");
		apiPortTxt.setColumns(3);
		portSelection.add(apiPortTxt);
		JLabel correctorPortLbl = new JLabel("Corrector port:");
		portSelection.add(correctorPortLbl);
		JTextField correctorPortTxt = new JTextField("3333");
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
				ProgressReporter reporter = connectRobot(id, ip, loggerPort, apiPort, correctorPort);
				ProgressWindow progressWin = new ProgressWindow("Connecting robot " + id, reporter);
				progressWin.setAlwaysOnTop(true);
				progressWin.requestFocus();
				Thread t = new Thread() 
				{
					@Override
					public void run()
					{
						connectBtn.setEnabled(false);
						while(!reporter.isDone()) 
							Thread.yield();
						connectBtn.setEnabled(true);
					}
				};
				t.start();
			}
			catch(NumberFormatException ex)
			{
				JOptionPane.showMessageDialog(null, ex.getMessage(), "Invalid format",
					JOptionPane.ERROR_MESSAGE);
			}
			catch (UnknownHostException ex)
			{
				JOptionPane.showMessageDialog(null, ex.getMessage(), "Unknown host",
					JOptionPane.ERROR_MESSAGE);
			}
			catch (IOException ex)
			{
				JOptionPane.showMessageDialog(null, ex.getMessage(), "Connection error",
					JOptionPane.ERROR_MESSAGE);
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
		planeWidth = (int) (long) planeSettings.get("width");
		planeHeight = (int) (long) planeSettings.get("height");
		int robotViewerWidth = 500;
		
		JSONArray storageCellIds = (JSONArray) settings.get("storage-cell-ids");
		storageNavigator = new StorageNavigator(storageCellIds);
		
		PathPerformer.initCollisionAvoider(planeWidth, planeHeight, storageNavigator.getCellTags());

		JPanel robotViewerParent = new JPanel();
		robotViewerParent.setLayout(new GridLayout(0, 1));
		Border robotViewerBorder = new CompoundBorder(new EmptyBorder(10, 10, 10, 10),
			BorderFactory.createTitledBorder("Robot viewer"));
		robotViewerParent.setBorder(robotViewerBorder);

		PaintPanel robotViewer = new PaintPanel();
		robotViewer.setPreferredSize(
			new Dimension(robotViewerWidth, (int) ((robotViewerWidth / (float) planeWidth) * planeHeight)));
		robotViewer.setBorder(new DashedBorder());
		RobotViewerGraphics robotGraphics = new RobotViewerGraphics(storageNavigator, observer, robotViewer, planeWidth);
		robotViewer.addLayer(robotGraphics);

		robotViewer.addLayer(new PathPerformer.PathGraphics(robotViewer, planeWidth));

		RemoteASCSRobot.RobotMovementGraphics movementGraphcis = new RemoteASCSRobot.RobotMovementGraphics(robotViewer,
			planeWidth);
		robotViewer.addLayer(movementGraphcis);

		robotViewer.addLayer(new PathGeneratorGraphics(robotViewer, planeWidth));

		robotViewerParent.add(robotViewer);
		getContentPane().add(robotViewerParent);

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

		JButton calibrateDeltaABtn = new JButton("Calibrate Delta A");
		calibrateDeltaABtn.setAlignmentX(CENTER_ALIGNMENT);
		calibrateDeltaABtn.addActionListener(e ->
		{
			Thread t = new Thread(() ->
			{
				double centerX = planeWidth / 2d;
				double centerY = planeHeight / 2d;
				double width = planeWidth / 2d;
				double height = planeHeight / 2d;
				RemoteASCSRobot[] robots = RemoteASCSRobot.getRobots();

				try
				{
					double deltaA = Calibrator.CalibrateDeltaA(
						new Point2D.Double(centerX - width / 2, centerY - height / 2),
						new Point2D.Double(centerX + width / 2, centerY + height / 2), robots[0], robots[1]);
					System.out.println(deltaA);
					PathPerformer.DELTA_A = deltaA;
				}
				catch (IOException ex)
				{
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Connection error",
						JOptionPane.ERROR_MESSAGE);
				}
				catch (RuntimeException ex)
				{
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Calibration failed",
						JOptionPane.ERROR_MESSAGE);
				}
			});
			t.start();
		});
		getContentPane().add(calibrateDeltaABtn);

		JPanel storageControlPanel = new JPanel();
		storageControlPanel.setBorder(new CompoundBorder(new EmptyBorder(10, 10, 10, 10),
			BorderFactory.createTitledBorder("Storage control")));
		Box box = new Box(BoxLayout.X_AXIS);
		box.add(Box.createHorizontalGlue());
		JTextField cellIDTxt = new JTextField();
		cellIDTxt.setColumns(3);
		JButton takeBtn = new JButton("Take from");
		takeBtn.addActionListener(e ->
		{
			Thread t = new Thread(() ->
			{
				try
				{
					int id = 0;
					try
					{
						id = Integer.parseInt(cellIDTxt.getText());
					}
					catch (NumberFormatException ex)
					{
						JOptionPane.showMessageDialog(null, cellIDTxt.getText() + " is not a number!", "Not a number",
							JOptionPane.ERROR_MESSAGE);
						return;
					}
					if(!storageNavigator.takeItemFrom(id))
					{
						JOptionPane.showMessageDialog(null, "Taking item from cell " + id + " failed!", "Action failed",
							JOptionPane.ERROR_MESSAGE);
					}
				}
				catch (IOException ex)
				{
					JOptionPane.showMessageDialog(null, ex.toString(), "Exception during storage control",
						JOptionPane.ERROR_MESSAGE);
				}
			});
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
		});
		box.add(takeBtn);
		JButton putBtn = new JButton("Put to");
		putBtn.addActionListener(e ->
		{
			Thread t = new Thread(() ->
			{
				try
				{
					int id = 0;
					try
					{
						id = Integer.parseInt(cellIDTxt.getText());
					}
					catch (NumberFormatException ex)
					{
						JOptionPane.showMessageDialog(null, cellIDTxt.getText() + " is not a number!", "Not a number",
							JOptionPane.ERROR_MESSAGE);
						return;
					}
					if(!storageNavigator.putItemTo(id))
					{
						JOptionPane.showMessageDialog(null, "Putting item to cell " + id + " failed!", "Action failed",
							JOptionPane.ERROR_MESSAGE);
					}
				}
				catch (IOException ex)
				{
					JOptionPane.showMessageDialog(null, ex.toString(), "Exception during storage control",
						JOptionPane.ERROR_MESSAGE);
				}
			});
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
		});
		box.add(putBtn);
		box.add(cellIDTxt);
		box.add(Box.createHorizontalGlue());
		
		storageControlPanel.add(box);
		getContentPane().add(storageControlPanel);
		
		JButton testBtn = new JButton("Start Test");
		testBtn.addActionListener(e ->
		{
			Thread t = new Thread(() ->
			{
				try
				{
					performTest();
				}
				catch (Exception ex)
				{
					JOptionPane.showMessageDialog(null, ex.toString(), "Exception during test",
						JOptionPane.ERROR_MESSAGE);
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

	private ProgressReporter connectRobot(int id, String ip, int loggerPort, int apiPort, int correctorPort)
		throws UnknownHostException, IOException
	{
		ProgressReporter reporter = new ProgressReporter();

		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				RemoteASCSRobot robot;
				try
				{
					robot = new RemoteASCSRobot(id, ip, loggerPort, apiPort, correctorPort, _logger, reporter);

					commanderWindows.add(new RobotCommanderWindow(robot));
					robots.add(robot);
				}
				catch (UnknownHostException e)
				{
					JOptionPane.showMessageDialog(null, "Unknown host error: " + e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				}
				catch (IOException e)
				{
					JOptionPane.showMessageDialog(null, "General exception when connecting to robot: " + e.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				}
				catch (RuntimeException e)
				{
					JOptionPane.showMessageDialog(null, "General exception when connecting to robot: " + e.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		};
		t.start();
		return reporter;
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
			new PathPerformer(_recordedPath, id);
		}
		catch (IOException e1)
		{
			JOptionPane.showMessageDialog(null, "Error when sending the goTo request: " + e1.toString(), "Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	Thread navigatorThread = null;
	public void performTest() throws IOException, InterruptedException
	{
		//RemoteASCSRobot.getRobot(5).api.rawRequest(RemoteASCSRobot.Requests.RESET_GYRO_AT.toTDN(new TDNValue(0f, TDNParsers.FLOAT)));
		//Path path = new Path();
		//path.add(new Waypoint(300, 300, 90));
		//new PathPerformer(path, 5);
		
		/*if(navigatorThread != null)
		{
			navigatorThread.interrupt();
			RemoteASCSRobot.getRobot(5).api.rawRequest(RemoteASCSRobot.Requests.STOP.toTDN());
			return;
		}
		
		Referenceable<Boolean> success = new Referenceable<Boolean>(null);
		navigatorThread = storageNavigator.goToStorageCellAsync(RemoteASCSRobot.getRobot(5), 0, success);
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				while(success.getValue() == null)
					Thread.yield();
				
				if(success.getValue())
					JOptionPane.showMessageDialog(null, "Success", "At cell storage", JOptionPane.INFORMATION_MESSAGE);
				else
					JOptionPane.showMessageDialog(null, "Failed", "Error", JOptionPane.ERROR_MESSAGE);
				
				navigatorThread = null;
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
		
		if(true) return;*/
		prepareTest();

		Path path5 = new Path();
		path5.add(new Waypoint(1490, 200));
		Path path6 = new Path();
		path6.add(new Waypoint(870, 200));
		//path6.add(new Waypoint(1180, 445));

		new PathPerformer(path6, 6);
		Thread.sleep(100);
		new PathPerformer(path5, 5);
	}

	public void prepareTest() throws IOException, InterruptedException
	{
		Path path5 = new Path();
		path5.add(new Waypoint(870, 870, 0));
		Path path6 = new Path();
		path6.add(new Waypoint(1490, 870, 180));
		PathPerformer performer5 = new PathPerformer(path5, 5);
		Thread.sleep(100);
		PathPerformer performer6 = new PathPerformer(path6, 6);
		while (!Thread.interrupted())
		{
			if (performer5.isFinished() && performer6.isFinished())
				break;
		}
	}

	@Override
	public void moveEventReceived(String name, TDNRoot params, Socket client, int robotID)
	{
		System.out.println("Robot " + robotID + " event: " + name + ": " + params.toString());
	}
}
