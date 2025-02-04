package com.littlekt.graphics

import com.littlekt.math.Mat4
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f

/**
 * Represents a viewing frustum typically used for visibility determination and culling in 3D
 * graphics.
 *
 * The frustum is composed of six planes defining a truncated pyramid used to determine whether
 * objects are inside the viewable space. The class provides functionality to update the frustum
 * based on a view-projection matrix and test the inclusion of points, spheres, or bounding boxes.
 *
 * @property planes A list of six planes representing the frustum boundaries.
 * @constructor Initializes the frustum with six planes. Default planes have zeroed vectors and zero
 *   distance.
 * @author Colton Daily
 * @date 1/22/2025
 */
class Frustum(
    p1: Plane = Plane(),
    p2: Plane = Plane(),
    p3: Plane = Plane(),
    p4: Plane = Plane(),
    p5: Plane = Plane(),
    p6: Plane = Plane(),
) {
    private val planes: List<Plane> = listOf(p1, p2, p3, p4, p5, p6)

    /**
     * Represents a mathematical plane in three-dimensional space defined by a normal vector and a
     * distance from the origin.
     *
     * @property normal The normal vector of the plane, which determines its orientation.
     * @property d The signed distance of the plane from the origin in the direction of the normal
     *   vector.
     */
    data class Plane(val normal: MutableVec3f = MutableVec3f(), var d: Float = 0f) {
        /**
         * Sets the components of the plane's normal vector and the distance value, updating the
         * plane instance.
         *
         * @param x The x-component of the plane's normal vector.
         * @param y The y-component of the plane's normal vector.
         * @param z The z-component of the plane's normal vector.
         * @param d The signed distance of the plane from the origin along its normal.
         * @return The updated plane instance.
         */
        fun set(x: Float, y: Float, z: Float, d: Float): Plane {
            normal.set(x, y, z)
            this.d = d
            return this
        }

        /**
         * Normalizes the plane's normal vector to unit length and adjusts the plane's distance
         * value accordingly. This method modifies the plane in place.
         *
         * @return The normalized plane instance.
         */
        fun normalize(): Plane {
            val inverseNormalLength = 1f / this.normal.length()
            this.normal.scale(inverseNormalLength)
            this.d *= inverseNormalLength
            return this
        }

        /**
         * Calculates the signed distance from a plane to a given point in 3D space.
         *
         * @param point The point in 3D space for which the distance to the plane is calculated.
         * @return The signed distance from the point to the plane. A positive value indicates the
         *   point is in the direction of the plane's normal, while a negative value indicates the
         *   point is in the opposite direction.
         */
        fun distanceToPoint(point: Vec3f): Float {
            return normal.dot(point) + d
        }
    }

    /**
     * Updates the frustum planes using the provided view projection matrix.
     *
     * @param viewProjection The 4x4 view projection matrix used to calculate the frustum planes.
     */
    fun updateFrustum(viewProjection: Mat4) {
        val me0 = viewProjection[0]
        val me1 = viewProjection[1]
        val me2 = viewProjection[2]
        val me3 = viewProjection[3]
        val me4 = viewProjection[4]
        val me5 = viewProjection[5]
        val me6 = viewProjection[6]
        val me7 = viewProjection[7]
        val me8 = viewProjection[8]
        val me9 = viewProjection[9]
        val me10 = viewProjection[10]
        val me11 = viewProjection[11]
        val me12 = viewProjection[12]
        val me13 = viewProjection[13]
        val me14 = viewProjection[14]
        val me15 = viewProjection[15]

        planes[0].set(me3 - me0, me7 - me4, me11 - me8, me15 - me12).normalize()
        planes[1].set(me3 + me0, me7 + me4, me11 + me8, me15 + me12).normalize()
        planes[2].set(me3 + me1, me7 + me5, me11 + me9, me15 + me13).normalize()
        planes[3].set(me3 - me1, me7 - me5, me11 - me9, me15 - me13).normalize()
        planes[4].set(me3 - me2, me7 - me6, me11 - me10, me15 - me14).normalize()
        planes[5].set(me2, me6, me10, me14).normalize()
    }

    /**
     * Determines if a bounding box, defined by the given minimum and maximum points, lies within
     * the frustum.
     *
     * @param min the minimum corner of the bounding box as a 3D vector (Vec3f).
     * @param max the maximum corner of the bounding box as a 3D vector (Vec3f).
     * @return true if the bounding box is inside the frustum, false otherwise.
     */
    fun isBoundsInside(min: Vec3f, max: Vec3f): Boolean {
        for (plane in planes) {
            val positiveVertex =
                Vec3f(
                    if (plane.normal.x >= 0) max.x else min.x,
                    if (plane.normal.y >= 0) max.y else min.y,
                    if (plane.normal.z >= 0) max.z else min.z,
                )

            if (plane.distanceToPoint(positiveVertex) < 0) {
                return false
            }
        }
        return true
    }

    /**
     * Determines if a sphere defined by its center and radius is inside the frustum.
     *
     * @param center The center of the sphere as a 3D vector (Vec3f).
     * @param radius The radius of the sphere as a floating-point value.
     * @return true if the sphere is entirely inside the frustum, false otherwise.
     */
    fun isSphereInside(center: Vec3f, radius: Float): Boolean {
        for (plane in planes) {
            val distance = plane.distanceToPoint(center)
            if (distance < -radius) {
                return false
            }
        }
        return true
    }

    /**
     * Determines if a point is inside the frustum.
     *
     * @param point The 3D vector (Vec3f) representing the point to be checked.
     * @return true if the point is inside the frustum, false otherwise.
     */
    fun isPointInside(point: Vec3f): Boolean {
        for (plane in planes) {
            if (plane.distanceToPoint(point) < 0f) {
                return false
            }
        }
        return true
    }
}
