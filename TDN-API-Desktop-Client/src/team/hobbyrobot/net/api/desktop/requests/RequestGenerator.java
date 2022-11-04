package team.hobbyrobot.net.api.desktop.requests;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class RequestGenerator 
{    
    public static void main(String[] args) throws IOException, ParseException
    {
        HashMap<String, Request> m = (HashMap<String, Request>) loadRequests("/Users/david/Documents/MAP/movement-vehicle-commands.json");
        Request rqst = m.get("travel");
        TDNRoot travelCmd = rqst.toTDN(rqst.parseParams(null, 100f));
        for(Entry<String, TDNValue> kv : travelCmd)
        {
            System.out.println(kv.getKey() + ": " + kv.getValue().value);
        }
        System.out.println("===============");
        for(Entry<String, Request> e : m.entrySet())
        {
            System.out.println(e.getKey() + ": ");
            System.out.println("\tservice:\t" + e.getValue().service);
            System.out.println("\tparams:\t\t" + Arrays.toString(e.getValue().params));
            System.out.println("\tparamTypes:\t" + Arrays.toString(e.getValue().paramTypes));
            System.out.println("\tresponse:\t" + Arrays.toString(e.getValue().response));
        }
        
        System.out.println("===============");
        travelCmd.writeToStream(new BufferedWriter(new OutputStreamWriter(System.out)));
    }
    
    @SuppressWarnings("unchecked")
    public static Map<String, Request> loadRequests(String path) throws IOException, ParseException
    {
        JSONParser jsonParser = new JSONParser();
        FileReader reader = new FileReader(path);
        
        JSONObject obj = (JSONObject) jsonParser.parse(reader);
        
        String service = (String) obj.get("service-name");
        JSONObject requests = (JSONObject) obj.get("requests");
        
        HashMap<String, Request> out = new HashMap<String, Request>();
        for(Object requestRaw : requests.entrySet())
        {
            Entry<String, JSONObject> request = (Entry<String, JSONObject>) requestRaw;
            String key = request.getKey();
            JSONArray paramsJSON = (JSONArray) request.getValue().get("params");
            JSONArray paramsTypesJSON = (JSONArray) request.getValue().get("params-types");
            JSONArray responseJSON = (JSONArray) request.getValue().get("response");
            String[] params = (String[]) paramsJSON.toArray(new String[paramsJSON.size()]);
            String[] paramsTypes = (String[]) paramsTypesJSON.toArray(new String[paramsTypesJSON.size()]);
            String[] response = (String[]) responseJSON.toArray(new String[responseJSON.size()]);

            out.put(key, new Request(key, service, params, paramsTypes, response));
        }
        return out;
        
    }
}
