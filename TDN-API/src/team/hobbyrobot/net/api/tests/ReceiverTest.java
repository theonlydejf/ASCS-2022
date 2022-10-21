package team.hobbyrobot.net.api.tests;

import java.io.IOException;
import java.util.Map.Entry;

import org.junit.Assert;

import org.junit.Test;

import team.hobbyrobot.logging.Logger;
import team.hobbyrobot.net.api.streaming.TDNReceiver;
import team.hobbyrobot.net.api.streaming.TDNReceiverListener;
import team.hobbyrobot.net.api.streaming.TDNSender;
import team.hobbyrobot.tdn.base.TDNArray;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNRootParser;
import team.hobbyrobot.tdn.core.TDNValue;

class ReceiverTest implements TDNReceiverListener
{
	private int _receivedCnt = 0;

	@Test
	public void test() throws IOException, InterruptedException
	{
		TDNReceiver receiver = new TDNReceiver(1111);
		receiver.addListener(this);
		receiver.start();
		
		TDNSender sender = new TDNSender("localhost", 1111);
		sender.connect();
		
		TDNRoot root = new TDNRoot();
		root.insertValue("hodnotaString", new TDNValue("tohle je text", TDNParsers.STRING));
		sender.send(root);
		
		Thread.sleep(1000);
		Assert.assertEquals("tohle je text", (String)receiver.getLastRoot().get("hodnotaString").value);
		Assert.assertEquals(1, _receivedCnt);
	}

	@Override
	public void rootReceived(TDNRoot root)
	{
		_receivedCnt++;
		printRoot(root);
	}
	
	public static void printRoot(TDNRoot root)
	{
		System.out.println("(");
		StringBuilder sb = new StringBuilder();
		for (Entry<String, TDNValue> val : root)
		{
			sb.append(val.getKey());
			sb.append(": ");
			if (val.getValue().value instanceof TDNRoot)
			{
				printRoot((TDNRoot) val.getValue().value);
				continue;
			}
			if (val.getValue().value instanceof TDNArray)
			{
				System.out.println(sb.toString() + "[");
				sb = new StringBuilder();
				TDNArray arr = (TDNArray) val.getValue().value;
				for (Object item : arr)
				{
					sb.append(",");
					if (arr.itemParser.typeKey().equals(new TDNRootParser().typeKey()))
						printRoot((TDNRoot) item);
					else
					{
						System.out.println(sb.toString() + item);
						sb = new StringBuilder();
					}
					continue;
				}
				System.out.println(sb.toString() + "]");
				sb = new StringBuilder();
				continue;
			}
			System.out.println(sb.toString() + val.getValue().value);
			sb = new StringBuilder();
		}
		System.out.println(sb.toString() + ")");
	}

}
