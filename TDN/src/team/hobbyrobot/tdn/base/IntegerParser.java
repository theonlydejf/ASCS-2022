package team.hobbyrobot.tdn.base;

import java.io.IOException;

import team.hobbyrobot.tdn.core.TDNBufferedReader;
import team.hobbyrobot.tdn.core.TDNBufferedWriter;
import team.hobbyrobot.tdn.core.TDNTypeParser;
import team.hobbyrobot.tdn.core.TDNValue;

public class IntegerParser implements TDNTypeParser
{   
	@Override
	public String typeKey()
	{
		return "int";
	}

	@Override
	public Object defaultValue()
	{
		return 0;
	}
	
	@Override
	public TDNValue readFromStream(TDNBufferedReader reader) throws IOException
	{
		String value = reader.readValue();
		return new TDNValue(Integer.parseInt(value), this);
	}

	@Override
	public void writeToStream(TDNBufferedWriter writer, Object value) throws IOException
	{
		writer.writeValue(String.valueOf((int)value));
	}

}
