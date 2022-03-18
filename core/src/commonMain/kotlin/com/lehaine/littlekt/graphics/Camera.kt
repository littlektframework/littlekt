package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.math.*
import com.lehaine.littlekt.util.LazyMat4
import com.lehaine.littlekt.util.viewport.Viewport

/**
 * @author Colton Daily
 * @date 11/27/2021
 */
abstract class Camera {
    val id = nextCameraId++

    val position = MutableVec3f(0f)
    val direction = MutableVec3f(Vec3f.Z_AXIS)
    val up = MutableVec3f(Vec3f.NEG_Y_AXIS)
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
        set(value) {
            field = value
            //    position.set(viewport.virtualWidth / 2f, viewport.virtualHeight / 2f, 0f)
        }

    var virtualWidth: Int
        get() = viewport.virtualWidth
        set(value) {
            viewport.virtualWidth = value
        }

    var virtualHeight: Int
        get() = viewport.virtualHeight
        set(value) {
            viewport.virtualHeight = value
        }

    var screenX: Int
        get() = viewport.x
        set(value) {
            viewport.x = value
        }

    var screenY: Int
        get() = viewport.y
        set(value) {
            viewport.y = value
        }

    /**
     * The screen width
     */
    var screenWidth: Int
        get() = viewport.width
        set(value) {
            viewport.width = value
        }

    /**
     * The screen height
     */
    var screenHeight: Int
        get() = viewport.height
        set(value) {
            viewport.height = value
        }


    /**
     * The current zoom value.
     */
    var zoom: Float = 1f

    private val tempVec2 = MutableVec2f()
    private val tempVec3 = MutableVec3f()
    private val tempVec4 = MutableVec4f()

    open fun update() {
        updateViewMatrix()
        updateProjectionMatrix()

        lazyInvProjection.isDirty = true
        lazyViewProjection.isDirty = true
        lazyInvViewProjection.isDirty = true
    }

    fun update(width: Int, height: Int, context: Context) {
        viewport.update(width, height, context)
        update()
    }

