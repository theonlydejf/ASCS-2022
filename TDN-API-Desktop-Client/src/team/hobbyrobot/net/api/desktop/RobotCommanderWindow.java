package team.hobbyrobot.net.api.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
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

public class RobotCommanderWindow extends JFrame implements RemoteASCSRobotListener
{
	private RemoteASCSRobot _robot;
	private JLabel _eventLbl;
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
		RobotCommanderWindow w = new RobotCommanderWindow(new RemoteASCSRobot(0, "localhost", 1111, 2222, 3333, l));
		w.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	private static class _RemoteEventListener implements RobotCommanderListener
	{
		@Override
		public void moveEventReceived(String name, TDNRoot params, Socket client, int robotID)
		{
			System.out.println("Event name: " + name);
			System.out.println("Params:" + params.toString());
		}

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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

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

			Object[] params = new Object[rqst.params.length];
			for (int i = 0; i < params.length; i++)
			{
				String str = JOptionPane.showInputDialog( "{" + rqst.paramTypes[i] + "} " + rqst.params[i]);
				switch (rqst.paramTypes[i])
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
			}
			TDNRoot cmd = rqst.toTDN(rqst.parseParams(null, params));

			if (_robot.api == null)
			{
				JOptionPane.showMessageDialog(this, "Robot not connected!", "Disconnected", JOptionPane.ERROR_MESSAGE);
				return;
			}

			for(Object o : params)
				System.out.println(o);
			
			try
			{
				TDNRoot response = _robot.api.rawRequest(cmd);

				JOptionPane.showMessageDialog(this, response.toString(), "Response", JOptionPane.PLAIN_MESSAGE);
				System.out.println(response.toString());
			}
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		sendBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		createCmdPanel.add(sendBtn);

		getContentPane().add(createCmdPanel);
		_eventLbl = new JLabel("No last event");
		_eventLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
		getContentPane().add(_eventLbl);

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
		_eventLbl.setText("Last event: " + name);
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
