package com.littlekt.graphics

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.math.geom.tan

/**
 * @author Colton Daily
 * @date 1/22/2025
 */
data class Frustum(private val planes: List<Plane> = emptyList()) {

    data class Plane(val normal: MutableVec3f, var d: Float) {
        fun distanceToPoint(point: Vec3f): Float {
            return normal.x * point.x + normal.y * point.y + normal.z * point.z + d
        }
    }

    private val nearCenter = MutableVec3f()
    private val farCenter = MutableVec3f()

    private val nearTopLeft = MutableVec3f()
    private val nearTopRight = MutableVec3f()
    private val nearBottomLeft = MutableVec3f()
    private val nearBottomRight = MutableVec3f()

    private val farTopLeft = MutableVec3f()
    private val farTopRight = MutableVec3f()
    private val farBottomLeft = MutableVec3f()
    private val farBottomRight = MutableVec3f()
    private val temp = MutableVec3f()

    fun updateFrustum(
        position: Vec3f,
        forward: Vec3f,
        up: Vec3f,
        right: Vec3f,
        fov: Float,
        aspectRatio: Float,
        near: Float,
        far: Float,
    ) {
        val tanFov = tan((fov / 2).degrees)
        val nearHeight = near * tanFov
        val nearWidth = nearHeight * aspectRatio
        val farHeight = far * tanFov
        val farWidth = farHeight * aspectRatio

        // Update near and far centers
        nearCenter.set(forward).scale(near).add(position)
        farCenter.set(forward).scale(far).add(position)

        // Update near plane corners
        nearTopLeft
            .set(nearCenter)
            .add(temp.set(up).scale(nearHeight))
            .subtract(temp.set(right).scale(nearWidth))
        nearTopRight
            .set(nearCenter)
            .add(temp.set(up).scale(nearHeight))
            .add(temp.set(right).scale(nearWidth))
        nearBottomLeft
            .set(nearCenter)
            .subtract(temp.set(up).scale(nearHeight))
            .subtract(temp.set(right).scale(nearWidth))
        nearBottomRight
            .set(nearCenter)
            .subtract(temp.set(up).scale(nearHeight))
            .add(temp.set(right).scale(nearWidth))

        // Update far plane corners
        farTopLeft
            .set(farCenter)
            .add(temp.set(up).scale(farHeight))
            .subtract(temp.set(right).scale(farWidth))
        farTopRight
            .set(farCenter)
            .add(temp.set(up).scale(farHeight))
            .add(temp.set(right).scale(farWidth))
        farBottomLeft
            .set(farCenter)
            .subtract(temp.set(up).scale(farHeight))
            .subtract(temp.set(right).scale(farWidth))
        farBottomRight
            .set(farCenter)
            .subtract(temp.set(up).scale(farHeight))
            .add(temp.set(right).scale(farWidth))

        updatePlanes()
    }

    fun isBoundsInside(min: Vec3f, max: Vec3f): Boolean {
        for (plane in planes) {
            val positiveVertex =
                Vec3f(
                    if (plane.normal.x >= 0) max.x else min.x,
                    if (plane.normal.y >= 0) max.y else min.y,
                    if (plane.normal.z >= 0) max.z else min.z,
                )

            if (plane.normal.dot(positiveVertex) + plane.d < 0) {
                return false
            }
        }
        return true
    }

    fun isSphereInside(center: Vec3f, radius: Float): Boolean {
        for (plane in planes) {
            val distance = plane.normal.dot(center) + plane.d
            if (distance < -radius) {
                // complete outside
                return false
            }
        }
        // intersecting or fully inside
        return true
    }

    private fun updatePlanes() {
        if(planes.isEmpty()) return
        // Near plane
        planes[0]
            .normal
            .set(nearTopRight)
            .subtract(nearTopLeft)
            .cross(temp.set(nearBottomLeft).subtract(nearTopLeft))
            .norm()
        planes[0].d = -planes[0].normal.dot(nearTopLeft)

        // Far plane
        planes[1]
            .normal
            .set(farBottomLeft)
            .subtract(farTopLeft)
            .cross(temp.set(farTopRight).subtract(farTopLeft))
            .norm()
        planes[1].d = -planes[1].normal.dot(farTopLeft)

        // Left plane
        planes[2]
            .normal
            .set(nearBottomLeft)
            .subtract(nearTopLeft)
            .cross(temp.set(farTopLeft).subtract(nearTopLeft))
            .norm()
        planes[2].d = -planes[2].normal.dot(nearTopLeft)

        // Right plane
        planes[3]
            .normal
            .set(farTopRight)
            .subtract(nearTopRight)
            .cross(temp.set(nearBottomRight).subtract(nearTopRight))
            .norm()
        planes[3].d = -planes[3].normal.dot(nearTopRight)

        // Top plane
        planes[4]
            .normal
            .set(farTopLeft)
            .subtract(nearTopLeft)
            .cross(temp.set(nearTopRight).subtract(nearTopLeft))
            .norm()
        planes[4].d = -planes[4].normal.dot(nearTopLeft)

        // Bottom plane
        planes[5]
            .normal
            .set(nearBottomRight)
            .subtract(nearBottomLeft)
            .cross(temp.set(farBottomLeft).subtract(nearBottomLeft))
            .norm()
        planes[5].d = -planes[5].normal.dot(nearBottomLeft)
    }
}
