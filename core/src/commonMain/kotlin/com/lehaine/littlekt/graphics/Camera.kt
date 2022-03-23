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

    var virtualWidth: Float = 0f

    var virtualHeight: Float = 0f

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

    fun worldToScreen(context: Context, screen: MutableVec2f, viewport: Viewport) = worldToScreen(context,
        screen,
        viewport.x.toFloat(),
        viewport.y.toFloat(),
        viewport.width.toFloat(),
        viewport.height.toFloat())

    fun worldToScreen(
        context: Context,
        world: Vec2f,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
    ): MutableVec2f {
        val result = MutableVec2f()
        worldToScreen(context, world, viewportX, viewportY, viewportWidth, viewportHeight, result)
        return result
    }

    fun worldToScreen(context: Context, screen: MutableVec2f, viewport: Viewport, result: MutableVec2f) = worldToScreen(
        context,
        screen,
        viewport.x.toFloat(),
        viewport.y.toFloat(),
        viewport.width.toFloat(),
        viewport.height.toFloat(),
        result)

    fun worldToScreen(
        context: Context,
        x: Float,
        y: Float,
        viewport: Viewport,
        result: MutableVec2f,
    ) = worldToScreen(context,
        x,
        y,
        viewport.x.toFloat(),
        viewport.y.toFloat(),
        viewport.width.toFloat(),
        viewport.height.toFloat(),
        result)

    fun worldToScreen(
        context: Context,
        x: Float,
        y: Float,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
    ): MutableVec2f {
        val result = MutableVec2f()
        worldToScreen(context, tempVec2.set(x, y), viewportX, viewportY, viewportWidth, viewportHeight, result)
        return result
    }

    fun worldToScreen(
        context: Context,
        x: Float,
        y: Float,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
        result: MutableVec2f,
    ) = worldToScreen(context, tempVec2.set(x, y), viewportX, viewportY, viewportWidth, viewportHeight, result)

    fun worldToScreen(
        context: Context,
        world: Vec2f,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
        result: MutableVec2f,
    ): Boolean {
        if (!project(world, result)) {
            return false
        }
        result.x = (1 + result.x) * 0.5f * viewportWidth + viewportX
        result.y = context.graphics.height - (1 + result.y) * 0.5f * viewportHeight + viewportY

        return true
    }

    fun worldToScreen(
        context: Context,
        world: Vec3f,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
    ): MutableVec3f {
        val result = MutableVec3f()
        worldToScreen(context, world, viewportX, viewportY, viewportWidth, viewportHeight, result)
        return result
    }

    fun worldToScreen(
        context: Context,
        x: Float,
        y: Float,
        z: Float,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
    ): MutableVec3f {
        val result = MutableVec3f()
        worldToScreen(context, tempVec3.set(x, y, z), viewportX, viewportY, viewportWidth, viewportHeight, result)
        return result
    }

    fun worldToScreen(
        context: Context,
        x: Float,
        y: Float,
        z: Float,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
        result: MutableVec3f,
    ) = worldToScreen(context, tempVec3.set(x, y, z), viewportX, viewportY, viewportWidth, viewportHeight, result)


    fun worldToScreen(
        context: Context,
        world: Vec3f,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
        result: MutableVec3f,
    ): Boolean {
        if (!project(world, result)) {
            return false
        }
        result.x = (1 + result.x) * 0.5f * viewportWidth + viewportX
        result.y = context.graphics.height - (1 + result.y) * 0.5f * viewportHeight + viewportY
        result.z = (1 + result.z) * 0.5f

        return true
    }

    fun screenToWorld(
        context: Context,
        screen: Vec2f,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
    ): MutableVec2f {
        val result = MutableVec2f()
        screenToWorld(context, screen, viewportX, viewportY, viewportWidth, viewportHeight, result)
        return result
    }

    fun screenToWorld(context: Context, screen: MutableVec2f, viewport: Viewport) = screenToWorld(context,
        screen,
        viewport.x.toFloat(),
        viewport.y.toFloat(),
        viewport.width.toFloat(),
        viewport.height.toFloat())

    fun screenToWorld(
        context: Context,
        x: Float,
        y: Float,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
    ): MutableVec2f {
        val result = MutableVec2f()
        screenToWorld(context, tempVec2.set(x, y), viewportX, viewportY, viewportWidth, viewportHeight, result)
        return result
    }

    fun screenToWorld(
        context: Context,
        x: Float,
        y: Float,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
        result: MutableVec2f,
    ) = screenToWorld(context, tempVec2.set(x, y), viewportX, viewportY, viewportWidth, viewportHeight, result)
    
    fun screenToWorld(context: Context, x: Float, y: Float, viewport: Viewport, result: MutableVec2f) =
        screenToWorld(context, tempVec2.set(x, y), viewport, result)

    fun screenToWorld(context: Context, screen: MutableVec2f, viewport: Viewport, result: MutableVec2f) = screenToWorld(
        context,
        screen,
        viewport.x.toFloat(),
        viewport.y.toFloat(),
        viewport.width.toFloat(),
        viewport.height.toFloat(),
        result)

    fun screenToWorld(
        context: Context,
        screen: Vec2f,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
        result: MutableVec2f,
    ): Boolean {
        val x = screen.x - viewportX
        val y = (context.graphics.height - screen.y) - viewportY

        tempVec4.set(2f * x / viewportWidth - 1f, 2f * y / viewportHeight - 1f, 1f, 1f)
        invViewProjection.transform(tempVec4)
        val s = 1f / tempVec4.w
        result.set(tempVec4.x * s, tempVec4.y * s)
        return true
    }

    fun screenToWorld(
        context: Context,
        screen: Vec3f,
        viewport: Viewport,
    ): MutableVec3f = screenToWorld(context,
        screen,
        viewport.x.toFloat(),
        viewport.y.toFloat(),
        viewport.width.toFloat(),
        viewport.height.toFloat())

    fun screenToWorld(
        context: Context,
        screen: Vec3f,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
    ): MutableVec3f {
        val result = MutableVec3f()
        screenToWorld(context, screen, viewportX, viewportY, viewportWidth, viewportHeight, result)
        return result
    }

    fun screenToWorld(
        context: Context,
        x: Float,
        y: Float,
        z: Float,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
    ): MutableVec3f {
        val result = MutableVec3f()
        screenToWorld(context, tempVec3.set(x, y, z), viewportX, viewportY, viewportWidth, viewportHeight, result)
        return result
    }

    fun screenToWorld(
        context: Context,
        x: Float,
        y: Float,
        z: Float,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
        result: MutableVec3f,
    ) = screenToWorld(context, tempVec3.set(x, y, z), viewportX, viewportY, viewportWidth, viewportHeight, result)


    fun screenToWorld(
        context: Context,
        screen: Vec3f,
        viewport: Viewport,
        result: MutableVec3f,
    ) = screenToWorld(context,
        screen,
        viewport.x.toFloat(),
        viewport.y.toFloat(),
        viewport.width.toFloat(),
        viewport.height.toFloat(),
        result)

    fun screenToWorld(
        context: Context,
        screen: Vec3f,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
        result: MutableVec3f,
    ): Boolean {
        val x = screen.x - viewportX
        val y = (context.graphics.height - screen.y) - viewportY

        tempVec4.set(2f * x / viewportWidth - 1f, 2f * y / viewportHeight - 1f, 2f * screen.z - 1f, 1f)
        invViewProjection.transform(tempVec4)
        val s = 1f / tempVec4.w
        result.set(tempVec4.x * s, tempVec4.y * s, tempVec4.z * s)
        return true
    }

    fun computePickRay(
        context: Context,
        pickRay: Ray,
        screenX: Float,
        screenY: Float,
        viewportX: Float,
        viewportY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
    ): Boolean {
        var valid = screenToWorld(context,
            tempVec3.set(screenX, screenY, 0f),
            viewportX,
            viewportY,
            viewportWidth,
            viewportHeight,
            pickRay.origin)
        valid = valid && screenToWorld(context,
            tempVec3.set(screenX, screenY, 1f),
            viewportX,
            viewportY,
            viewportWidth,
            viewportHeight,
            pickRay.direction)

        if (valid) {
            pickRay.direction.subtract(pickRay.origin)
            pickRay.direction.norm()
        }

        return valid
    }

    override fun toString(): String {
        return "Camera(id=$id, position=$position, direction=$direction, up=$up, rightDir=$rightDir, projection=$projection, view=$view, invProj=$invProj, invView=$invView, viewProjection=$viewProjection, invViewProjection=$invViewProjection, near=$near, far=$far, fov=$fov, zoom=$zoom)"
    }


    companion object {
        private var nextCameraId = 1L
    }
}

open class OrthographicCamera(virtualWidth: Float = 0f, virtualHeight: Float = 0f) : Camera() {
    constructor(virtualWidth: Int, virtualHeight: Int) : this(virtualWidth.toFloat(), virtualHeight.toFloat())

    private val planes = List(6) { FrustumPlane() }

    private val tempCenter = MutableVec3f()

    init {
        this.virtualWidth = virtualWidth
        this.virtualHeight = virtualHeight
        near = 0f
    }

    fun ortho(virtualWidth: Float, virtualHeight: Float) {
        this.virtualWidth = virtualWidth
        this.virtualHeight = virtualHeight
        position.set(zoom * virtualWidth * 0.5f, zoom * virtualHeight * 0.5f, 0f)
        update()
    }

    fun ortho(virtualWidth: Int, virtualHeight: Int) = ortho(virtualWidth.toFloat(), virtualHeight.toFloat())

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