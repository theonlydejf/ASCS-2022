package team.hobbyrobot.net.api.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.FontUIResource;

import org.json.simple.parser.ParseException;

import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.net.api.TDNAPIClient;
import team.hobbyrobot.net.api.desktop.requests.Request;
import team.hobbyrobot.net.api.desktop.requests.RequestGenerator;
import team.hobbyrobot.net.api.remoteevents.RemoteEventListener;
import team.hobbyrobot.net.api.remoteevents.RemoteEventListenerServer;
import team.hobbyrobot.robotmodeling.RemoteASCSRobot;
import team.hobbyrobot.robotmodeling.RemoteASCSRobotListener;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;
import java.util.ArrayList;

public class RobotCommanderWindow extends JFrame implements RemoteASCSRobotListener
{
	private RemoteASCSRobot _robot;
	private ArrayList<TDNRoot> _eventsData = new ArrayList<TDNRoot>();
	private DefaultListModel<String> _eventNames = null;
	private JList<String> _eventList;
	private Thread _rmLogReaderThread;

	public static void main(String[] args) throws UnknownHostException, IOException
	{
		Logger l = new Logger();
		l.registerEndpoint(new PrintWriter(System.out));
		Runtime.getRuntime().addShutdownHook(new Thread(() ->
		{
			try
			{
				RemoteASCSRobot.eventServer.stopRegisteringClients();
			}
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try
			{
				RemoteASCSRobot.eventServer.closeRegisteredClients();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}));

		RemoteASCSRobot.initEventListenerServer(5555, l);
		System.out.println("Connecting to robot...");
		RobotCommanderWindow w = new RobotCommanderWindow(new RemoteASCSRobot(0, "192.168.1.100", 1111, 2222, 3333, l));
		w.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	public RobotCommanderWindow(RemoteASCSRobot robot)
	{
		_robot = robot;

		_robot.addRobotListener(this);

		setTitle("Robot " + robot.getID() + " Commander");
		setResizable(true);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent)
			{
				try
				{
					_rmLogReaderThread.interrupt();
					_robot.close();
				}
				catch (IOException e)
				{
					JOptionPane.showMessageDialog(null, "Failed to disconnect from robot " + robot.getID() + ": " + e.getMessage(), "Connection error",
						JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		JPanel top = new JPanel(new FlowLayout());
		JPanel createCmdPanel = new JPanel(new FlowLayout());
		createCmdPanel.setLayout(new BoxLayout(createCmdPanel, BoxLayout.Y_AXIS));
		createCmdPanel.setBorder(new CompoundBorder(
			new EmptyBorder(10, 10, 10, 10), 
			BorderFactory.createTitledBorder("Command manager")
			));
		
		JTextField cmdTxt = new JTextField();
		cmdTxt.setColumns(15);
		cmdTxt.setMaximumSize(cmdTxt.getPreferredSize());
		cmdTxt.setAlignmentX(Component.CENTER_ALIGNMENT);
		createCmdPanel.add(cmdTxt);

		JButton sendBtn = new JButton();
		sendBtn.setText("Create & send");
		sendBtn.addActionListener(w ->
		{
			Request rqst = RemoteASCSRobot.moveRequests.get(cmdTxt.getText());
			if(rqst == null)
				rqst = RemoteASCSRobot.osRequests.get(cmdTxt.getText());
			if(rqst == null)
				rqst = RemoteASCSRobot.vehicleRequests.get(cmdTxt.getText());
			
			if(rqst == null)
			{
				JOptionPane.showMessageDialog(null, "Unknown request", "Unknown request",
					JOptionPane.ERROR_MESSAGE);
				return;
			}

			Object[] params = new Object[rqst.params.length];
			for (int i = 0; i < params.length; i++)
			{
				try
				{
					String type = rqst.paramTypes[i];
					if(type.equals("any"))
						type = JOptionPane.showInputDialog("Entery type key of the value:");
					if(type == null || type.length() <= 0)
						return;
					
					String str = JOptionPane.showInputDialog("{" + rqst.paramTypes[i] + "} " + rqst.params[i]);
					if(str == null || str.length() <= 0)
						return;
					
					switch (type)
					{
						case "flt":
							params[i] = Float.parseFloat(str);
							break;
							
						case "int":
							params[i] = Integer.parseInt(str);
							break;
							
						case "bln":
							params[i] = Boolean.parseBoolean(str);
							break;
							
						case "str":
							params[i] = str;
							break;
					}
					
					if(rqst.paramTypes[i].equals("any"))
					{
						params[i] = new Object[] { type, params[i] };
					}
				}
				catch(NumberFormatException e)
				{
					JOptionPane.showMessageDialog(null, "You need to enter a valid number", "Not a number",
						JOptionPane.WARNING_MESSAGE);
					i--;
				}
			}
			TDNRoot cmd = rqst.toTDN(rqst.parseParams(null, params));

			if (_robot.api == null)
			{
				JOptionPane.showMessageDialog(this, "Robot not connected!", "Disconnected", JOptionPane.ERROR_MESSAGE);
				return;
			};
			
			try
			{
				TDNRoot response = _robot.api.rawRequest(cmd);
				System.out.println(response.toString());

				JOptionPane.showMessageDialog(this, response.toString(), "Response", JOptionPane.PLAIN_MESSAGE);
			}
			catch (IOException e1)
			{
				JOptionPane.showMessageDialog(null, "Sailed to send the request: " + e1.getMessage(), "Connection error",
					JOptionPane.ERROR_MESSAGE);
			}
		});
		sendBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		createCmdPanel.add(sendBtn);

		top.add(createCmdPanel);
		
		JTextArea poseInfoTxt = new JTextArea();
		poseInfoTxt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
		poseInfoTxt.setEditable(false);
		poseInfoTxt.setPreferredSize(new Dimension(100, 150));
		top.add(poseInfoTxt);
		
		_eventNames = new DefaultListModel<String>();
		_eventList = new JList<String>(_eventNames);
		_eventList.setAlignmentX(Component.CENTER_ALIGNMENT);
		_eventList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
		_eventList.addMouseListener(new MouseAdapter() {
		    public void mouseClicked(MouseEvent evt) {
		        if (evt.getClickCount() == 2) 
		        {
		            int index = _eventList.locationToIndex(evt.getPoint());
					JOptionPane.showMessageDialog(RobotCommanderWindow.this, "Data received with event " + _eventNames.getElementAt(index) + ":\n" + _eventsData.get(index).toString(), "Event Info", JOptionPane.PLAIN_MESSAGE);
		        }
		    }
		});
		JScrollPane eventListSP = new JScrollPane(_eventList);
		eventListSP.setPreferredSize(new Dimension(120, 150));
		top.add(eventListSP);
		getContentPane().add(top);
		
		JPanel loggerPanel = new JPanel();
		loggerPanel.setLayout(new BoxLayout(loggerPanel, BoxLayout.Y_AXIS));
		loggerPanel.setBorder(
			new CompoundBorder(new EmptyBorder(10, 10, 10, 10), BorderFactory.createTitledBorder("Logger output")));

		JTextArea loggerTextArea = new JTextArea(10, 80);
		loggerTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
		loggerTextArea.setBackground(Color.black);
		loggerTextArea.setForeground(Color.white);
		JScrollPane scrollPane = new JScrollPane(loggerTextArea);
		loggerTextArea.setEditable(false);
		loggerPanel.add(scrollPane);

		getContentPane().add(loggerPanel);
		pack();
		setMinimumSize(getPreferredSize());
		setLocationRelativeTo(null);

		setVisible(true);

		_rmLogReaderThread = new Thread(() ->
		{
			try
			{
				InputStream stream = _robot.remoteLog.getInputStream();
				InputStreamReader reader = new InputStreamReader(stream);
				StringBuilder sb = new StringBuilder();
				while (!Thread.interrupted())
				{
					if (stream.available() > 0)
					{
						char buffer[] = new char[1024];
						int count = 0;

						do
						{
							count = reader.read(buffer);
							sb.append(buffer, 0, count);
						} while (stream.available() > 0);

						loggerTextArea.append(sb.toString());
						loggerTextArea.setCaretPosition(loggerTextArea.getDocument().getLength());
						sb.setLength(0);
					}
				}
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		_rmLogReaderThread.setDaemon(true);
		_rmLogReaderThread.setPriority(Thread.MIN_PRIORITY);
		_rmLogReaderThread.start();
	}

	@Override
	public void eventReceived(String name, TDNRoot params, Socket client)
	{
		_eventNames.addElement(name);
		_eventsData.add(params);
		if(_eventNames.size() > 15)
		{
			_eventNames.remove(0);
			_eventsData.remove(0);
		}
		
		int lastIndex = _eventNames.getSize() - 1;
		if (lastIndex >= 0) {
		   _eventList.ensureIndexIsVisible(lastIndex);
		}
	}

	@Override
	public void moveStarted(int id, Move move)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveStopped(int id, Move move)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void atWaypoint(int robotID, Waypoint waypoint, Pose pose, int sequence)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pathComplete(int robotID, Waypoint waypoint, Pose pose, int sequence)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pathInterrupted(int robotID, Waypoint waypoint, Pose pose, int sequence)
	{
		// TODO Auto-generated method stub
		
	}
}