    protected open fun updateViewMatrix() {
        direction.cross(up, rightMut).norm()
        view.setToLookAt(position, tempVec3.set(position).add(direction), up)
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

    abstract fun boundsInFrustum(point: Vec3f, size: Vec3f): Boolean
    abstract fun sphereInFrustum(center: Vec3f, radius: Float): Boolean

    fun project(world: Vec2f, result: MutableVec2f): Boolean {
        tempVec4.set(world.x, world.y, 1f, 1f)
        viewProjection.transform(tempVec4)
        if (tempVec4.w.isFuzzyZero()) {
            return false
        }
        result.set(tempVec4.x, tempVec4.y).scale(1f / tempVec4.w)
        return true
    }

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

    fun projectScreen(world: Vec2f, context: Context): MutableVec2f {
        val result = MutableVec2f()
        projectScreen(world, context, result)
        return result
    }

    fun projectScreen(x: Float, y: Float, context: Context): MutableVec2f {
        val result = MutableVec2f()
        projectScreen(tempVec2.set(x, y), context, result)
        return result
    }

    fun projectScreen(x: Float, y: Float, context: Context, result: MutableVec2f) =
        projectScreen(tempVec2.set(x, y), context, result)

    fun projectScreen(world: Vec2f, context: Context, result: MutableVec2f): Boolean {
        if (!project(world, result)) {
            return false
        }
        result.x = (1 + result.x) * 0.5f * viewport.width + viewport.x
        result.y = context.graphics.height - (1 + result.y) * 0.5f * viewport.height + viewport.y

        return true
    }

    fun projectScreen(world: Vec3f, context: Context): MutableVec3f {
        val result = MutableVec3f()
        projectScreen(world, context, result)
        return result
    }

    fun projectScreen(x: Float, y: Float, z: Float, context: Context): MutableVec3f {
        val result = MutableVec3f()
        projectScreen(tempVec3.set(x, y, z), context, result)
        return result
    }

    fun projectScreen(x: Float, y: Float, z: Float, context: Context, result: MutableVec3f) =
        projectScreen(tempVec3.set(x, y, z), context, result)


    fun projectScreen(world: Vec3f, context: Context, result: MutableVec3f): Boolean {
        if (!project(world, result)) {
            return false
        }
        result.x = (1 + result.x) * 0.5f * viewport.width + viewport.x
        result.y = context.graphics.height - (1 + result.y) * 0.5f * viewport.height + viewport.y
        result.z = (1 + result.z) * 0.5f

        return true
    }

    fun unProjectScreen(screen: Vec2f, context: Context): MutableVec2f {
        val result = MutableVec2f()
        unProjectScreen(screen, context, result)
        return result
    }

    fun unProjectScreen(x: Float, y: Float, context: Context): MutableVec2f {
        val result = MutableVec2f()
        unProjectScreen(tempVec2.set(x, y), context, result)
        return result
    }

    fun unProjectScreen(x: Float, y: Float, context: Context, result: MutableVec2f) =
        unProjectScreen(tempVec2.set(x, y), context, result)


    fun unProjectScreen(screen: Vec2f, context: Context, result: MutableVec2f): Boolean {
        val x = screen.x - viewport.x
        val y = (context.graphics.height - screen.y) - viewport.y

        tempVec4.set(2f * x / viewport.width - 1f, 2f * y / viewport.height - 1f, 1f, 1f)
        invViewProjection.transform(tempVec4)
        val s = 1f / tempVec4.w
        result.set(tempVec4.x * s, tempVec4.y * s)
        return true
    }

    fun unProjectScreen(screen: Vec3f, context: Context): MutableVec3f {
        val result = MutableVec3f()
        unProjectScreen(screen, context, result)
        return result
    }

    fun unProjectScreen(x: Float, y: Float, z: Float, context: Context): MutableVec3f {
        val result = MutableVec3f()
        unProjectScreen(tempVec3.set(x, y, z), context, result)
        return result
    }

    fun unProjectScreen(x: Float, y: Float, z: Float, context: Context, result: MutableVec3f) =
        unProjectScreen(tempVec3.set(x, y, z), context, result)


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
        context: Context,
    ): Boolean {
        var valid = unProjectScreen(tempVec3.set(screenX, screenY, 0f), context, pickRay.origin)
        valid = valid && unProjectScreen(tempVec3.set(screenX, screenY, 1f), context, pickRay.direction)

        if (valid) {
            pickRay.direction.subtract(pickRay.origin)
            pickRay.direction.norm()
        }

        return valid
    }

    override fun toString(): String {
        return "Camera(id=$id, position=$position, direction=$direction, up=$up, rightDir=$rightDir, projection=$projection, view=$view, invProj=$invProj, invView=$invView, viewProjection=$viewProjection, invViewProjection=$invViewProjection, near=$near, far=$far, fov=$fov, viewport=$viewport, zoom=$zoom)"
    }


    companion object {
        private var nextCameraId = 1L
    }
}

open class OrthographicCamera(virtualWidth: Int = 0, virtualHeight: Int = 0) : Camera() {
    private val planes = List(6) { FrustumPlane() }

    private val tempCenter = MutableVec3f()

    init {
        this.virtualWidth = virtualWidth
        this.virtualHeight = virtualHeight
        near = 0f
    }

    override fun updateProjectionMatrix() {
        val left = zoom * -virtualWidth / 2
        val right = zoom * (virtualWidth / 2)
        val bottom = zoom * -(virtualHeight / 2)
        val top = zoom * virtualHeight / 2

        if (near != far && left != right && top != bottom) {
            projection.setToOrthographic(left, right, bottom, top, near, far)
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
        if (x > virtualWidth + radius || x < -radius) {
            // sphere is either left or right of frustum
            return false
        }

        val y = tempCenter.dot(up)
        if (y > virtualHeight + radius || y < -radius) {
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