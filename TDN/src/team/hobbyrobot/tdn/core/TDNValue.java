package team.hobbyrobot.tdn.core;

import java.util.ArrayList;
import java.util.List;

import team.hobbyrobot.tdn.base.TDNArray;

public class TDNValue
{
    public TDNValue(Object value, TDNTypeParser parser)
    {
        this.parser = parser;
        this.value = value;
    }

    private TDNTypeParser parser;
    public TDNTypeParser parser()
    {
    	return parser;
    }

    public Object value;

    public <T> T as()
    {
		return (T)value;
    }
    
    public static <T> List<T> asList(TDNValue value)
    {
    	TDNArray tdnArr = (TDNArray)value.value;
    	return tdnArr.asList();
    }
    //((TDNArray)value.Value).Value.Select(x => (T)x.Value).ToArray();
}
