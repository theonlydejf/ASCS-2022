package team.hobbyrobot.net.api.desktop.requests;

import team.hobbyrobot.tdn.base.DefaultTDNParserSettings;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNParserSettings;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class Request 
{
    public static final String SERVICE_KEYWORD = "service";
    public static final String REQUEST_KEYWORD = "request";
    public static final String PARAMS_KEYWORD = "params";
    
    public String service;
    public String key;
    public String[] params;
    public String[] paramTypes;
    public String[] response;
    

    public Request(String key, String service, String[] params, String[] paramTypes, String[] response) 
    {
        super();
        this.key = key;
        this.service = service;
        this.params = params;
        this.paramTypes = paramTypes;
        this.response = response;
    }
    
    public TDNRoot toTDN(TDNValue... params)
    {
        TDNRoot paramsFinal = new TDNRoot();
        for(int i = 0; i < params.length; i++)
        {
            paramsFinal.insertValue(this.params[i], params[i]);
        }
        return new TDNRoot()
                .insertValue(SERVICE_KEYWORD, new TDNValue(service, TDNParsers.STRING))
                .insertValue(REQUEST_KEYWORD, new TDNValue(key, TDNParsers.STRING))
                .insertValue(PARAMS_KEYWORD, new TDNValue(paramsFinal, TDNParsers.ROOT));
    }
    
    public TDNValue[] parseParams(TDNParserSettings stg, Object... objects)
    {
        TDNValue[] out = new TDNValue[objects.length];
        
        if(stg == null)
            stg = new DefaultTDNParserSettings();
        
        for(int i = 0; i < params.length; i++)
        {
            out[i] = new TDNValue(objects[i], stg.parsers().get(paramTypes[i]));
        }
        
        return out;
    }
}
