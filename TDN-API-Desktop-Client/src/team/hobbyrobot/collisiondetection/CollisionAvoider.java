package team.hobbyrobot.collisiondetection;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.LinkedList;

import lejos.robotics.geometry.Line;
import lejos.robotics.geometry.Point;
import lejos.robotics.mapping.LineMap;
import lejos.robotics.navigation.DestinationUnreachableException;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;

public class CollisionAvoider
{
	private Dimension _size;

	private LineMap _map;

	private DijkstraPathFinder _pathFinder;

	public CollisionAvoider(Dimension size)
	{
		_size = size;
		_map = new LineMap(new Line[] {}, new lejos.robotics.geometry.Rectangle(0, 0, _size.width, _size.height));
		_pathFinder = new DijkstraPathFinder(_map);
	}

	//TODO Kdyz robot ma spoustu casu, ignoruj konecnou pozici jiz jedouciho robota
	public Path getPath(Graphics2D g, Vector A, Vector B, Vector C, Vector D, float headingStart, Float headingEnd,
		double robotSize, double deltaA) throws DestinationUnreachableException
	{
		// Calculate colliders, which represent robots destination positions
		PathCollider f1 = new PathCollider(B, B, robotSize * 1f);
		PathCollider f2 = new PathCollider(D, D, robotSize * 1f);

		g.setColor(Color.blue);
		f1.draw(g);
		f2.draw(g);

		// If destinationts overlap -> Invalid destination
		if (f1.intersects(f2))
			throw new DestinationUnreachableException();

		//PathCollider a = new PathCollider(A, B, robotSize);
		// Calculate collider of the second robots path
		PathCollider c = new PathCollider(C, D, robotSize);

		//g.setColor(Color.red);
		//a.draw(g);
		g.setColor(Color.green);
		c.draw(g);

		// Calculate the point, at which the robots could collide
		Vector E = getCollisionPoint(A, B, C, deltaA);

		Vector EB = null;
		if (E != null)
			EB = B.minus(E);

		//if(EB != null && EB.dot(EB) < safeDistance * safeDistance)

		// Create lines, which represent a no-go zone for the second robot. 
		// That's destination of the first robot with some padding.
		ArrayList<Line> obstructions = new ArrayList<Line>();
		// @formatter:off
				obstructions.add(new Line(
								(float)(B.cartesian(0) + robotSize*2), (float)(B.cartesian(1) + robotSize*2),
								(float)(B.cartesian(0) + robotSize*2), (float)(B.cartesian(1) - robotSize*2)));
				obstructions.add(new Line(
								(float)(B.cartesian(0) + robotSize*2), (float)(B.cartesian(1) - robotSize*2),
								(float)(B.cartesian(0) - robotSize*2), (float)(B.cartesian(1) - robotSize*2)));
				obstructions.add(new Line(
								(float)(B.cartesian(0) - robotSize*2), (float)(B.cartesian(1) - robotSize*2),
								(float)(B.cartesian(0) - robotSize*2), (float)(B.cartesian(1) + robotSize*2)));
				obstructions.add(new Line(
								(float)(B.cartesian(0) - robotSize*2), (float)(B.cartesian(1) + robotSize*2),
								(float)(B.cartesian(0) + robotSize*2), (float)(B.cartesian(1) + robotSize*2)));
				// @formatter:on

		// Draw the no-go zone
		g.setColor(new Color(200, 200, 255));
		for (Line l : obstructions)
		{
			g.drawLine((int) l.x1, (int) l.y1, (int) l.x2, (int) l.y2);
		}
		// Update path finder with the current no-go zone
		_pathFinder.setMap(obstructions);
		_pathFinder.lengthenLines(1);

		// Convert Vectors to starting Pose and destination Waypoint
		Pose start = new Pose((float) C.cartesian(0), (float) C.cartesian(1), headingStart);
		Waypoint end = new Waypoint((float) D.cartesian(0), (float) D.cartesian(1));
		if (headingEnd != null)
			end = new Waypoint((float) D.cartesian(0), (float) D.cartesian(1), headingEnd);
		// Calculate shortest path to the destination with consideration of the
		// no-go zone
		Path path = _pathFinder.findRoute(start, end);

		// If no potential collision point was found, return the found path without
		// limmiting its movement
		// TODO Move the no-go zone around the destination of the first robot here.
		// 		add checking wheter the second robot has enaugh time to go through
		//		the no-go zone without colliding with the first robot
		if (E == null)
			return path;

		// Calculate a collider for an area, where there is possibility of the two
		// robots colliding
		PathCollider b = new PathCollider(E, B.minus(B.minus(A).direction().scale(robotSize)), robotSize);
		g.setColor(Color.orange);
		b.draw(g);

		// If the first robot doesn't go through the collision area, return the 
		// found path without limmiting its movement
		if (!c.intersects(b))
			return path;

		double boundingBoxPadding = robotSize * Math.sqrt(2);
		LinkedList<Line> boundingBox = new LinkedList<Line>();

		// TODO Remove lengthening fo the horizontal lines on the right
		// Calculate the bounding box of the area with possible collision:
		// Iterate through the collider lines, excpet the vertical right one - 
		// messes with robot when going around the seconds robot destination
		//  => Robot doesn't need limmiting there, the first robot won't go any further
		for (int j = 0; j < b.lines.length - 1; j++)
		{
			Line colliderBase = b.lines[j];

			// Calculate the delta between two points of the line
			Vector vecCollider = new Vector(colliderBase.x2 - colliderBase.x1, colliderBase.y2 - colliderBase.y1);

			Vector dirCollider = vecCollider.direction();

			// Rotate the direction by 90 degrees, away from the center of the collider
			Vector rotatedDir = new Vector(-dirCollider.cartesian(1) * boundingBoxPadding,
				dirCollider.cartesian(0) * boundingBoxPadding);

			// Calculate points of the collider line and lengthen them, so the the
			// bounding box is connected
			Vector colliderA = new Vector(colliderBase.x1 + rotatedDir.cartesian(0),
				colliderBase.y1 + rotatedDir.cartesian(1));
			Vector colliderB = colliderA.plus(vecCollider).plus(dirCollider.scale(boundingBoxPadding));
			colliderA = colliderA.minus(dirCollider.scale(boundingBoxPadding));

			// Convert the calculated points to Line
			Line collider = new Line((float) colliderA.cartesian(0), (float) colliderA.cartesian(1),
				(float) colliderB.cartesian(0), (float) colliderB.cartesian(1));

			boundingBox.add(collider);
		}

		// List which will contain points which intersect the bounding box
		LinkedList<Point> collisionPoints = new LinkedList<Point>();
		// Index of the waypoint, from which the movement should be limmited
		int collisionStartIdx = -1;

		g.setColor(Color.magenta);
		for (int i = 0; i < path.size() - 1; i++)
		{
			Waypoint current = path.get(i);
			Waypoint next = path.get(i + 1);
			Line route = new Line(current.x, current.y, next.x, next.y);

			for (Line collider : boundingBox)
			{
				// Calculate the intersection of the current route with the
				// bounding boxes line
				Point intersection = collider.intersectsAt(route);

				// If intersection was found
				if (intersection != null)
				{
					// Calculate the angle, which determines if the route 
					// comes from the inside of the bounding box
					double base = Math.atan2(collider.x2 - collider.x1, -(collider.y2 - collider.y1));

					// Calculate the angle of the current route
					double routeAng = Math.atan2(next.y - current.y, next.x - current.x);

					// Calculate angle between the two control angles
					double delta = routeAng - base;
					// Normalize the angle
					if (delta > Math.PI)
						delta -= 2 * Math.PI;
					if (delta <= -Math.PI)
						delta += 2 * Math.PI;

					// If path goes from the inside of the bounding box -> skip this limmiting
					if (Math.abs(delta) < Math.PI / 2)
						continue;

					collisionPoints.add(intersection);
					g.drawLine((int) collider.x1, (int) collider.y1, (int) collider.x2, (int) collider.y2);
					drawCross(g, (int) intersection.x, (int) intersection.y);
				}
			}

			if (!collisionPoints.isEmpty())
			{
				collisionStartIdx = i;
				break;
			}
		}

		if (collisionStartIdx < 0)
			return path;

		double limit = Double.POSITIVE_INFINITY;
		Point limitStartPos = path.get(collisionStartIdx);
		for (Point p : collisionPoints)
		{
			double dist = limitStartPos.distance(p);
			if (dist < limit)
				limit = dist;
		}

		LimmitedPath limmitedPath = new LimmitedPath();
		limmitedPath.addAll(path);
		limmitedPath.limmitedStartWaypointIndex = collisionStartIdx;
		limmitedPath.travelLimit = limit;

		return limmitedPath;
	}

	private Vector getCollisionPoint(Vector A, Vector B, Vector C, double deltaA)
	{
		Vector AB = B.minus(A);
		double distAB = AB.magnitude();
		Vector dirAB = AB.scale(1f / distAB);

		Vector A2 = A.plus(dirAB.scale(deltaA));
		Vector A2B = B.minus(A2);
		Vector A2C = C.minus(A2);
		double distA2B = distAB - deltaA;
		double distA2C = A2C.magnitude();

		double cosTheta = A2B.dot(A2C) / (distA2B * distA2C);

		Vector A2E = dirAB.scale(distA2C / (2 * cosTheta));
		//double distA2E = A2E.magnitude();
		Vector E = A2.plus(A2E);

		Vector AE = E.minus(A);
		Vector BE = E.minus(B);

		double ABsq = AB.dot(AB);

		double AEsq = AE.dot(AE);

		double BEsq = BE.dot(BE);

		if (AEsq > ABsq)
			return null;

		if (BEsq > ABsq)
			return A;

		return E;
	}

	private void drawCross(Graphics2D g, int x, int y)
	{
		g.drawLine(x - 5, y - 5, x + 5, y + 5);
		g.drawLine(x - 5, y + 5, x + 5, y - 5);
	}
}
