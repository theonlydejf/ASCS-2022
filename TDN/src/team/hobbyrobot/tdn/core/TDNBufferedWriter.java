package team.hobbyrobot.tdn.core;

import java.io.BufferedWriter;
import java.io.IOException;

public class TDNBufferedWriter
{
    private final BufferedWriter bw;
    private final TDNParserSettings settings;

    //public TDNBufferedWriter(BufferedWriter streamWriter) : this(streamWriter, new DefaultTDNParserSettings()) { }

    public TDNBufferedWriter(BufferedWriter writer, TDNParserSettings parserSettings)
    {
        bw = writer;
        settings = parserSettings;
    }

    public void write(char c) throws IOException
    {
    	write(c, true);
    }
    
    public void write(char c, boolean flushStream) throws IOException
    {
        if (ArrayUtils.indexOf(settings.invalidChars(), c) >= 0)
            bw.write(settings.escapeCharacter());
        bw.write(c);
        if (flushStream)
            bw.flush();
    }

    public void write(String s) throws IOException
    {
    	write(s, true);
    }
    
    public void write(String s, boolean flushStream) throws IOException
    {
    	for (char c : s.toCharArray())
            write(c, false);
        if(flushStream)
            bw.flush();
    }

    public void writeType(String value) throws IOException
    {
        write(value, false);
        bw.write(settings.typeSeparator());
        bw.flush();
    }

    public void writeKey(String value) throws IOException
    {
        write(value, false);
        bw.write(settings.keySeparator());
        bw.flush();
    }

    public void writeValue(String value) throws IOException
    {
        write(value, false);
        bw.write(settings.valueSeparator());
        bw.flush();
    }
}