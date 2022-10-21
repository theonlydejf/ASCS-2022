package team.hobbyrobot.python;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class PythonInterpreter
{
	private String _pythonPath;

	public PythonInterpreter(String pythonPath)
	{
		_pythonPath = pythonPath;
	}

	public Process startScript(String scriptPath, String... args) throws IOException
	{
		String[] cmd = new String[args.length + 2];
		cmd[0] = _pythonPath;
		cmd[1] = scriptPath;
		if(args.length > 0)
			System.arraycopy(args, 0, cmd, 2, args.length);
		
		ProcessBuilder pb = new ProcessBuilder(cmd);
		//pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		pb.redirectError(ProcessBuilder.Redirect.INHERIT);
		//pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
		Process p = pb.start();
		final Thread ioThread = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					final InputStreamReader reader = new InputStreamReader(p.getInputStream());
					String line = null;
					while (p.isAlive())
					{
						if(reader.ready())
							System.out.print((char)reader.read());
					}
					reader.close();
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		};
		ioThread.start();

		return p;
	}
}
