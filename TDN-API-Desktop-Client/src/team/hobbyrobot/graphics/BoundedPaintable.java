package team.hobbyrobot.graphics;

import java.awt.Graphics2D;

public class BoundedPaintable implements Paintable
{
	private Paintable[] bases;
	private int x, y, width, height;
	
	public BoundedPaintable(int x, int y, int width, int height, Paintable... bases)
	{
		super();
		this.bases = bases;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	@Override
	public void paint(Graphics2D g)
	{
		for(Paintable p : bases)
			p.paint((Graphics2D)g.create(x, y, width, height));
	}
	
}
