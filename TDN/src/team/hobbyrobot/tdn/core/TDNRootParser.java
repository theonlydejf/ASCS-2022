package team.hobbyrobot.tdn.core;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map.Entry;

import team.hobbyrobot.tdn.core.TDNBufferedReader;
import team.hobbyrobot.tdn.core.TDNBufferedWriter;
import team.hobbyrobot.tdn.core.TDNControlCharacter;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNTypeParser;
import team.hobbyrobot.tdn.core.TDNValue;

public class TDNRootParser implements TDNTypeParser
{

	@Override
	public String typeKey()
	{
		// TODO Auto-generated method stub
		return "obj";
	}

	@Override
	public Object defaultValue()
	{
		return new TDNRoot();
	}
	
	@Override
	public TDNValue readFromStream(TDNBufferedReader reader) throws IOException
	{
		TDNRoot obj = new TDNRoot();
        Entry<String, TDNValue> readData = readKeyValuePair(reader);

        while (readData != null)
        {
            obj.insertValue(readData.getKey(), readData.getValue());
            readData = readKeyValuePair(reader);
        }

        return new TDNValue(obj, this);
	}
	
    private Entry<String, TDNValue> readKeyValuePair(TDNBufferedReader reader) throws IOException
    {
        int firstChar = reader.read();
        if (reader.lastReadControlCharacter() == TDNControlCharacter.VALUE_SAPERATOR)
            return null;

        reader.queueCharacter((char)firstChar);
        String type = reader.readType();
        String key = reader.readKey();
        TDNTypeParser parser = reader.settings.parsers().get(type);
        if(parser == null)
        	throw new RuntimeException("Error when parsing TDN: \"" + type + "\" is not known type!");
        TDNValue value = parser.readFromStream(reader);

        return new AbstractMap.SimpleEntry<String, TDNValue>(key, value);
    }

	@Override
	public void writeToStream(TDNBufferedWriter writer, Object value) throws IOException
	{
        TDNRoot obj = (TDNRoot)value;
        
        for(Entry<String, TDNValue> notatedValue : obj)
        {
            TDNTypeParser parser = notatedValue.getValue().parser();
            if(parser == null)
            	throw new RuntimeException("Error when parsing root" + obj.toString() + ": parser is null!");
        	writer.writeType(parser.typeKey());
            writer.writeKey(notatedValue.getKey());
            notatedValue.getValue().parser().writeToStream(writer, notatedValue.getValue().value);
        }
        writer.writeValue("");
	}

}
