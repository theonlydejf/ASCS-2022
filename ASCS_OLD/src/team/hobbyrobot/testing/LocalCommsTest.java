package team.hobbyrobot.testing;

import java.io.IOException;

import team.hobbyrobot.ascsvehicle.communicator.*;
import team.hobbyrobot.tdn.core.TDNRoot;

public class LocalCommsTest implements TDNReceiverListener
{
	public static void main(String[] args) throws IOException
	{
		TDNReceiver receiver = new TDNReceiver(1234);
		receiver.addListener(new LocalCommsTest());
		receiver.start();
		System.out.println("receiving..");
		System.in.read();
	}

	@Override
	public void rootReceived(TDNRoot root)
	{
		TDNTesting.printRoot(root);
	}
}
