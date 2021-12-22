package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.viewport.Viewport
import com.lehaine.littlekt.math.*
import com.lehaine.littlekt.util.LazyMat4

/**
 * @author Colton Daily
 * @date 11/27/2021
 */
abstract class Camera {
    val position = MutableVec3f(Vec3f.Z_AXIS)
    val direction = MutableVec3f(Vec3f.NEG_Z_AXIS)
    val up = MutableVec3f(Vec3f.Y_AXIS)
    val rightDir: Vec3f get() = rightMut

    protected val rightMut = MutableVec3f()

    val projection = Mat4()
    val view = Mat4()

    private val lazyInvProjection = LazyMat4 { projection.invert(it) }
    val invProj: Mat4 get() = lazyInvProjection.get()

    private val lazyInvView = LazyMat4 { view.invert(it) }
    val invView: Mat4 get() = lazyInvView.get()

    private val lazyViewProjection = LazyMat4 { projection.mul(view, it) }
    val viewProjection: Mat4 get() = lazyViewProjection.get()

    private val lazyInvViewProjection = LazyMat4 { viewProjection.invert(it) }
    val invViewProjection: Mat4 get() = lazyInvViewProjection.get()

    /**
     * The near clipping plane distance
     */
    var near = 1f

    /**
     * The far clipping plane distance
     */
    var far = 100f

    var fov = 67f

    var viewport = Viewport()

    var virtualWidth by viewport::virtualWidth

    var virtualheight by viewport::virtualHeight

    var viewportX by viewport::x

    var viewportY by viewport::y

    /**
     * The viewport width
     */
    var viewportWidth by viewport::width

    /**
     * The viewport height
     */
    var viewportHeight by viewport::height


    /**
     * The zoom value should be between -1 and 1. This value is then translated to be from [minimumZoom] to [maximumZoom].
     * This lets you set appropriate minimum/maximum values then use a more intuitive -1 to 1 mapping to change the zoom.
     */
    var zoom: Float
        get() = _zoom
        set(value) {
            zoom(value)
        }

    /**
     * Minimum non-scaled value (0 - [Float.MAX_VALUE] that the camera zoom can be. Defaults to 0.3f.
     */
    var minimumZoom: Float
        get() = _minimumZoom
        set(value) {
            minimumZoom(value)
        }

    /**
     * Maximum non-scaled value (0 - [Float.MAX_VALUE] that the camera zoom can be. Defaults to 3f.
     */
    var maximumZoom: Float
        get() = _maximumZoom
        set(value) {
            maximumZoom(value)
        }
    private var _minimumZoom = 0.3f
    private var _maximumZoom = 3f
    private var _zoom = 1f

    private val tempVec3 = MutableVec3f()
    private val tempVec4 = MutableVec4f()
    private val ray = Ray()

    open fun update() {
        updateViewMatrix()
        updateProjectionMatrix()

        lazyInvProjection.isDirty = true
        lazyViewProjection.isDirty = true
        lazyInvViewProjection.isDirty = true
    }

    protected open fun updateViewMatrix() {
        direction.cross(up, rightMut).norm()
        view.setLookAt(position, tempVec3.set(position).add(direction), up)
        lazyInvView.isDirty = true
    }

    protected abstract fun updateProjectionMatrix()

    fun lookAt(x: Float, y: Float, z: Float) {
        tempVec3.set(x, y, z).subtract(position).norm()
        if (tempVec3 != Vec3f.ZERO) {
            val dot = tempVec3.dot(up)
            if ((dot - 1f).isFuzzyZero(0.000001f)) {
                up.set(direction).scale(-1f)
            } else if ((dot + 1f).isFuzzyZero(0.000001f)) {
                up.set(direction)
            }
            direction.set(tempVec3)
            normalizeUp()
        }
    }

    fun lookAt(target: Vec3f) = lookAt(target.x, target.y, target.z)

    fun rotate(angle: Float, axisX: Float, axisY: Float, axisZ: Float) {
        direction.rotate(angle, axisX, axisY, axisZ)
        up.rotate(angle, axisX, axisY, axisZ)
    }

    fun rotate(angle: Float, axis: Vec3f) {
        direction.rotate(angle, axis)
        up.rotate(angle, axis)
    }

    fun rotateAround(point: Vec3f, axis: Vec3f, angle: Float) {
        tempVec3.set(point).subtract(position)
        translate(tempVec3)
        rotate(angle, axis)
        tempVec3.rotate(angle, axis)
        translate(-tempVec3.x, -tempVec3.y, -tempVec3.z)
    }

    fun translate(x: Float, y: Float, z: Float) {
        position.add(x, y, z)
    }

    fun translate(offset: Vec3f) {
        position += offset
    }

    fun normalizeUp() {
        tempVec3.set(direction).cross(up, tempVec3)
        up.set(tempVec3).cross(direction, up).norm()
    }

