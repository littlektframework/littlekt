package com.littlekt.graphics

import com.littlekt.Context
import com.littlekt.math.*
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.degrees
import com.littlekt.math.geom.normalized
import com.littlekt.util.LazyMat4
import com.littlekt.util.viewport.Viewport

/**
 * A base camera class that handles calculating the view and projection matrices based on position
 * and direction.
 *
 * @author Colton Daily
 * @date 11/27/2021
 */
abstract class Camera {
    /** The internal id of the [Camera]. */
    val id = nextCameraId++

    /** @return ratio of `virtualWidth / virtualHeight` */
    val aspectRatio: Float
        get() = if (virtualHeight > 0) virtualWidth / virtualHeight else 1f

    /**
     * The current position of the camera. This can be changed directly and then [update] to
     * calculate the matrices.
     */
    val position = MutableVec3f(0f)

    /** The direction the camera faces. */
    abstract val direction: MutableVec3f

    /** The up-direction. */
    abstract val up: MutableVec3f

    /**
     * The right direction.
     *
     * @see [rightMutable]
     */
    val right: Vec3f
        get() = rightMutable

    /** A mutable version of [right]. */
    protected val rightMutable = MutableVec3f()

    /** The project matrix. */
    val projection = Mat4()

    /** The view matrix. */
    val view = Mat4()

    private val lazyInvProjection = LazyMat4 { projection.invert(it) }

    /** The inverted matrix of the [projection] matrix. */
    val invProj: Mat4
        get() = lazyInvProjection.get()

    private val lazyInvView = LazyMat4 { view.invert(it) }

    /** The inverted matrix of the [view] matrix. */
    val invView: Mat4
        get() = lazyInvView.get()

    private val lazyViewProjection = LazyMat4 { projection.mul(view, it) }

    /** The combined matrix of [view] and [projection]. */
    val viewProjection: Mat4
        get() = lazyViewProjection.get()

    private val lazyInvViewProjection = LazyMat4 { viewProjection.invert(it) }

    /** The inverted matrix of [viewProjection]. */
    val invViewProjection: Mat4
        get() = lazyInvViewProjection.get()

    /** The near clipping plane distance */
    var near = 1f

    /** The far clipping plane distance */
    var far = 100f

    /** The current field of view. */
    var fov = 67.degrees

    /** The virtual width of this camera. */
    var virtualWidth: Float = 0f

    /** The virtual height of this camera. */
    var virtualHeight: Float = 0f

    /** The current zoom value. */
    var zoom: Float = 1f

    private val tempVec2 = MutableVec2f()
    private val tempVec3 = MutableVec3f()
    private val tempVec4 = MutableVec4f()

    /** Updates the view and projection matrices. */
    open fun update() {
        updateViewMatrix()
        updateProjectionMatrix()

        lazyInvProjection.isDirty = true
        lazyViewProjection.isDirty = true
        lazyInvViewProjection.isDirty = true
    }

    /** Updates the view matrix. */
    protected open fun updateViewMatrix() {
        direction.cross(up, rightMutable).norm()
        view.setToLookAt(position, tempVec3.set(position).add(direction), up)
        lazyInvView.isDirty = true
    }

    /** Updates the projection matrix. */
    protected abstract fun updateProjectionMatrix()

    /** Update the [direction] of this camera to look at the specified point. */
    fun lookAt(x: Float, y: Float, z: Float) {
        tempVec3.set(x, y, z).subtract(position).norm()
        if (tempVec3 != Vec3f.ZERO) {
            val dot = tempVec3.dot(up)
            if ((dot - 1f).isFuzzyZero(0.000000001f)) {
                up.set(direction).scale(-1f)
            } else if ((dot + 1f).isFuzzyZero(0.000000001f)) {
                up.set(direction)
            }
            direction.set(tempVec3)
            normalizeUp()
        }
    }

    /** Update the [direction] of this camera to look at the specified point. */
    fun lookAt(target: Vec3f) = lookAt(target.x, target.y, target.z)

    /** Rotates the [direction] and [up] vectors by the given [angle] around the given axis. */
    fun rotate(angle: Angle, axisX: Float, axisY: Float, axisZ: Float) {
        direction.rotate(angle, axisX, axisY, axisZ)
        up.rotate(angle, axisX, axisY, axisZ)
    }

    /** Rotates the [direction] and [up] vectors by the given [angle] around the given [axis]. */
    fun rotate(angle: Angle, axis: Vec3f) {
        if (angle.normalized.radians.isFuzzyZero()) return
        direction.rotate(angle, axis)
        up.rotate(angle, axis)
    }

