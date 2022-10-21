package team.hobbyrobot.tdn.base;

import java.io.IOException;

import team.hobbyrobot.tdn.core.TDNBufferedReader;
import team.hobbyrobot.tdn.core.TDNBufferedWriter;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNTypeParser;
import team.hobbyrobot.tdn.core.TDNValue;

public class BooleanParser implements TDNTypeParser
{

	@Override
	public String typeKey()
	{
		return "bln";
	}

	@Override
	public Object defaultValue()
	{
		return false;
	}
	
	@Override
	public TDNValue readFromStream(TDNBufferedReader reader) throws IOException
	{
		String value = reader.readValue();
		
		return new TDNValue(value.toLowerCase().equals("true"), this);
	}

	@Override
	public void writeToStream(TDNBufferedWriter writer, Object value) throws IOException
	{
        writer.writeValue(String.valueOf((boolean)value).toLowerCase());
	}

}
