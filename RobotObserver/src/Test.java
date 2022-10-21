import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.swing.JFrame;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import team.hobbyrobot.python.Bridge;
import team.hobbyrobot.python.BridgeListener;
import team.hobbyrobot.python.PythonInterpreter;

public class Test implements BridgeListener
{
	static final String settingsPath = "/Users/david/Documents/MAP/ascs-settings.json";
	
	static final String scriptPath = "/Users/david/Documents/MAP/StorageObserver/py/start.py";
	static final String pythonPath = "/opt/local/bin/python3.9";
	
	public static void main(String[] args) throws IOException, InterruptedException, ParseException
	{
		JSONParser jsonParser = new JSONParser();
        FileReader reader = new FileReader(settingsPath);
        
        JSONObject settings = (JSONObject) ((JSONObject) jsonParser.parse(reader)).get("storage-observer");
        System.out.println(settings.toJSONString());
        
        //PythonInterpreter interpreter = new PythonInterpreter(pythonPath);
        //Process p = interpreter.startScript(scriptPath, "/Users/david/Documents/MAP/ascs-settings.json", "-v");
                
        JSONObject cameraServerStg = (JSONObject) settings.get("camera-server");
		Bridge bridge = new Bridge((String)cameraServerStg.get("host"), (int)(long)cameraServerStg.get("port"));
		bridge.addListener(new Test());
		bridge.start();
		
		
		System.out.println("Waiting for user to end...");
		System.in.read();
		bridge.close();
		//System.out.println("Waiting for python server to end...");
		//while(p.isAlive()) ;
		//System.out.println("done!");
		
		/*
		HashMap last = null;
		while(true)
		{
			HashMap _new = bridge.getLast();
			System.out.println("---");
			if(last == null || _new == null || last.equals(_new))
				continue;
			
			last = _new;
			System.out.println(_new.get("ahoj"));
			
		}
		*/
	}
	
	static String readInputStream(InputStream stream) throws IOException
	{
		 int bufferSize = 1024;
		 char[] buffer = new char[bufferSize];
		 StringBuilder out = new StringBuilder();
		 Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
		 for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
		     out.append(buffer, 0, numRead);
		 }
		 return out.toString();
	}

	@Override
	public void dataReceived(JSONObject object)
	{
		// TODO Auto-generated method stub
		System.out.println(object.get("robots"));
	}

}
