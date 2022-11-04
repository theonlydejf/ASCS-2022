package team.hobbyrobot.tdn.core;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

import team.hobbyrobot.tdn.base.DefaultTDNParserSettings;
import team.hobbyrobot.tdn.base.TDNArray;

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
	
	public String toString()
	{
	    return toString(TOSTRING_INDENT);
	}
	
	private static final String TOSTRING_INDENT = "  ";
	private String toString(String prefix)
	{
	    StringBuilder mainSb = new StringBuilder();
	    //if(prefix.length() > TOSTRING_INDENT.length())
        //    mainSb.append("\n");
	    
        mainSb.append(prefix.substring(TOSTRING_INDENT.length()) + "(\n");
        StringBuilder sb = new StringBuilder();
        for (Entry<String, TDNValue> val : this)
        {
            sb.append(prefix + "{" + val.getValue().parser().typeKey() + "} " + val.getKey() + ": ");
            
            if (val.getValue().value instanceof TDNRoot)
            {
                sb.append("\n" + ((TDNRoot) val.getValue().value).toString(prefix + TOSTRING_INDENT));
                continue;
            }
            if (val.getValue().value instanceof TDNArray)
            {
                mainSb.append(sb.toString() + "\n" + prefix + "[\n");
                TDNArray arr = (TDNArray) val.getValue().value;
                int i = 0;
                for (Object item : arr)
                {
                    i++;
                    if (arr.itemParser.typeKey().equals(new TDNRootParser().typeKey()))
                    {
                        String rootStr = ((TDNRoot) item).toString(prefix + TOSTRING_INDENT + TOSTRING_INDENT);
                        mainSb.append(rootStr.substring(0, rootStr.length() - 1));
                    }
                    else
                    {
                        mainSb.append(prefix + TOSTRING_INDENT + item);
                    }
                    if(i < arr.size())
                        mainSb.append(",");
                    mainSb.append("\n");
                    continue;
                }
                
                mainSb.append(prefix + "]\n");
                sb = new StringBuilder();
                continue;
            }
            mainSb.append(sb.toString() + val.getValue().value + "\n");
            sb = new StringBuilder();
        }
        mainSb.append(prefix.substring(2) + sb.toString() + ")\n");
        
        return mainSb.toString();
	}
}
