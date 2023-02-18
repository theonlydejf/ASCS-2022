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
    
    private Object _targetTableLock = new Object(); 
    private Object _modelTableLock = new Object();
    private Hashtable<Integer, RobotInfo> _targetedRobots = new Hashtable<Integer, RobotInfo>();
    private Hashtable<Integer, RobotModel> _robotModels = new Hashtable<Integer, RobotModel>();

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
        synchronized(_targetTableLock)
        {
            _targetedRobots.put(id, new RobotInfo(robot, id, System.currentTimeMillis()));            
        }
    }
    
    public void stopCorrectingRobot(int id) throws IOException
    {
        RobotInfo robot = _targetedRobots.get(id);
        synchronized(_targetTableLock)
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
            RobotModel model = RobotModel.fromJSON(robotJSON);
            
            RobotInfo robot = null;
            synchronized(_targetTableLock)
            {
                int robotID = (int)(long)robotJSON.get("id");
                synchronized(_modelTableLock)
                {
                	_robotModels.put(robotID, model);
                }
                
                robot = _targetedRobots.get(robotID);
                if(!_targetedRobots.containsKey(robotID))
                	continue;
            }
            
            correctRobot(robot, model);
        }
    }
    
    public RobotModel getRobotModel(int id)
    {
        synchronized(_modelTableLock)
        {
            return _robotModels.get(id);
        }
    }
    
    private void correctRobot(RobotInfo robot, RobotModel model)
    {        
        try 
        {
            if(System.currentTimeMillis() - robot.lastSendTime >= 200)
            {
                robot.sender.send(model.toTDN());
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
