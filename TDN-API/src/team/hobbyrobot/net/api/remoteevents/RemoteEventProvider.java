package team.hobbyrobot.net.api.remoteevents;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class RemoteEventProvider 
{
    public static final String EVENT_KEY = "event";
    public static final String PARAMS_KEY = "params";
    
    private LinkedList<EventListener> _listeners = new LinkedList<EventListener>();
    
    private Object _eventLock = new Object();
    
    public void connectListener(String ip, int port) throws UnknownHostException, IOException
    {
        Socket socket = new Socket(ip, port);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        _listeners.add(new EventListener(socket, bw));
    }
    
    public void disconnectListener(String ip, int port)
    {
        // TODO remove and close
        throw new UnsupportedOperationException();
    }
    
    public void newEvent(String event, TDNRoot params)
    {
    	synchronized(_eventLock)
    	{
    		for(EventListener l : _listeners)
    		{
    			if(!l.connected)
    				continue;
    			try
    			{
    				createRoot(event, params).writeToStream(l.bw);
    				l.bw.flush();    				
    			}
    			catch (IOException ex)
    			{
    				l.connected = false;
    			}
    		}
    		   
    		//System.out.println("Notified " + _listeners.size() + " listeners about event: " + event);
    		// Wait to give listeners time to receive the event
    		/*try
			{
				Thread.sleep(200);
			}
			catch (InterruptedException e)
			{
				return;
			}*/
    	}
    }
    
    private static TDNRoot createRoot(String event, TDNRoot params)
    {
        return new TDNRoot()
                .insertValue(EVENT_KEY, new TDNValue(event, TDNParsers.STRING))
                .insertValue(PARAMS_KEY, new TDNValue(params, TDNParsers.ROOT));
    }
    
    private static class EventListener
    {
        public EventListener(Socket socket, BufferedWriter bw) 
        {
            super();
            this.socket = socket;
            this.bw = bw;
        }
        public Socket socket;
        public BufferedWriter bw;
        public boolean connected = true;
    }
}
