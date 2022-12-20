package team.hobbyrobot.sim.ascs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.util.LinkedList;

import javax.swing.JFrame;

import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.MoveListener;
import lejos.robotics.navigation.MoveProvider;
import team.hobbyrobot.sim.ascs.SimRobot.Pose;

public class SimWindow extends JFrame implements MoveListener
{
	private static long planeWidth, planeHeight;
	
	private SimRobotViewer _robotViewer;
	private LinkedList<SimRobot> _robots = new LinkedList<SimRobot>();

	public static void main(String[] args) throws IOException
	{
		//planeWidth = 2360l;
		//planeHeight = 1140l;
		
		planeWidth = Long.parseLong(args[0]);
		planeHeight = Long.parseLong(args[1]);
		
		SimWindow w = new SimWindow();
		
		for(int i = 2; i < args.length; i++)
		{
			String[] data = args[i].split(",");
			int logger = Integer.parseInt(data[0]);
			int api = Integer.parseInt(data[1]);
			int corrector = Integer.parseInt(data[2]);

			float x = Float.parseFloat(data[3]);
			float y = Float.parseFloat(data[4]);
			float h = Float.parseFloat(data[5]);
			
			SimRobot robot = new SimRobot(logger, api, corrector);
			robot.pose = new Pose(x, y, h);
			robot.addMoveListener(w);
			w._robots.add(robot);
			robot.rotate(0);
		}
				
		/*SimRobot robot1 = new SimRobot(2111, 2222, 2333);
		robot1.pose = new Pose((int)planeWidth/2-100, (int)planeHeight/2, 90);
		robot1.addMoveListener(w);
		w._robots.add(robot1);

		SimRobot robot2 = new SimRobot(3111, 3222, 3333);
		robot2.pose = new Pose((int)planeWidth/2+100, (int)planeHeight/2, 90);
		robot2.addMoveListener(w);
		w._robots.add(robot2);*/
	}

	public SimWindow() throws IOException
	{
		SimBridge bridgeServer = new SimBridge(_robots, 1111);
		bridgeServer.start();

		setLayout(new BorderLayout());
		setTitle("Robots simulator");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);

		int robotViewerWidth = 500;

		_robotViewer = new SimRobotViewer(_robots, planeWidth);
		_robotViewer.setPreferredSize(
			new Dimension(robotViewerWidth, (int) ((robotViewerWidth / (float) planeWidth) * planeHeight)));

		getContentPane().add(_robotViewer);

		pack();

		setLocationRelativeTo(null);

		setVisible(true);
		
		Thread updateThread = new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				while(true)
				{
					//System.out.println("Repainting...");
					_robotViewer.repaint();
					try
					{
						Thread.sleep(40);
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		updateThread.setDaemon(true);
		updateThread.setPriority(Thread.MIN_PRIORITY);
		updateThread.start();

	}

	private void doStuff(SimRobot robot) throws InterruptedException
	{
	}

	@Override
	public void moveStarted(Move event, MoveProvider mp)
	{
		SimRobot robot = (SimRobot) mp;
		System.out.println("Move started on robot " + robot.id + ": " + event.toString());
	}

	@Override
	public void moveStopped(Move event, MoveProvider mp)
	{
		SimRobot robot = (SimRobot) mp;
		System.out.println("Move stopped on robot " + robot.id + ": " + event.toString());
	}
}
