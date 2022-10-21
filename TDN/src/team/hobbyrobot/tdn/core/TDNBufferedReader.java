package team.hobbyrobot.tdn.core;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TDNBufferedReader
{
	private TDNControlCharacter lastReadControlCharacter;
	public TDNControlCharacter lastReadControlCharacter()
	{
		return lastReadControlCharacter;
	}
	
	private boolean isLastReadCharacterEscaped;
	public boolean isLastReadCharacterEscaped()
	{
		return isLastReadCharacterEscaped;
	}
	

	private final BufferedReader br;
	private LinkedList<Character> charQueue = new LinkedList<Character>();
	public final TDNParserSettings settings;

	/*public TDNBufferedReader(BufferedReader reader) 
	{
		this(reader, new DefaultTDNParserSettings);
	}*/

	public TDNBufferedReader(BufferedReader reader, TDNParserSettings parserSettings)
    {
        br = reader;
        settings = parserSettings;
    }

	/*
	 * public TDNCharStreamReader(StreamReader streamReader, DefaultTDNParserSettings
	 * defaultTDNParserSettings)
	 * {
	 * sr = streamReader;
	 * settings = defaultTDNParserSettings;
	 * }
	 */

	public int read() throws IOException
	{
		return read(true);
	}

	private int read(boolean useEscapeChar) throws IOException
	{
		int iChar = -1;
		if (charQueue.size() > 0)
			iChar = charQueue.removeFirst();
		else
			iChar = br.read();

		if (iChar < 0)
			return iChar;

		char c = (char) iChar;
		if (useEscapeChar)
			isLastReadCharacterEscaped = c == settings.escapeCharacter();

		if (c == settings.escapeCharacter() && useEscapeChar)
		{
			lastReadControlCharacter = TDNControlCharacter.NONE;
			return read(false);
		}

        if(!isLastReadCharacterEscaped)
        	lastReadControlCharacter = settings.getCharacterType(c);

		return c;
	}

	public void queueCharacter(char c) 
	{
		charQueue.add(c);
	}

	public String readUntilControlCharacter(TDNControlCharacter controlChar) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        int currChar;
        while ((currChar = read()) >= 0 && lastReadControlCharacter != controlChar)
            sb.append((char)currChar);

        if (currChar < 0)
            throw new EOFException("End of stream was unexpectadly reached before " + controlChar.toString());

        return sb.toString();
    }

	public String readType() throws IOException 
	{
		return readUntilControlCharacter(TDNControlCharacter.TYPE_SEPARATOR);
	}

	public String readKey() throws IOException
	{
		return readUntilControlCharacter(TDNControlCharacter.KEY_SEPARATOR);
	}

	public String readValue() throws IOException
	{
		return readUntilControlCharacter(TDNControlCharacter.VALUE_SAPERATOR);
	}
}