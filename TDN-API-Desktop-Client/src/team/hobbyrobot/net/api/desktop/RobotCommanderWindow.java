package team.hobbyrobot.net.api.desktop;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
import team.hobbyrobot.tdn.core.TDNRoot;

public class RobotCommanderWindow extends JFrame
{
    private static Map<String, Request> _requests;
    private TDNAPIClient _apiClient = null;
    
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
    
    public static void main(String[] args) throws UnknownHostException, IOException
    {
        Logger l = new Logger();
        l.registerEndpoint(new PrintWriter(System.out));
        RobotCommanderWindow w = new RobotCommanderWindow(new TDNAPIClient("192.168.1.101", 2222, l));
    }
    
    public RobotCommanderWindow(TDNAPIClient client)
    {
        _apiClient = client;

        setTitle(client.getIP());
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
        
        pack();

        setLocationRelativeTo(null);

        setVisible(true);
    }
}
