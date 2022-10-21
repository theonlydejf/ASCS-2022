package team.hobbyrobot.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.LinkedList;

import javax.swing.JPanel;

public class PaintPanel extends JPanel
{
	private LinkedList<Paintable> _paintables;
	
	public PaintPanel(Paintable... paintables)
	{
		_paintables = new LinkedList<>();
		
		for(Paintable p : paintables)
			_paintables.addLast(p);
	}
	
	public void addLayer(Paintable layer)
	{
		_paintables.addLast(layer);
	}
	
	public void removeLastLayer()
	{
		_paintables.removeLast();
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		
		for(Paintable p : _paintables)
			p.paint(g2);
	}
}
