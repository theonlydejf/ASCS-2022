package team.hobbyrobot.net.api.desktop;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import team.hobbyrobot.net.api.streaming.TDNSender;
import team.hobbyrobot.python.Bridge;
import team.hobbyrobot.robotobserver.*;

public class StorageObserverWindow extends JFrame implements RobotObserverListener 
{
    static final String settingsPath = "/Users/david/Documents/MAP/ascs-settings.json";

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private RobotViewer _robotViewer;
    private TDNSender _tdnSender;

    public static void main(String[] args) throws IOException, ParseException 
    {
        StorageObserverWindow win = new StorageObserverWindow();
    }

    public StorageObserverWindow() throws IOException, ParseException 
    {
        // Load storage observer settings
        JSONParser jsonParser = new JSONParser();
        FileReader reader = new FileReader(settingsPath);
        JSONObject settings = (JSONObject) ((JSONObject) jsonParser.parse(reader)).get("robot-observer");
        
        // Connect to camera server through bridge
        JSONObject cameraServerStg = (JSONObject) settings.get("camera-server");
        Bridge bridge = new Bridge((String) cameraServerStg.get("host"), (int) (long) cameraServerStg.get("port"));
        bridge.start();

        _tdnSender = new TDNSender("192.168.1.101", 3333);

        JButton btn = new JButton();
        btn.setLocation(0,  0);
        btn.setText("Connect to vehicle");
        btn.addActionListener(e -> {
            try 
            {
                _tdnSender.connect();
            } 
            catch (IOException e1) 
            {
                e1.printStackTrace();
            }
        });
        
        RobotObserver observer = new RobotObserver(bridge);
        observer.addListener(this);

        setLayout(new BorderLayout());
        setTitle("Storage Observer Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        _robotViewer = new RobotViewer();
        bridge.addListener(_robotViewer);
        double scale = .5;
        double w = 2340;
        double h = 1120;
        _robotViewer.setPreferredSize(new Dimension((int) (w * scale), (int) (h * scale)));
        
        getContentPane().add(_robotViewer);
        _robotViewer.add(btn);
        
        _robotViewer.scale = scale;
        // _paintPanel.addMouseListener(this);

        pack();

        setLocationRelativeTo(null);

        setVisible(true);

    }
    
    long millis = System.currentTimeMillis();
    @Override
    public void robotsReceived(JSONArray robots) 
    {
        if(!_tdnSender.isConnected())
            return;
        
        RobotModel robot = null;     
        String json = "";
        System.out.println("robot received");
        for(Object obj : robots)
        {
            JSONObject robotJSON = (JSONObject) obj;
            if((long)robotJSON.get("id") == 5l)
            {
                System.out.println("FOUND!");
                robot = RobotModel.fromJSON(robotJSON);
                json = robotJSON.toJSONString();
                break;
            }
        }
        
        if(robot == null)
            return;
        
        try 
        {
            if(System.currentTimeMillis() - millis >= 200)
            {
                _tdnSender.send(robot.toTDN());
                System.out.println("sent: " + json);
                millis = System.currentTimeMillis();
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
            System.err.println("Failed to send robot tdn data");
        }
    }
}
