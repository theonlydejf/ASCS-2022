package team.hobbyrobot.tdn.core;

public class TDNParserSettings
{
    private char typeSeparator;
    private char keySeparator;
    private char valueSeparator;
    private char escapeCharacter;
    private TDNParserCollection parsers;
	
	public char typeSeparator()
	{
		return typeSeparator;
	}

	public char keySeparator()
	{
		return keySeparator;
	}

	public char valueSeparator()
	{
		return valueSeparator;
	}

	public char escapeCharacter()
	{
		return escapeCharacter;
	}
	
	public TDNParserCollection parsers()
	{
		return parsers;
	}

    public TDNParserSettings(char _typeSeparator, char _keySeparator, char _valueSeparator, char _escapeCharacter, TDNParserCollection _parsers)
    {
        typeSeparator = _typeSeparator;
        keySeparator = _keySeparator;
        valueSeparator = _valueSeparator;
        escapeCharacter = _escapeCharacter;
        parsers = _parsers;
    }
    
    Character[] controlChars()
    {
    	return new Character[] { typeSeparator, keySeparator, valueSeparator };
    }
    
    Character[] invalidChars()
    {
    	return new Character[] { typeSeparator, keySeparator, valueSeparator, escapeCharacter };
    }
    
    public TDNControlCharacter getCharacterType(char character)
    {
    	return TDNControlCharacter.fromInt(ArrayUtils.indexOf(controlChars(), character) + 1);
    }
}