    /**
     * Sets the zoom value should be between -1 and 1. This value is then translated to be from [minimumZoom] to [maximumZoom].
     * This lets you set appropriate minimum/maximum values then use a more intuitive -1 to 1 mapping to change the zoom.
     * @param value the new zoom
     * @return this camera
     */
    fun zoom(value: Float) {
        val newZoom = value.clamp(-1f, 1f)
        _zoom = when {
            newZoom == 0f -> {
                1f
            }
            newZoom < 0f -> {
                map(-1f, 0f, _minimumZoom, 1f, newZoom)
            }
            else -> {
                map(0f, 1f, 1f, _maximumZoom, newZoom)
            }
        }
    }

    /**
     * Sets the minimum non-scaled value (0 - [Float.MAX_VALUE] that the camera zoom can be. Defaults to 0.3f.
     * @param value the new minimum zoom
     * @return this camera
     */
    fun minimumZoom(value: Float) {
        check(value > 0f) { "Minimum zoom must be greater than zero!" }

        if (_zoom < value) {
            _zoom = value
        }

        _minimumZoom = value
    }


    /**
     * Sets the maximum non-scaled value (0 - [Float.MAX_VALUE] that the camera zoom can be. Defaults to 3f.
     * @param value the new maximum zoom
     * @return this camera
     */
    fun maximumZoom(value: Float) {
        check(value > 0f) { "Maximum zoom must be greater than zero!" }

        if (_zoom > value) {
            _zoom = value
        }

        _maximumZoom = value
    }

    abstract fun boundsInFrustum(point: Vec3f, size: Vec3f): Boolean
    abstract fun sphereInFrustum(center: Vec3f, radius: Float): Boolean

    fun project(world: Vec3f, result: MutableVec3f): Boolean {
        tempVec4.set(world.x, world.y, world.z, 1f)
        viewProjection.transform(tempVec4)
        if (tempVec4.w.isFuzzyZero()) {
            return false
        }
        result.set(tempVec4.x, tempVec4.y, tempVec4.z).scale(1f / tempVec4.w)
        return true
    }

    fun project(world: Vec3f, result: MutableVec4f): MutableVec4f =
        viewProjection.transform(result.set(world.x, world.y, world.z, 1f))

    fun projectScreen(world: Vec3f, context: Context, result: MutableVec3f): Boolean {
        if (!project(world, result)) {
            return false
        }
        result.x = (1 + result.x) * 0.5f * viewport.width + viewport.x
        result.y = context.graphics.height - (1 + result.y) * 0.5f * viewport.height + viewport.y
        result.z = (1 + result.z) * 0.5f

        return true
    }

    fun unProjectScreen(screen: Vec3f, context: Context, result: MutableVec3f): Boolean {
        val x = screen.x - viewport.x
        val y = (context.graphics.height - screen.y) - viewport.y

        tempVec4.set(2f * x / viewport.width - 1f, 2f * y / viewport.height - 1f, 2f * screen.z - 1f, 1f)
        invViewProjection.transform(tempVec4)
        val s = 1f / tempVec4.w
        result.set(tempVec4.x * s, tempVec4.y * s, tempVec4.z * s)
        return true
    }

    fun computePickRay(
        pickRay: Ray,
        screenX: Float,
        screenY: Float,
        context: Context
    ): Boolean {
        var valid = unProjectScreen(tempVec3.set(screenX, screenY, 0f), context, pickRay.origin)
        valid = valid && unProjectScreen(tempVec3.set(screenX, screenY, 1f), context, pickRay.direction)

        if (valid) {
            pickRay.direction.subtract(pickRay.origin)
            pickRay.direction.norm()
        }

        return valid
    }
}

open class OrthographicCamera(viewportWidth: Int = 0, viewportHeight: Int = 0) : Camera() {
    private val planes = List(6) { FrustumPlane() }

    private val tempCenter = MutableVec3f()

    init {
        this.viewportWidth = viewportWidth
        this.viewportHeight = viewportHeight
        near = 0f
    }

    override fun updateProjectionMatrix() {
        if (near != far) {
            projection.setOrthographic(
                zoom * -viewportWidth / 2, zoom * (viewportWidth / 2), zoom * -(viewportHeight / 2), zoom
                        * viewportHeight / 2, near, far
            )
        }
    }

    override fun boundsInFrustum(point: Vec3f, size: Vec3f): Boolean {
        // TODO impl
        return false
    }

    override fun sphereInFrustum(center: Vec3f, radius: Float): Boolean {
        tempCenter.set(center)
        tempCenter.subtract(position)

        val x = tempCenter.dot(rightDir)
        if (x > viewportWidth + radius || x < -radius) {
            // sphere is either left or right of frustum
            return false
        }

        val y = tempCenter.dot(up)
        if (y > viewportHeight + radius || y < -radius) {
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

class FrustumPlane {
    val upperLeft = MutableVec3f()
    val upperRight = MutableVec3f()
    val lowerLeft = MutableVec3f()
    val lowerRight = MutableVec3f()

    fun set(other: FrustumPlane) {
        upperLeft.set(other.upperLeft)
        upperRight.set(other.upperRight)
        lowerLeft.set(other.lowerLeft)
        lowerRight.set(other.lowerRight)
    }
}