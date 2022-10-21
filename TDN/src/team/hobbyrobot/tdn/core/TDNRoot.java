package team.hobbyrobot.tdn.core;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

import team.hobbyrobot.tdn.base.DefaultTDNParserSettings;

import java.io.*;

public class TDNRoot implements Iterable<Entry<String, TDNValue>>
{
	public TDNRoot()
    {
        rootData = new Hashtable<String, TDNValue>();
    }

    Hashtable<String, TDNValue> rootData;
    
    public TDNValue get(String key)
    {
    	return get(key, false);
    }
    
    public TDNValue get(String key, boolean createNewRoots)
    {
        String[] path = key.split("\\.");
        Hashtable<String, TDNValue> root = getRootData(path, createNewRoots);
        return root.get(path[path.length - 1]);
    }
    
    public void put(String key, TDNValue value)
    {
        String[] path = key.split("\\.");
        Hashtable<String, TDNValue> root = getRootData(path, true);
        root.put(path[path.length - 1], value);
    }

    private Hashtable<String, TDNValue> getRootData(String[] path, boolean createNewRoots)
    {
    	String[] rootPath = Arrays.copyOf(path, path.length - 1);

    	Hashtable<String, TDNValue> currTable = rootData;
        for(String rootName : rootPath)
        {
            if (createNewRoots && !currTable.containsKey(rootName))
                currTable.put(rootName, new TDNValue(new TDNRoot(), new TDNRootParser()));
            TDNValue newRoot = currTable.get(rootName);
            if (newRoot.parser().typeKey() != new TDNRootParser().typeKey())
            {
            	StringBuilder sb = new StringBuilder();
            	for(String s : path)
            	{
            		sb.append(s);
            		sb.append('.');
            	}
            	sb.setLength(sb.length() - 1);
            	throw new IllegalArgumentException(String.format("Root \"%1$s\" in path \"%2$s\" is not a valid root!", rootName, sb.toString()));
            }
            currTable = ((TDNRoot)newRoot.value).rootData;
        }

        return currTable;
    }

    public void writeToStream(BufferedWriter s) throws IOException
    {
        TDNBufferedWriter sw = new TDNBufferedWriter(new BufferedWriter(s), new DefaultTDNParserSettings());

        new TDNRootParser().writeToStream(sw, this);
    }

    public static TDNRoot readFromStream(BufferedReader br) throws IOException
    {
        TDNBufferedReader reader = new TDNBufferedReader(br, new DefaultTDNParserSettings());
        TDNValue objVal = new TDNRootParser().readFromStream(reader);
        return (TDNRoot)objVal.value;
    }
    
    public TDNRoot insertValue(String key, TDNValue value)
    {
    	rootData.put(key, value);
    	return this;
    }

	@Override
	public Iterator<Entry<String, TDNValue>> iterator()
	{
		return rootData.entrySet().iterator();
	}
}
