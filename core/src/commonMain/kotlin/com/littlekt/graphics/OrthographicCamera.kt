package com.littlekt.graphics

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f

/** A [Camera] that uses orthographic for the [projection] matrix. */
open class OrthographicCamera(virtualWidth: Float = 0f, virtualHeight: Float = 0f) : Camera() {
    constructor(
        virtualWidth: Int,
        virtualHeight: Int,
    ) : this(virtualWidth.toFloat(), virtualHeight.toFloat())

    override val direction: MutableVec3f = MutableVec3f(Vec3f.NEG_Z_AXIS)
    override val up: MutableVec3f = MutableVec3f(Vec3f.Y_AXIS)

    private val tempCenter = MutableVec3f()

    init {
        this.virtualWidth = virtualWidth
        this.virtualHeight = virtualHeight
        near = 0f
        far = 1f
    }

    /** Set the virtual size and centers the cameras position and updates the matrices. */
    fun ortho(virtualWidth: Float, virtualHeight: Float) {
        this.virtualWidth = virtualWidth
        this.virtualHeight = virtualHeight
        position.set(zoom * virtualWidth * 0.5f, zoom * virtualHeight * 0.5f, 0f)
        update()
    }

    /** Set the virtual size and centers the cameras position and updates the matrices. */
    fun ortho(virtualWidth: Int, virtualHeight: Int) =
        ortho(virtualWidth.toFloat(), virtualHeight.toFloat())

    override fun updateProjectionMatrix() {
        val left = zoom * -virtualWidth / 2
        val right = zoom * (virtualWidth / 2)
        val bottom = zoom * -(virtualHeight / 2)
        val top = zoom * virtualHeight / 2

        if (near != far && left != right && top != bottom) {
            projection.setToOrthographic(left, right, bottom, top, near, far)
        }
    }

    override fun boundsInFrustum(
        px: Float,
        py: Float,
        pz: Float,
        width: Float,
        height: Float,
        length: Float,
    ): Boolean {
        tempCenter.set(px, py, pz)
        tempCenter.subtract(position)

        val x = tempCenter.dot(right)
        val halfWidth = virtualWidth * 0.5f + width
        if (x > halfWidth || x < -halfWidth) {
            // bounds is either left or right of frustum
            return false
        }

        val y = tempCenter.dot(up)
        val halfHeight = virtualHeight * 0.5f + height
        if (y > halfHeight || y < -halfHeight) {
            // bounds is either above or below frustum
            return false
        }

        val z = tempCenter.dot(direction)
        if (z > far + length || z < near - length) {
            // bounds is either in front of near or behind far plane
            return false
        }
        return true
    }

    override fun sphereInFrustum(cx: Float, cy: Float, cz: Float, radius: Float): Boolean {
        tempCenter.set(cx, cy, cz)
        tempCenter.subtract(position)

        val x = tempCenter.dot(right)
        val halfWidth = virtualWidth * 0.5f + radius
        if (x > halfWidth || x < -halfWidth) {
            // sphere is either left or right of frustum
            return false
        }

        val y = tempCenter.dot(up)
        val halfHeight = virtualHeight * 0.5f + radius
        if (y > halfHeight || y < -halfHeight) {
            // sphere is either above or below frustum
            return false
        }

        val z = tempCenter.dot(direction)
        if (z > far + radius || z < near - radius) {
            // sphere is either in front of near or behind far plane
            return false
        }
        return true
    }
}
