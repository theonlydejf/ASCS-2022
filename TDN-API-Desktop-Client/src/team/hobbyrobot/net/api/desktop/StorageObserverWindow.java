package team.hobbyrobot.net.api.desktop;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import team.hobbyrobot.graphics.PaintPanel;
import team.hobbyrobot.net.api.streaming.TDNSender;
import team.hobbyrobot.python.Bridge;
import team.hobbyrobot.robotobserver.*;

public class StorageObserverWindow extends JFrame 
{
    static final String settingsPath = "/Users/david/Documents/MAP/ascs-settings.json";

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private RobotViewerGraphics _robotViewerGraphics;
    private PaintPanel _robotViewer;
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

        RobotObserver observer = new RobotObserver(bridge);
        RobotCorrector corrector = new RobotCorrector(observer);
        _tdnSender = new TDNSender("localhost", 3333);
        _robotViewer = new PaintPanel();
        _robotViewerGraphics = new RobotViewerGraphics(null, observer, _robotViewer, 2340);

        JButton btn = new JButton();
        btn.setLocation(0,  0);
        btn.setText("Connect to vehicle");
        btn.addActionListener(e -> {
            try 
            {
                corrector.startCorrectingRobot(_tdnSender, 0);
                btn.setEnabled(false);
            } 
            catch (IOException e1) 
            {
                e1.printStackTrace();
            }
        });
        

        
        setLayout(new BorderLayout());
        setTitle("Storage Observer Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        double w = 2340;
        double h = 1120;
        _robotViewer.setPreferredSize(new Dimension((int) (w * .3f), (int) (h * .3f)));
        
        getContentPane().add(_robotViewer);
        _robotViewer.add(btn);
        // _paintPanel.addMouseListener(this);

        pack();

        setLocationRelativeTo(null);

        setVisible(true);

    }
}
