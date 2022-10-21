package team.hobbyrobot.net.api.desktop;

import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Map.Entry;

import team.hobbyrobot.graphics.PaintPanel;
import team.hobbyrobot.graphics.Paintable;
import team.hobbyrobot.tdn.base.TDNArray;
import team.hobbyrobot.tdn.base.TDNParsers;
import team.hobbyrobot.tdn.core.TDNRoot;
import team.hobbyrobot.tdn.core.TDNValue;

public class TDNViewer extends PaintPanel
{
	private TDNRoot _source;
	private Font _font = new Font(Font.MONOSPACED, Font.PLAIN, 10);
	
	public TDNViewer()
	{
		super();
		addLayer(new Graphics());
	}
	
	private static String entryToString(String key, TDNValue value)
	{
		return "(" + value.parser().typeKey() + ") " + key + "=" + String.valueOf(value.value);
	}
	
	private class Graphics implements Paintable
	{		
		public Graphics()
		{

		}
		
		@Override
		public void paint(Graphics2D g)
		{
			int w = TDNViewer.this.getWidth();
			int h = TDNViewer.this.getHeight();
			
			TDNRoot root = new TDNRoot()
				.insertValue("cislo", new TDNValue(420, TDNParsers.INTEGER))
				.insertValue("neceleCislo", new TDNValue(6.9f, TDNParsers.FLOAT))
				.insertValue("pole", new TDNValue(new TDNArray(new Object[] { "ahoj1", "ahoj2", "ahoj3" }, TDNParsers.STRING), TDNParsers.ARRAY));
			
			g.translate(-8, 0);
			
			paintTDNValue(g, "MAIN", new TDNValue(root, TDNParsers.ROOT));
			
			System.out.println("nakresleno");
		}
	}
	
	private void paintTDNValue(Graphics2D g, String key, TDNValue value)
	{	
		System.out.println(key + "  " + g.getTransform());
		
		g.setFont(_font);
		int height = g.getFontMetrics().getHeight();
		
		g.drawLine(5, 2, 5, height + 1);
		g.drawLine(5, (int)(height * .67f), 10, (int)(height * .67f));
		g.translate(0, height);
		g.translate(10, 0);

		if(value.parser().typeKey().equals(TDNParsers.ROOT.typeKey()))
		{
			paintTDNRoot(g, key, value.as());
		}
		else if(value.parser().typeKey().equals(TDNParsers.ARRAY.typeKey()))
		{
			paintTDNArray(g, key, value.as());
		}
		else
		{
			g.drawString(TDNViewer.entryToString(key, value), 0, 0);			
		}
		
		g.translate(-10, 0);
	}
	
	private void paintTDNRoot(Graphics2D g, String key, TDNRoot root)
	{
		g.setFont(_font);
		g.drawString(key + " {", 0, 0);
		
		for(Entry<String, TDNValue> entry : root)
		{
			paintTDNValue(g, entry.getKey(), entry.getValue());
		}
		
		g.translate(0, g.getFontMetrics(_font).getHeight());
		g.drawString("}", 0, 0);
	}
	
	public void paintTDNArray(Graphics2D g, String key, TDNArray array)
	{
		//TODO
		g.setFont(_font);

		g.drawString("(" + TDNParsers.ARRAY.typeKey() + ") " + key + " [", 0, 0);
		
		int i = 0;
		for(Object o : array)
		{
			paintTDNValue(g, "[" + i + "]", new TDNValue(o, array.itemParser));
			i++;
		}
		
		g.translate(0, g.getFontMetrics(_font).getHeight());

		g.drawString("]", 0, 0);
	}
}
