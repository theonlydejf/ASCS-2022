package team.hobbyrobot.net.api.desktop;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.FontUIResource;

import org.json.simple.parser.ParseException;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.net.api.TDNAPIClient;
import team.hobbyrobot.net.api.desktop.requests.Request;
import team.hobbyrobot.net.api.desktop.requests.RequestGenerator;
import team.hobbyrobot.net.api.remoteevents.RemoteEventListener;
import team.hobbyrobot.net.api.remoteevents.RemoteEventListenerServer;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class OldRobotCommanderWindow extends JFrame implements RemoteEventListener
{
    private static Map<String, Request> _requests;
    private TDNAPIClient _apiClient = null;
    private int _id;
    private RemoteEventListenerServer _eventServer;
    private RobotCommanderListener _listener;
    private String _robotIpStr;
    private JLabel _eventLbl;
    
    static
    {
        try 
        {
            _requests = RequestGenerator.loadRequests("/Users/david/Documents/MAP/movement-vehicle-commands.json");
        } 
        catch (IOException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        catch (ParseException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private static RemoteEventListenerServer eventServer;
    public static void main(String[] args) throws UnknownHostException, IOException
    {
        Logger l = new Logger();
        l.registerEndpoint(new PrintWriter(System.out));
        
        eventServer = new RemoteEventListenerServer(5555, l);
        eventServer.start();
        eventServer.startRegisteringClients();
        
        OldRobotCommanderWindow w = new OldRobotCommanderWindow(new TDNAPIClient("192.168.1.100", 2222, l), 0, eventServer, new _RemoteEventListener());
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
    
    public OldRobotCommanderWindow(TDNAPIClient client, int id, RemoteEventListenerServer eventServer, RobotCommanderListener listener)
    {
        _apiClient = client;
        _id = id;
        _eventServer = eventServer;
        _listener = listener;

        if(_eventServer != null)
        {
        	_eventServer.addListener(this);
        	try
			{
        		TDNValue portTDNValue = new TDNValue(_eventServer.getPort(), TDNParsers.INTEGER);
        		TDNValue idTDNValue = new TDNValue(_id, TDNParsers.INTEGER);

        		TDNRoot connectEventRqst = _requests.get("registerMoveListener").toTDN(portTDNValue, idTDNValue);
				TDNRoot response = client.rawRequest(connectEventRqst);
				
				JOptionPane.showMessageDialog(this, "Event provider connected!\n" + response.toString(), "Success!", JOptionPane.INFORMATION_MESSAGE);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        _robotIpStr = client.getIP();
        setTitle(_robotIpStr);
        setResizable(true);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) 
            {
                try 
                {
                    _apiClient.close();
                } 
                catch (IOException e) 
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        
        JPanel createCmdPanel = new JPanel();
        createCmdPanel.setLayout(new BoxLayout(createCmdPanel, BoxLayout.Y_AXIS));
        createCmdPanel.setBorder(new CompoundBorder(new EmptyBorder(10, 10, 10, 10), BorderFactory.createTitledBorder("Command manager")));
        JTextField cmdTxt = new JTextField();
        cmdTxt.setColumns(10);
        createCmdPanel.add(cmdTxt);

        JButton sendBtn = new JButton();
        sendBtn.setText("Create & send");
        sendBtn.addActionListener(w -> 
        {
            Request rqst = _requests.get(cmdTxt.getText());
            
            Object[] params = new Object[rqst.params.length];
            for(int i = 0; i < params.length; i++)
            {
                switch(rqst.paramTypes[i])
                {
                    case "flt":
                        params[i] = Float.parseFloat(JOptionPane.showInputDialog(rqst.params[i]));
                        break;
                        
                    case "int":
                        params[i] = Integer.parseInt(JOptionPane.showInputDialog(rqst.params[i]));
                        break;
                        
                    case "bln":
                        params[i] = Boolean.parseBoolean(JOptionPane.showInputDialog(rqst.params[i]));
                        break;
                        
                    case "str":
                        params[i] = JOptionPane.showInputDialog(rqst.params[i]);
                        break;
                }
            }
            TDNRoot cmd = rqst.toTDN(rqst.parseParams(null, params));
            try 
            {
                System.out.print("Generated command: ");
                cmd.writeToStream(new BufferedWriter(new OutputStreamWriter(System.out)));
                System.out.println();
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
            }
            
            if(_apiClient == null)
            {
                JOptionPane.showMessageDialog(this, "Robot not connected!", "Disconnected", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try 
            {
                TDNRoot response = _apiClient.rawRequest(cmd);
                
                JOptionPane.showMessageDialog(this, response.toString(), "Response", JOptionPane.PLAIN_MESSAGE);
                System.out.println(response.toString());
            } 
            catch (IOException e1) 
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
        createCmdPanel.add(sendBtn);
                
        getContentPane().add(createCmdPanel);
        _eventLbl = new JLabel("No last event");
        getContentPane().add(_eventLbl);
        
        pack();

        setLocationRelativeTo(null);

        setVisible(true);
    }

	@Override
	public void eventReceived(String name, TDNRoot params, Socket client)
	{
		TDNValue id = params.get("id");
		if(id != null && (int)id.as() == _id)
		{
			_eventLbl.setText("Last event: " + name);
			_listener.moveEventReceived(name, params, client, _id);
		}
	}
}
