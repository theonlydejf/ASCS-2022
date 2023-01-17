package team.hobbyrobot.collisiondetection;

import java.awt.Graphics2D;

import lejos.robotics.geometry.Line;

public class PathCollider
{
	public Vector A, B;
	public Vector delta;

	public Line[] lines;
	public Line[] linesCollider;

	public PathCollider(Vector A, Vector B, double height)
	{
		this.A = A;
		this.B = B;
		initLines(height);
	}

	public boolean intersects(PathCollider collider)
	{
		for (Line l2 : collider.lines)
			for (Line l1 : lines)
				if (l2.intersectsLine(l1))
					return true;

		return false;
	}

	public void draw(Graphics2D g)
	{
		for (int i = 0; i < lines.length; i++)
		{
			Line l = lines[i];
			g.drawLine((int) l.x1, (int) l.y1, (int) l.x2, (int) l.y2);
		}
	}

	private void initLines(double height)
	{
		delta = B.minus(A);
		Vector dir = new Vector(1, 0);
		try
		{
			dir = delta.direction();
		}
		catch (ArithmeticException e)
		{
		}
		Vector move = dir.scale(height);
		Vector perpMove = new Vector(move.cartesian(1), move.cartesian(0) * -1);

		Vector topLeft = A.minus(move).minus(perpMove);
		Vector bottomLeft = A.minus(move).plus(perpMove);
		Vector topRight = B.plus(move).minus(perpMove);
		Vector bottomRight = B.plus(move).plus(perpMove);

		lines = new Line[] { lineFromVectors(bottomRight, bottomLeft), lineFromVectors(bottomLeft, topLeft),
				lineFromVectors(topLeft, topRight), lineFromVectors(topRight, bottomRight) };
		linesCollider = new Line[] { lineFromVectors(bottomRight, bottomLeft), lineFromVectors(bottomLeft, topLeft),
				lineFromVectors(topLeft, topRight), lineFromVectors(topRight, bottomRight),
				lineFromVectors(topRight, bottomLeft), lineFromVectors(bottomRight, topLeft) };
	}

	private static Line lineFromVectors(Vector a, Vector b)
	{
		return new Line((float) a.cartesian(0), (float) a.cartesian(1), (float) b.cartesian(0), (float) b.cartesian(1));
	}

}