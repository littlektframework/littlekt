package com.littlekt.graphics

import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees

/** A [Camera] that uses perspective for the [projection] matrix. */
open class PerspectiveCamera(virtualWidth: Float = 0f, virtualHeight: Float = 0f) : Camera() {
    constructor(
        virtualWidth: Int,
        virtualHeight: Int,
    ) : this(virtualWidth.toFloat(), virtualHeight.toFloat())

    override val direction: MutableVec3f = MutableVec3f(Vec3f.NEG_Z_AXIS)
    override val up: MutableVec3f = MutableVec3f(Vec3f.Y_AXIS)

    private val tempCenter = MutableVec3f()
    private val tempMin = MutableVec3f()
    private val tempMax = MutableVec3f()
    private val frustum = Frustum()

    init {
        this.virtualWidth = virtualWidth
        this.virtualHeight = virtualHeight
        near = 0.01f
        far = 1000f
        fov = 60.degrees
    }

    override fun update() {
        super.update()
        updateFrustum()
    }

    /** Updates the [frustum] planes. */
    open fun updateFrustum() {
        frustum.updateFrustum(viewProjection)
    }

    override fun updateProjectionMatrix() {
        projection.setToPerspective(fov, aspectRatio, near, far)
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
        return frustum.isSphereInside(tempCenter.set(cx, cy, cz), radius)
    }
}