    /** Rotates the [direction] and [up] vectors by the given [quaternion]. */
    fun rotate(quaternion: Quaternion) {
        quaternion.transform(direction, direction)
        quaternion.transform(up, up)
    }

    /**
     * Rotates the [direction] and [up] vectors by the given [angle] around the given [axis], with
     * the axis attached to the given [point].
     */
    fun rotateAround(point: Vec3f, axis: Vec3f, angle: Angle) {
        tempVec3.set(point).subtract(position)
        translate(tempVec3)
        rotate(angle, axis)
        tempVec3.rotate(angle, axis)
        translate(-tempVec3.x, -tempVec3.y, -tempVec3.z)
    }

    /** Translate the [position] by the given offsets. */
    fun translate(x: Float, y: Float, z: Float) {
        position.add(x, y, z)
    }

    /** Translate the [position] by the given offset. */
    fun translate(offset: Vec3f) {
        position += offset
    }

    /** Normalize the [up] vector. */
    fun normalizeUp() {
        tempVec3.set(direction).cross(up, tempVec3)
        up.set(tempVec3).cross(direction, up).norm()
    }

    /** Determines if the given [point] and [size] are within the camera frustum. */
    fun boundsInFrustum(point: Vec3f, size: Vec3f): Boolean =
        boundsInFrustum(point.x, point.y, point.z, size.x, size.y, size.z)

    /** Determines if the given [center] point and [radius] are within the camera frustum. */
    fun sphereInFrustum(center: Vec3f, radius: Float): Boolean =
        sphereInFrustum(center.x, center.y, center.z, radius)

    /** Determines if the given center point and [radius] are within the camera frustum. */
    fun sphereInFrustum(cx: Float, cy: Float, radius: Float): Boolean =
        sphereInFrustum(cx, cy, 0f, radius)

    /** Determines if the given point and size are within the camera frustum. */
    fun boundsInFrustum(px: Float, py: Float, width: Float, height: Float): Boolean =
        boundsInFrustum(px, py, 0f, width, height, 0f)

    /** Determines if the given center point and [radius] are within the camera frustum. */
    abstract fun sphereInFrustum(cx: Float, cy: Float, cz: Float, radius: Float): Boolean

    /** Determines if the given point and size are within the camera frustum. */
    abstract fun boundsInFrustum(
        px: Float,
        py: Float,
        pz: Float,
        width: Float,
        height: Float,
        length: Float,
    ): Boolean

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

    /**
     * Convert the given [world] coordinates to screen coordinates.
     *
     * @return a newly created [MutableVec2f] that contains the converted screen coordinates.
     */
    fun worldToScreen(context: Context, world: MutableVec2f, viewport: Viewport) =
        worldToScreen(
            context,
            world,
            viewport.x.toFloat(),
            viewport.y.toFloat(),
            viewport.width.toFloat(),
            viewport.height.toFloat(),
        )

    /**
     * Convert the given [world] coordinates to screen coordinates.
     *
     * @return a newly created [MutableVec2f] that contains the converted screen coordinates.
     */
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

    /**
     * Convert the given [world] coordinates to screen coordinates.
     *
     * @return `true` if able to project from screen to world coordinates; `false` otherwise.
     */
    fun worldToScreen(
        context: Context,
        world: Vec2f,
        viewport: Viewport,
        result: MutableVec2f,
    ): Boolean =
        worldToScreen(
            context,
            world,
            viewport.x.toFloat(),
            viewport.y.toFloat(),
            viewport.width.toFloat(),
            viewport.height.toFloat(),
            result,
        )

    /**
     * Convert the given world coordinates to screen coordinates.
     *
     * @return `true` if able to project from screen to world coordinates; `false` otherwise.
     */
    fun worldToScreen(
        context: Context,
        x: Float,
        y: Float,
        viewport: Viewport,
        result: MutableVec2f,
    ): Boolean =
        worldToScreen(
            context,
            x,
            y,
            viewport.x.toFloat(),
            viewport.y.toFloat(),
            viewport.width.toFloat(),
            viewport.height.toFloat(),
            result,
        )

