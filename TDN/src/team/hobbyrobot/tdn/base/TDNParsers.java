package team.hobbyrobot.tdn.base;

import team.hobbyrobot.tdn.core.TDNRootParser;
import team.hobbyrobot.tdn.core.TDNTypeParser;

public final class TDNParsers
{
	public static final TDNTypeParser ARRAY = new ArrayParser();
	public static final TDNTypeParser BOOLEAN = new BooleanParser();
	public static final TDNTypeParser FLOAT = new FloatParser();
	public static final TDNTypeParser INTEGER = new IntegerParser();
	public static final TDNTypeParser STRING = new StringParser();
	public static final TDNTypeParser ROOT = new TDNRootParser();
}
