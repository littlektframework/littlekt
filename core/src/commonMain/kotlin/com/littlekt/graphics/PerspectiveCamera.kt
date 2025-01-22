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
        virtualHeight: Int,
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
    private val tempMin = MutableVec3f()
    private val tempMax = MutableVec3f()
    private val frustum = Frustum()

    init {
        this.virtualWidth = virtualWidth
        this.virtualHeight = virtualHeight
        near = 0.01f
        far = 1000f
        fov = 60f
    }

    override fun update() {
        super.update()
        updateFrustum()
    }

    /** Updates the [frustum] planes. */
    open fun updateFrustum() {
        frustum.updateFrustum(position, direction, up, right, fov, aspectRatio, near, far)
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
        length: Float,
    ): Boolean {
        tempMin.set(px, py, pz)
        tempMax.set(px + width, py + height, pz + length)
        return frustum.isBoundsInside(tempMin, tempMax)
    }

    override fun sphereInFrustum(cx: Float, cy: Float, cz: Float, radius: Float): Boolean {
        tempCenter.set(cx, cy, cz)
        return frustum.isSphereInside(tempCenter, radius)
    }
}
