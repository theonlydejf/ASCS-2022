package team.hobbyrobot.robotobserver;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Hashtable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import team.hobbyrobot.net.api.streaming.TDNSender;

public class RobotCorrector implements RobotObserverListener
{
    private RobotObserver _observer;
    
    private Object _tableLock = new Object(); 
    private Hashtable<Integer, RobotInfo> _targetedRobots = new Hashtable<Integer, RobotInfo>();

    public RobotCorrector(RobotObserver observer)
    {
        _observer = observer;
        observer.addListener(this);
    }
    
    public void startCorrectingRobot(TDNSender robot, int id) throws UnknownHostException, IOException
    {
        if(!robot.isConnected())
            robot.connect();
        
        System.out.println("Robot " + id + " is connected!");
        synchronized(_tableLock)
        {
            _targetedRobots.put(id, new RobotInfo(robot, id, System.currentTimeMillis()));            
        }
    }
    
    public void stopCorrectingRobot(int id) throws IOException
    {
        RobotInfo robot = _targetedRobots.get(id);
        synchronized(_tableLock)
        {
            _targetedRobots.remove(id);
        }
        robot.sender.close();
    } 

    @Override
    public void robotsReceived(JSONArray robots) 
    {
        for(Object json : robots)
        {
            JSONObject robotJSON = (JSONObject) json;
            
            RobotInfo robot = null;
            synchronized(_tableLock)
            {
                int robotID = (int)(long)robotJSON.get("id");
                if(_targetedRobots.containsKey(robotID))
                    robot = _targetedRobots.get(robotID);
            }
            
            if(robot == null)
                continue;
            
            correctRobot(robot, RobotModel.fromJSON(robotJSON));
        }
    }
    
    private void correctRobot(RobotInfo robot, RobotModel model)
    {        
        try 
        {
            if(System.currentTimeMillis() - robot.lastSendTime >= 200)
            {
                robot.sender.send(model.toTDN());
                System.out.println(" sent to " + model.id + ": " + model.toTDN());
                robot.lastSendTime = System.currentTimeMillis();
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
            System.err.println("Failed to send robot tdn data");
        }
    }
    
    private static class RobotInfo
    {
        public RobotInfo(TDNSender sender, int id, long lastSendTime) 
        {
            super();
            this.sender = sender;
            this.id = id;
            this.lastSendTime = lastSendTime;
        }
        public TDNSender sender;
        public int id;
        public long lastSendTime;
    }
}