    /**
     * Convert the given world coordinates to screen coordinates.
     *
     * @return a newly created [MutableVec2f] that contains the converted screen coordinates.
     */
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
        worldToScreen(
            context,
            tempVec2.set(x, y),
            viewportX,
            viewportY,
            viewportWidth,
            viewportHeight,
            result,
        )
        return result
    }

    /**
     * Convert the given world coordinates to screen coordinates.
     *
     * @return `true` if able to project from screen to world coordinates; `false` otherwise.
     */
    fun worldToScreen(
        context: Context,
        x: Float,
        y: Float,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
        result: MutableVec2f,
    ): Boolean =
        worldToScreen(
            context,
            tempVec2.set(x, y),
            viewportX,
            viewportY,
            viewportWidth,
            viewportHeight,
            result,
        )

    /**
     * Convert the given [world] coordinates to screen coordinates.
     *
     * @param result the output result vector of the calculation.
     * @return `true` if able to project from screen to world coordinates; `false` otherwise.
     */
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
        result.y = (1 + result.y) * 0.5f * viewportHeight + viewportY

        return true
    }

    /**
     * Convert the given [world] coordinates to screen coordinates.
     *
     * @return `true` if able to project from screen to world coordinates; `false` otherwise.
     */
    fun worldToScreen(
        context: Context,
        world: Vec3f,
        viewport: Viewport,
        result: MutableVec3f,
    ): Boolean =
        worldToScreen(
            context,
            world,
            viewport.x.toFloat(),
            viewport.y.toFloat(),
            viewport.width.toFloat(),
            viewport.height.toFloat(),
            result,
        )

    /**
     * Convert the given [world] coordinates to screen coordinates.
     *
     * @return a newly created [MutableVec3f] that contains the converted screen coordinates.
     */
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

    /**
     * Convert the given world coordinates to screen coordinates.
     *
     * @return a newly created [MutableVec3f] that contains the converted screen coordinates.
     */
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
        worldToScreen(
            context,
            tempVec3.set(x, y, z),
            viewportX,
            viewportY,
            viewportWidth,
            viewportHeight,
            result,
        )
        return result
    }

    /**
     * Convert the given world coordinates to screen coordinates.
     *
     * @return `true` if able to project from screen to world coordinates; `false` otherwise.
     */
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
    ): Boolean =
        worldToScreen(
            context,
            tempVec3.set(x, y, z),
            viewportX,
            viewportY,
            viewportWidth,
            viewportHeight,
            result,
        )

    /**
     * Convert the given [world] coordinates to screen coordinates.
     *
     * @return `true` if able to project from screen to world coordinates; `false` otherwise.
     */
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
        result.y = (1 + result.y) * 0.5f * viewportHeight + viewportY
        result.z = (1 + result.z) * 0.5f

        return true
    }

    /**
     * Convert the given [screen] coordinates into world coordinates.
     *
     * @return a newly create [MutableVec2f] containing the calculated world coordinates.
     */
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

    /**
     * Convert the given [screen] coordinates into world coordinates.
     *
     * @return a newly create [MutableVec2f] containing the calculated world coordinates.
     */
    fun screenToWorld(context: Context, screen: MutableVec2f, viewport: Viewport): MutableVec2f =
        screenToWorld(
            context,
            screen,
            viewport.x.toFloat(),
            viewport.y.toFloat(),
            viewport.width.toFloat(),
            viewport.height.toFloat(),
        )

    /**
     * Convert the given screen coordinates into world coordinates.
     *
     * @return a newly create [MutableVec2f] containing the calculated world coordinates.
     */
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
        screenToWorld(
            context,
            tempVec2.set(x, y),
            viewportX,
            viewportY,
            viewportWidth,
            viewportHeight,
            result,
        )
        return result
    }

    /**
     * Convert the given screen coordinates into world coordinates.
     *
     * @param result the output result vector of the calculation.
     * @return `true` if able to unproject from screen to world coordinates; `false` otherwise.
     */
    fun screenToWorld(
        context: Context,
        x: Float,
        y: Float,
        viewportX: Float = 0f,
        viewportY: Float = 0f,
        viewportWidth: Float = context.graphics.width.toFloat(),
        viewportHeight: Float = context.graphics.height.toFloat(),
        result: MutableVec2f,
    ): Boolean =
        screenToWorld(
            context,
            tempVec2.set(x, y),
            viewportX,
            viewportY,
            viewportWidth,
            viewportHeight,
            result,
        )

    /**
     * Convert the given screen coordinates into world coordinates.
     *
     * @param result the output result vector of the calculation.
     * @return `true` if able to unproject from screen to world coordinates; `false` otherwise.
     */
    fun screenToWorld(
        context: Context,
        x: Float,
        y: Float,
        viewport: Viewport,
        result: MutableVec2f,
    ): Boolean = screenToWorld(context, tempVec2.set(x, y), viewport, result)

    /**
     * Convert the given screen coordinates into world coordinates.
     *
     * @param result the output result vector of the calculation.
     * @return `true` if able to unproject from screen to world coordinates; `false` otherwise.
     */
    fun screenToWorld(
        context: Context,
        screen: MutableVec2f,
        viewport: Viewport,
        result: MutableVec2f,
    ): Boolean =
        screenToWorld(
            context,
            screen,
            viewport.x.toFloat(),
            viewport.y.toFloat(),
            viewport.width.toFloat(),
            viewport.height.toFloat(),
            result,
        )

    /**
     * Convert the given screen coordinates into world coordinates.
     *
     * @param result the output result vector of the calculation.
     * @return `true` if able to unproject from screen to world coordinates; `false` otherwise.
     */
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
        val y = context.graphics.height - screen.y - viewportY

        tempVec4.set(2f * x / viewportWidth - 1f, 2f * y / viewportHeight - 1f, -1f, 1f)
        invViewProjection.transform(tempVec4)
        val s = 1f / tempVec4.w
        result.set(tempVec4.x * s, tempVec4.y * s)
        return true
    }

    /**
     * Convert the given [screen] coordinates into world coordinates.
     *
     * @return a newly create [MutableVec3f] containing the calculated world coordinates.
     */
    fun screenToWorld(context: Context, screen: Vec3f, viewport: Viewport): MutableVec3f =
        screenToWorld(
            context,
            screen,
            viewport.x.toFloat(),
            viewport.y.toFloat(),
            viewport.width.toFloat(),
            viewport.height.toFloat(),
        )

    /**
     * Convert the given [screen] coordinates into world coordinates.
     *
     * @return a newly create [MutableVec3f] containing the calculated world coordinates.
     */
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

    /**
     * Convert the given screen coordinates into world coordinates.
     *
     * @return a newly create [MutableVec3f] containing the calculated world coordinates.
     */
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
        screenToWorld(
            context,
            tempVec3.set(x, y, z),
            viewportX,
            viewportY,
            viewportWidth,
            viewportHeight,
            result,
        )
        return result
    }

    /**
     * Convert the given screen coordinates into world coordinates.
     *
     * @param result the output result vector of the calculation.
     * @return `true` if able to unproject from screen to world coordinates; `false` otherwise.
     */
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
    ): Boolean =
        screenToWorld(
            context,
            tempVec3.set(x, y, z),
            viewportX,
            viewportY,
            viewportWidth,
            viewportHeight,
            result,
        )

    /**
     * Convert the given screen coordinates into world coordinates.
     *
     * @param result the output result vector of the calculation.
     * @return `true` if able to unproject from screen to world coordinates; `false` otherwise.
     */
    fun screenToWorld(
        context: Context,
        screen: Vec3f,
        viewport: Viewport,
        result: MutableVec3f,
    ): Boolean =
        screenToWorld(
            context,
            screen,
            viewport.x.toFloat(),
            viewport.y.toFloat(),
            viewport.width.toFloat(),
            viewport.height.toFloat(),
            result,
        )

    /**
     * Convert the given screen coordinates into world coordinates.
     *
     * @param result the output result vector of the calculation.
     * @return `true` if able to unproject from screen to world coordinates; `false` otherwise.
     */
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

        tempVec4.set(
            2f * x / viewportWidth - 1f,
            2f * y / viewportHeight - 1f,
            2f * screen.z - 1f,
            1f,
        )
        invViewProjection.transform(tempVec4)
        val s = 1f / tempVec4.w
        result.set(tempVec4.x * s, tempVec4.y * s, tempVec4.z * s)
        return true
    }

    /**
     * Computes the given [pickRay] from the given screen coordinates, if valid. The calculation
     * result is stored in the given [pickRay].
     *
     * @return `true` if the given [pickRay] updated and valid.
     */
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
        var valid =
            screenToWorld(
                context,
                tempVec3.set(screenX, screenY, 0f),
                viewportX,
                viewportY,
                viewportWidth,
                viewportHeight,
                pickRay.origin,
            )
        valid =
            valid &&
                screenToWorld(
                    context,
                    tempVec3.set(screenX, screenY, 1f),
                    viewportX,
                    viewportY,
                    viewportWidth,
                    viewportHeight,
                    pickRay.direction,
                )

        if (valid) {
            pickRay.direction.subtract(pickRay.origin)
            pickRay.direction.norm()
        }

        return valid
    }

    override fun toString(): String {
        return "Camera(id=$id, position=$position, direction=$direction, up=$up, rightDir=$right, projection=$projection, view=$view, invProj=$invProj, invView=$invView, viewProjection=$viewProjection, invViewProjection=$invViewProjection, near=$near, far=$far, fov=$fov, zoom=$zoom)"
    }

    companion object {
        private var nextCameraId = 1L
    }
}
