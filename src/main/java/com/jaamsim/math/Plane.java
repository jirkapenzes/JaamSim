/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.math;

public class Plane {

	public static Plane XY_PLANE = new Plane();

	/**
	 * The normal direction of the plane, should always have w = 0 and be of unit length
	 */
private Vector4d _normal;
/**
 * The shortest distance from the plane to the origin, by normal direction (affects sign)
 */
private double _dist;

/**
 * Create a plane defined by a normal, and a closest distance to the origin
 * This is the storage format and similar to the common mathematical definition for a plane
 * @param normal
 * @param distance
 */
public Plane(Vector4d normal, double distance) {
	_normal = new Vector4d(normal);
	_normal.normalizeLocal3();
	_normal.data[3] = 0;

	_dist = distance;

}

/**
 * By default return the XY plane
 */
public Plane() {
	_normal = new Vector4d(0, 0, 1, 0);
	_dist = 0;
}

/**
 * Create a plane defined by 3 points
 * @param p0
 * @param p1
 * @param p2
 */
public Plane(Vector4d p0, Vector4d p1, Vector4d p2) {
	_normal = new Vector4d();
	Vector4d v0 = new Vector4d(p1);
	v0.subLocal3(p0);
	v0.normalizeLocal3();

	Vector4d v1 = new Vector4d(p2);
	v1.subLocal3(p1);
	v1.normalizeLocal3();

	v0.cross(v1, _normal);
	_normal.normalizeLocal3();
	_normal.data[3] = 0;
	_dist = _normal.dot3(p0);
}

public void getNormal(Vector4d norm) {
	norm.copyFrom(_normal);
}

public Vector4d getNormalRef() {
	return _normal;
}

public double getDist() {
	return _dist;
}

/**
 * Get the shortest distance from the plane to this point, effectively just a convenient dot product
 * @param point
 * @return
 */
public double getNormalDist(Vector4d point) {
	double dot = point.dot3(_normal);
	return dot - _dist;
}

/**
 * Transform this plane by the coordinate transform 't'. Safe for self assignment
 * @param t - the Transform
 * @param out - the output
 */
public void transform(Transform t, Plane out) {

	Vector4d closePoint = new Vector4d(); // The point closest to the origin (need any point on the plane
	_normal.scale3(_dist, closePoint);

	t.apply(_normal, out._normal);
	out._normal.normalizeLocal3();

	// Now close point is the transformed point
	t.apply(closePoint, closePoint);

	out._dist = out._normal.dot3(closePoint);

}

public boolean equals(Plane p) {
	return _normal.equals(p._normal) && MathUtils.near(_dist, p._dist);
}

/**
 * Get the distance along a ray that it collides with this plane, this can return
 * infinity if the ray is parallel
 * @param r
 * @return
 */
public double collisionDist(Ray r) {

	// cos = plane-Normal dot ray-direction
	double cos = -1 * _normal.dot3(r.getDirRef());

	if (MathUtils.near(cos, 0.0)) {
		// The ray is nearly parallel to the plane, so no collision
		return Double.POSITIVE_INFINITY;
	}

	return ( _normal.dot3(r.getStartRef()) - _dist ) / cos;

}

/**
 * Returns if ray 'r' collides with the back of the plane
 * @param r
 * @return
 */
public boolean backFaceCollision(Ray r) {

	return _normal.dot3(r.getDirRef()) > 0;
}

} // class Plane