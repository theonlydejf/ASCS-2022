package team.hobbyrobot.net.api.desktop;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.net.api.TDNAPIClient;
import team.hobbyrobot.net.api.streaming.TDNSender;
import team.hobbyrobot.python.Bridge;
import team.hobbyrobot.robotobserver.RobotCorrector;
import team.hobbyrobot.robotobserver.RobotObserver;

public class RobotManagerWindow extends JFrame
{
    static final String settingsPath = "/Users/david/Documents/MAP/ascs-settings.json";
    
    public Logger _logger = new Logger();
    public LinkedList<RobotCommanderWindow> _commanderWindows = new LinkedList<>();
    
    public static void main(String[] args) throws IOException, ParseException
    {
        RobotManagerWindow w = new RobotManagerWindow();
        w._logger.registerEndpoint(new PrintWriter(System.out));
    }
    
    public RobotManagerWindow() throws IOException, ParseException
    {
        setTitle("TDN API Client");
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
        RobotCorrector corrector = new RobotCorrector(observer);
                
        JPanel robotConnectionPanel = new JPanel();
        robotConnectionPanel.setLayout(new BoxLayout(robotConnectionPanel, BoxLayout.PAGE_AXIS));
        
        JPanel ipSelection = new JPanel();
        ipSelection.setLayout(new FlowLayout());
        JLabel ipLbl = new JLabel("IP:");
        ipSelection.add(ipLbl);
        JTextField ipTxt = new JTextField("localhost");
        ipTxt.setColumns(8);
        ipSelection.add(ipTxt);
        JLabel portLbl = new JLabel("Port:");
        ipSelection.add(portLbl);
        JTextField portTxt = new JTextField("2222");
        portTxt.setColumns(3);
        ipSelection.add(portTxt);
        robotConnectionPanel.add(ipSelection);

        
        JPanel correctorParams = new JPanel();
        correctorParams.setLayout(new FlowLayout());
        JLabel idLbl = new JLabel("ID:");
        correctorParams.add(idLbl);
        JTextField idTxt = new JTextField("5");
        idTxt.setColumns(1);
        correctorParams.add(idTxt);
        JLabel correctorPortLbl = new JLabel("Corrector port:");
        correctorParams.add(correctorPortLbl);
        JTextField correctorPortTxt = new JTextField("3333");
        correctorPortTxt.setColumns(3);
        correctorParams.add(correctorPortTxt);
        robotConnectionPanel.add(correctorParams);
        
        JButton connectBtn = new JButton("Connect");
        connectBtn.addActionListener(e -> 
        {            
            try 
            {
                TDNAPIClient apiClient = new TDNAPIClient(ipTxt.getText(), Integer.parseInt(portTxt.getText()), _logger.createSubLogger(ipTxt.getText() + ":" + portTxt.getText()));
                _commanderWindows.add(new RobotCommanderWindow(apiClient));
                TDNSender robot = new TDNSender(ipTxt.getText(), Integer.parseInt(correctorPortTxt.getText()));
                corrector.startCorrectingRobot(robot, Integer.parseInt(idTxt.getText()));
                
                JOptionPane.showMessageDialog(this, "Connected to " + ipTxt.getText() + ":" + portTxt.getText(), "Success", JOptionPane.INFORMATION_MESSAGE);
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
        });
        robotConnectionPanel.add(connectBtn);
        
        getContentPane().add(robotConnectionPanel);
        
        JSONObject calibSettings = (JSONObject) observerSettings.get("calib");
        JSONObject planeSettings = (JSONObject) calibSettings.get("rectangle");
        long planeWidth = (long) planeSettings.get("width");
        long planeHeight = (long) planeSettings.get("height");
        int robotViewerWidth = 500;
        
        RobotViewer robotViewer = new RobotViewer(observer, (int) planeWidth);
        robotViewer.setPreferredSize(new Dimension(robotViewerWidth, (int)((robotViewerWidth / (float)planeWidth) * planeHeight)));
        robotViewer.setBorder(BorderFactory.createTitledBorder("Storage viewer"));
        
        getContentPane().add(robotViewer);
        
        pack();

        setLocationRelativeTo(null);

        setVisible(true);
    }
}
