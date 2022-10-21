package team.hobbyrobot.tdn.core;

public enum TDNControlCharacter
{
    NONE,
    TYPE_SEPARATOR,
    KEY_SEPARATOR,
    VALUE_SAPERATOR;
	
	public static TDNControlCharacter fromInt(int i)
	{
		return TDNControlCharacter.values()[i];
	}
	
	public static int toInt(TDNControlCharacter TDNCtrlChar)
	{
		return TDNCtrlChar.toInt();
	}
	
	public int toInt()
	{
		return ArrayUtils.indexOf(TDNControlCharacter.values(), this);
	}
}
