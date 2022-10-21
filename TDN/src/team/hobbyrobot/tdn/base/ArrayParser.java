package team.hobbyrobot.tdn.base;

import java.io.IOException;

import team.hobbyrobot.tdn.core.TDNBufferedReader;
import team.hobbyrobot.tdn.core.TDNBufferedWriter;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNTypeParser;
import team.hobbyrobot.tdn.core.TDNValue;

public class ArrayParser implements TDNTypeParser
{
    private static final TDNTypeParser intParser = TDNParsers.INTEGER;

	@Override
	public String typeKey()
	{
		return "arr";
	}

	@Override
	public Object defaultValue()
	{
		return new TDNArray(new Object[0], intParser);
	}
	
	@Override
	public TDNValue readFromStream(TDNBufferedReader reader) throws IOException
	{
        int itemCnt = (int)intParser.readFromStream(reader).value;
        String itemType = reader.readValue();
        TDNTypeParser itemParser = reader.settings.parsers().get(itemType);

        Object[] array = new Object[itemCnt];

        for (int i = 0; i < itemCnt; i++)
            array[i] = itemParser.readFromStream(reader).value;

        return new TDNValue(new TDNArray(array, itemParser), this);
	}

	@Override
	public void writeToStream(TDNBufferedWriter writer, Object value) throws IOException
	{
		if(!(value instanceof TDNArray))
            throw new IllegalArgumentException("Value is not type of TDNArray!");

        TDNArray arr = (TDNArray)value;
        intParser.writeToStream(writer, arr.value.length);
        writer.writeValue(arr.itemParser.typeKey());
        for(Object obj : arr)
        	arr.itemParser.writeToStream(writer, obj);
	}

}
