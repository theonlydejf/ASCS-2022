package team.hobbyrobot.net.api.registerer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class RegisteredEndpoint implements Closeable
{
	public Socket socket;
	public BufferedReader reader;
	public BufferedWriter writer;
	public InputStream inputStream;
	public OutputStream outputStream;

	public RegisteredEndpoint(Socket client) throws IOException
	{
		this.socket = client;
		inputStream = client.getInputStream();
		outputStream = client.getOutputStream();
		reader = new BufferedReader(new InputStreamReader(inputStream));
		writer = new BufferedWriter(new OutputStreamWriter(outputStream));
	}

	@Override
	public void close() throws IOException
	{
		socket.close();
	}
}
