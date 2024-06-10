package com.littlekt.graphics

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.cosine
import com.littlekt.math.geom.degrees
import com.littlekt.math.geom.radians
import com.littlekt.math.geom.tangent
import kotlin.math.atan

/** A [Camera] that uses perspective for the [projection] matrix. */
open class PerspectiveCamera(virtualWidth: Float = 0f, virtualHeight: Float = 0f) : Camera() {
    constructor(
        virtualWidth: Int,
        virtualHeight: Int
    ) : this(virtualWidth.toFloat(), virtualHeight.toFloat())

    override val direction: MutableVec3f = MutableVec3f(Vec3f.NEG_Z_AXIS)
    override val up: MutableVec3f = MutableVec3f(Vec3f.Y_AXIS)
    var fovX = 0f
        private set

    private var sphereFacX = 1f
    private var sphereFacY = 1f
    private var tangX = 1f
    private var tangY = 1f

    private val tempCenter = MutableVec3f()

    init {
        this.virtualWidth = virtualWidth
        this.virtualHeight = virtualHeight
        near = 0.1f
        fov = 60f
    }

    override fun updateProjectionMatrix() {
        projection.setToPerspective(fov, aspectRatio, near, far)

        val angY = fov.degrees / 2f
        sphereFacX = 1f / angY.cosine
        tangY = angY.tangent

        val angX = atan(tangY * aspectRatio).radians
        sphereFacX = 1f / angX.cosine
        tangX = angX.tangent
        fovX = (angX * 2).degrees
    }

    override fun boundsInFrustum(
        px: Float,
        py: Float,
        pz: Float,
        width: Float,
        height: Float,
        length: Float
    ): Boolean {
        // TODO
        return true
    }

    override fun sphereInFrustum(cx: Float, cy: Float, cz: Float, radius: Float): Boolean {
        tempCenter.set(cx, cy, cz)
        tempCenter.subtract(position)

        var z = tempCenter.dot(direction)
        if (z > far + radius || z < near - radius) {
            // sphere is either front or behind of frustum
            return false
        }

        val y = tempCenter.dot(up)
        var d = radius * sphereFacY
        z *= tangY
        if (y > z + d || y < -z - d) {
            // sphere is either above or below of frustum
            return false
        }

        val x = tempCenter.dot(rightDir)
        d = radius * sphereFacX
        z *= aspectRatio
        if (x > z + d || x < -z - d) {
            // sphere is either left or right of frustum
            return false
        }

        return true
    }
}
