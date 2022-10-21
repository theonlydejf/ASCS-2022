package team.hobbyrobot.tdn.core;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TDNParserCollection implements Iterable<Entry<String, TDNTypeParser>>, Map<String, TDNTypeParser>
{
    public Hashtable<String, TDNTypeParser> TDNParserLookupTable;

    public TDNParserCollection(TDNTypeParser... parsers)
    {
        TDNParserLookupTable = new Hashtable<String, TDNTypeParser>();
        for (TDNTypeParser parser : parsers)
        {
        	TDNParserLookupTable.put(parser.typeKey(), parser);        	
        }
    }

    public TDNTypeParser get(Object key)
    {
    	return TDNParserLookupTable.get(key);
    }
    
    public int size()
    {
    	return TDNParserLookupTable.size();
    }
    
	@Override
	public Iterator<Entry<String, TDNTypeParser>> iterator()
	{
		return TDNParserLookupTable.entrySet().iterator();
	}

	@Override
	public boolean isEmpty()
	{
		return TDNParserLookupTable.isEmpty();
	}

	@Override
	public boolean containsKey(Object key)
	{
		return TDNParserLookupTable.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value)
	{
		return TDNParserLookupTable.containsValue(value);
	}

	@Override
	public TDNTypeParser put(String key, TDNTypeParser value)
	{
		throw new UnsupportedOperationException("Parser collection is readonly!");
	}

	@Override
	public TDNTypeParser remove(Object key)
	{
		throw new UnsupportedOperationException("Parser collection is readonly!");
	}

	@Override
	public void putAll(Map<? extends String, ? extends TDNTypeParser> m)
	{
		throw new UnsupportedOperationException("Parser collection is readonly!");
	}

	@Override
	public void clear()
	{
		throw new UnsupportedOperationException("Parser collection is readonly!");
	}

	@Override
	public Set<String> keySet()
	{
		return TDNParserLookupTable.keySet();
	}

	@Override
	public Collection<TDNTypeParser> values()
	{
		return TDNParserLookupTable.values();
	}

	@Override
	public Set<Entry<String, TDNTypeParser>> entrySet()
	{
		return TDNParserLookupTable.entrySet();
	}
}
