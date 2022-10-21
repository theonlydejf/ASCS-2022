package team.hobbyrobot.subos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.subos.errorhandling.ErrorLogging;
import team.hobbyrobot.tdn.base.TDNArray;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNTypeParser;
import team.hobbyrobot.tdn.core.TDNValue;

public class Resources
{
	private static Resources _global = null;

	private File _file;
	private TDNRoot _resources = null;

	private Resources(String filename)
	{
		_file = new File(filename);
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					push();
				}
				catch (IOException e)
				{
					ErrorLogging
						.logError("Exception occ ured while finally pushing resources: " + Logger.getExceptionInfo(e));
				}
			}
		});
	}

	private void readResources() throws IOException
	{
		if (!_file.exists())
		{
			_resources = new TDNRoot();
			push();
		}
		BufferedReader reader = new BufferedReader(new FileReader(_file));
		_resources = TDNRoot.readFromStream(reader);
		reader.close();
	}

	public void push() throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(_file, false));
		_resources.writeToStream(writer);
		writer.close();
	}

	public Object get(String path)
	{
		return getTDN(path).value;
	}

	public TDNValue getTDN(String path)
	{
		return _resources.get(path, true);
	}

	public void set(String path, TDNValue value)
	{
		_resources.put(path, value);
	}

	public int getInt(String path)
	{
		return (int) checkType(path, TDNParsers.INTEGER);
	}

	public void setInt(String path, int value)
	{
		set(path, new TDNValue(value, TDNParsers.INTEGER));
	}

	public float getFloat(String path)
	{
		return (float) checkType(path, TDNParsers.FLOAT);
	}

	public void setFloat(String path, float value)
	{
		set(path, new TDNValue(value, TDNParsers.FLOAT));
	}

	public TDNRoot getTDNRoot()
	{
		return _resources;
	}
	
	public boolean getBoolean(String path)
	{
		return (boolean) checkType(path, TDNParsers.BOOLEAN);
	}

	public void setBoolean(String path, boolean value)
	{
		set(path, new TDNValue(value, TDNParsers.BOOLEAN));
	}
	
	public String getString(String path)
	{
		return (String) checkType(path, TDNParsers.STRING);
	}

	public void setString(String path, String value)
	{
		set(path, new TDNValue(value, TDNParsers.STRING));
	}
	
	public TDNRoot getRoot(String path)
	{
		return (TDNRoot) checkType(path, TDNParsers.ROOT);
	}

	public void setRoot(String path, TDNRoot value)
	{
		set(path, new TDNValue(value, TDNParsers.ROOT));
	}
	
	public TDNArray getArray(String path)
	{
		return (TDNArray) checkType(path, TDNParsers.ARRAY);
	}

	public void setArray(String path, TDNArray value)
	{
		set(path, new TDNValue(value, TDNParsers.ARRAY));
	}

	private Object checkType(String path, TDNTypeParser parser)
	{
		TDNValue val = getTDN(path);
		if (val == null)
		{
			val = new TDNValue(parser.defaultValue(), parser);
			_resources.put(path, val);
		}
		if (!val.parser().typeKey().equals(parser.typeKey()))
			throw new IllegalArgumentException("Following path ends at object with unexpected type: " + path);
		return val.value;
	}
	
	public static Resources loadGlobalResources(String filename) throws IOException
	{
		_global = new Resources(filename);
		_global.readResources();
		return _global;
	}

	public static Resources loadResources(String filename) throws IOException
	{
		Resources rscs = new Resources(filename);
		rscs.readResources();
		return rscs;
	}

	public static Resources global()
	{
		return _global;
	}
}
