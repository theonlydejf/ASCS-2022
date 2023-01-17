package team.hobbyrobot.sim.ascs;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SimBridge extends Thread
{
	private List<SimRobot> _robots;
	private ServerSocket _server;

	public SimBridge(List<SimRobot> robots, int port) throws IOException
	{
		_robots = robots;
		_server = new ServerSocket(port);
		setDaemon(true);
	}

	private Random rnd = new Random();

	@SuppressWarnings("unchecked")
	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				Socket s = _server.accept();
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
				while (!interrupted())
				{
					Thread.sleep(50 + rnd.nextInt(50));
					JSONArray array = new JSONArray();
					for (SimRobot robot : _robots)
					{
						synchronized (robot.pose)
						{
							JSONObject r = new JSONObject();
							r.put("id", robot.id);
							r.put("x", robot.pose.x);
							r.put("y", robot.pose.y);
							r.put("heading", robot.pose.heading);
							array.add(r);
						}
					}
					JSONObject out = new JSONObject();
					out.put("robots", array);
					String json = out.toJSONString();
					//System.out.println(json);
					pw.println(json);
					pw.flush();
				}
			}
			catch (InterruptedException e)
			{
				System.err.println("Bridge interrutped");
			}
			catch (IOException e)
			{
				System.out.println("Bridge disconnected.. waiting");
			}
		}
	}
}
