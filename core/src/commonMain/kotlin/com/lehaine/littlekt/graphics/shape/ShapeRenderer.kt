package com.lehaine.littlekt.graphics.shape

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.math.*
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.radians


/**
 * Draws lines, shapes, and paths using a [Batch] for optimal performance.
 *
 * Line mitering can be performed when drawing polygons and paths, see [JoinType] for options.
 *
 * Also includes an option to snap lines to the center of pixels.
 *
 * Ported from [Shape Drawer](https://github.com/earlygrey/shapedrawer) by [earlygrey](https://github.com/earlygrey)
 *
 * @param batch the batch used to batch draw calls with
 * @param slice a 1x1 slice of a texture. Generally a single white pixel.
 * @author Colton Daily
 * @date 7/16/2022
 */
class ShapeRenderer(val batch: Batch, val slice: TextureSlice = Textures.white) {
    /**
     * Whether line endpoints are snapped to the center of pixels by default.
     */
    var snap = false

    /**
     * The default thickness, in world units, of lines and outlines when drawing when thickness is not specified
     */
    var thickness: Int = 1

    /**
     * The [SideEstimator] used to calculate the number of sides. Defaults to [DefaultSideEstimator].
     */
    var sideEstimator: SideEstimator = DefaultSideEstimator()

    /**
     * The packed color to be used when drawing polygons. See [Color.toFloatBits].
     */
    var colorBits: Float
        set(value) {
            batchManager.colorBits = value
        }
        get() = batchManager.colorBits

    /**
     * The current pixel size in world units.
     */
    var pixelSize: Float
        private set(value) {
            batchManager.pixelSize = value
        }
        get() = batchManager.pixelSize

    private val batchManager = BatchManager(batch, slice)
    private val lineDrawer = LineDrawer(batchManager)
    private val polygonDrawer = PolygonDrawer(batchManager, lineDrawer)
    private val pathDrawer = PathDrawer(batchManager, lineDrawer)

    /**
     * Uses the current projection and transformation matrices of [Batch] to calculate the size of the screen pixel
     * along the x-axis in world units, and calls sets the [pixelSize] with that value.
     *
     * This should be called if [Batch.projectionMatrix] or [Batch.transformMatrix]are changed.
     */
    fun updatePixelSize(context: Context) {
        val trans = batch.transformMatrix
        val proj = batch.projectionMatrix
        mat4.set(proj).mul(trans)
        val scaleX = mat4.scaleX
        val worldWidth = 2f / scaleX
        val newPixelSize = worldWidth / context.graphics.width
        pixelSize = newPixelSize
    }

    /**
     * Draws a line from point A to point B.
     * @param v1 starting vertex point
     * @param v2 ending vertex point
     * @param color color of the start vertex
     * @param color2 color of the end vertex
     * @param thickness the thickness of the line in pixels
     * @param snap whether to snap the given coordinates to the center of the pixel
     */
    fun line(
        v1: Vec2f,
        v2: Vec2f,
        color: Color,
        color2: Color = color,
        thickness: Int = this.thickness,
        snap: Boolean = this.snap,
    ) = line(v1.x, v1.y, v2.x, v2.y, color, color2, thickness, snap)

    /**
     * Draws a line from point A to point B.
     * @param x starting x-coord
     * @param y starting y-coord
     * @param x2 ending x-coord
     * @param y2 ending y-coord
     * @param color color of the start vertex
     * @param color2 color of the end vertex
     * @param thickness the thickness of the line in pixels
     * @param snap whether to snap the given coordinates to the center of the pixel
     */
    fun line(
        x: Float,
        y: Float,
        x2: Float,
        y2: Float,
        color: Color,
        color2: Color = color,
        thickness: Int = this.thickness,
        snap: Boolean = this.snap,
    ) {
        line(x, y, x2, y2, color.toFloatBits(), color2.toFloatBits(), thickness, snap)
    }

    /**
     * Draws a line from point A to point B.
     * @param v1 starting vertex point
     * @param v2 ending vertex point
     * @param colorBits packed color of the start vertex
     * @param colorBits2 packed color of the end vertex
     * @param thickness the thickness of the line in world units
     * @param snap whether to snap the given coordinates to the center of the pixel
     */
    fun line(
        v1: Vec2f,
        v2: Vec2f,
        colorBits: Float = this.colorBits,
        colorBits2: Float = colorBits,
        thickness: Int = this.thickness,
        snap: Boolean = this.snap,
    ) = line(v1.x, v1.y, v2.x, v2.y, colorBits, colorBits2, thickness, snap)

    /**
     * Draws a line from point A to point B.
     * @param x starting x-coord
     * @param y starting y-coord
     * @param x2 ending x-coord
     * @param y2 ending y-coord
     * @param colorBits packed color of the start vertex
     * @param colorBits2 packed color of the end vertex
     * @param thickness the thickness of the line in world units
     * @param snap whether to snap the given coordinates to the center of the pixel
     */
    fun line(
        x: Float,
        y: Float,
        x2: Float,
        y2: Float,
        colorBits: Float = this.colorBits,
        colorBits2: Float = colorBits,
        thickness: Int = this.thickness,
        snap: Boolean = this.snap,
    ) {
        lineDrawer.line(x, y, x2, y2, thickness, snap, colorBits, colorBits2)
    }

    /**
     * Draws a path by drawing a line between each point and the next.
     *
     * The points at which two lines connect can be mitered to give a smooth join.
     * Note that this may cause strange looking joins when the angle between connected lines approaches &pi;, as the
     * miter can get arbitrarily long. For thin line where the miter cannot be seen,
     * you can set [joinType] to [JoinType.NONE].
     *
     * Only a subset of the path containing unique consecutive points (up to some small error) will be considered.
     * For example, the paths [(0,0), (1.001, 1, (1, 1), (2, 2)] and [(0,0), (1,1), (2,2)] will be drawn identically.
     *
     * If [pathPoints] is empty nothing will be drawn, if it contains wo points [line] will be used.
     *
     * @param pathPoints a list of [Vec2f] containing the ordered points in the path
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param open if false then the first and last points are connected
     */
    fun path(
        pathPoints: List<Vec2f>,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
        open: Boolean = true,
    ) {
        pathDrawer.path(pathPoints, thickness, joinType, open)
    }

    /**
     * Draws a path by drawing al ine between each point and the next. See [path] for details.
     * @param pathPoints a [FloatArray] containing the ordered points in the path
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param start the index of [pathPoints] which represents the first point to draw, inclusive
     * @param end the index of [pathPoints] which represents the last point to draw, exclusive
     * @param open if false then the first and last points are connected
     */
    fun path(
        pathPoints: FloatArray,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
        start: Int = 0,
        end: Int = pathPoints.size,
        open: Boolean = true,
    ) {
        pathDrawer.path(pathPoints, thickness, joinType, start, end, open)
    }

    /**
     * Draws a circle around the specified point with the given radius.
     * @param center the center point
     * @param radius the radius of the circle
     * @param rotation the rotation of the circle
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     */
    fun circle(
        center: Vec2f,
        radius: Float,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
    ) = circle(center.x, center.y, radius, rotation, thickness, joinType)

    /**
     * Draws a circle around the specified point with the given radius.
     * @param x center x-coord
     * @param y center y-coord
     * @param radius the radius of the circle
     * @param rotation the rotation of the circle
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     */
    fun circle(
        x: Float,
        y: Float,
        radius: Float,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
    ) {
        ellipse(x, y, radius, radius, rotation, thickness, joinType)
    }

    /**
     * Draws an ellipse around the specified point with the given radius's.
     * @param center the center point
     * @param rx the horizontal radius
     * @param ry the vertical radius
     * @param rotation the rotation of the ellipse
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     */
    fun ellipse(
        center: Vec2f,
        rx: Float,
        ry: Float = rx,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
    ) = ellipse(center.x, center.y, rx, ry, rotation, thickness, joinType)

    /**
     * Draws an ellipse around the specified point with the given radius's.
     * @param center the center point
     * @param radius the radius vector
     * @param rotation the rotation of the ellipse
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     */
    fun ellipse(
        center: Vec2f,
        radius: Vec2f,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
    ) = ellipse(center.x, center.y, radius.x, radius.y, rotation, thickness, joinType)

    /**
     * Draws an ellipse around the specified point with the given radius's.
     * @param x the x-coord of the center point
     * @param y the y-coord of the center point
     * @param rx the horizontal radius
     * @param ry the vertical radius
     * @param rotation the rotation of the ellipse
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     */
    fun ellipse(
        x: Float,
        y: Float,
        rx: Float,
        ry: Float = rx,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
    ) {
        polygon(x, y, estimateSidesRequired(rx, ry), rx, ry, rotation, thickness, joinType)
    }

    /**
     * Draws a triangle at the specified points.
     * @param v1 the coordinates of the first vertex
     * @param v2 the coordinates of the second vertex
     * @param v3 the coordinates of the third vertex
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the packed color to draw the outline. See [Color.toFloatBits].
     */
    fun triangle(
        v1: Vec2f,
        v2: Vec2f,
        v3: Vec3f,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.POINTY else JoinType.NONE,
        color: Float = colorBits,
    ) = triangle(v1.x, v1.y, v2.x, v2.y, v3.x, v3.y, thickness, joinType, color)

    /**
     * Draws a triangle at the specified points.
     * @param x1 x-coord of first vertex
     * @param y1 y-coord of first vertex
     * @param x2 x-coord of second vertex
     * @param y2 y-coord of second vertex
     * @param x3 x-coord of third vertex
     * @param y3 y-coord of third vertex
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the packed color to draw the outline. See [Color.toFloatBits].
     */
    fun triangle(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.POINTY else JoinType.NONE,
        color: Float = colorBits,
    ) {

        val cBits = colorBits
        colorBits = color
        if (joinType == JoinType.NONE) {
            line(x1, y1, x2, y2, thickness = thickness)
            line(x2, y2, x3, y3, thickness = thickness)
            line(x3, y3, x1, y1, thickness = thickness)
        } else {
            // TODO impl via path drawer
        }
        colorBits = cBits
    }

    /**
     * Draws a rectangle.
     * @param position the position of the bottom left corner of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     */
    fun rectangle(
        position: Vec2f,
        width: Float,
        height: Float,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = JoinType.POINTY,
    ) = rectangle(position.x, position.y, width, height, rotation, thickness, joinType)

    /**
     * Draws a rectangle.
     * @param rect the rectangle info
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     */
    fun rectangle(
        rect: Rect,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = JoinType.POINTY,
    ) = rectangle(rect.x, rect.y, rect.width, rect.height, rotation, thickness, joinType)

    /**
     * Draws a rectangle.
     * @param x x-coord of the bottom left corner of the rectangle
     * @param y y-coord of the bottom left corner of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     */
    fun rectangle(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = JoinType.POINTY,
    ) {
        if (joinType == JoinType.POINTY && rotation.radians.isFuzzyZero()) {
            val halfThickness = 0.5f * thickness
            val nx = x + width
            val ny = y + height
            val caching = batchManager.cachingDraws
            lineDrawer.run {
                pushLine(x + halfThickness, y, nx - halfThickness, y, thickness, false) // bottom
                pushLine(x + halfThickness, ny, nx - halfThickness, ny, thickness, false) // top
                pushLine(x, y - halfThickness, x, ny + halfThickness, thickness, false) // left
                pushLine(nx, y - halfThickness, nx, ny + halfThickness, thickness, false) // right
            }
            if (!caching) {
                batchManager.pushToBatch()
            }
            return
        }
        // TODO impl via path
    }

    /**
     * Draws a regular polygon by drawing lines between the vertices
     * @param center the center point
     * @param sides the number of sides
     * @param scaleX the horizontal scale
     * @param scaleY the vertical scale
     * @param rotation the rotation
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     */
    fun polygon(
        center: Vec2f,
        sides: Int,
        scaleX: Float,
        scaleY: Float = scaleX,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.POINTY else JoinType.NONE,
    ) = polygon(center.x, center.y, sides, scaleX, scaleY, rotation, thickness, joinType)

    /**
     * Draws a regular polygon by drawing lines between the vertices
     * @param x the x-coord of the center point
     * @param y the y-coord of the center point
     * @param sides the number of sides
     * @param scaleX the horizontal scale
     * @param scaleY the vertical scale
     * @param rotation the rotation
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     */
    fun polygon(
        x: Float,
        y: Float,
        sides: Int,
        scaleX: Float,
        scaleY: Float = scaleX,
        rotation: Angle = 0.radians,
        thickness: Int = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.POINTY else JoinType.NONE,
    ) {
        polygonDrawer.polygon(x, y, sides, scaleX, scaleY, rotation, thickness, joinType, 0.radians, PI2_F)
    }


    private fun isJoinNecessary(thickness: Int) = thickness > 3 * batchManager.pixelSize
    private fun isJoinNecessary() = isJoinNecessary(thickness)
    private fun estimateSidesRequired(rx: Float, ry: Float) =
        sideEstimator.estimateSidesRequired(batchManager.pixelSize, rx, ry)

    companion object {
        private val mat4 = Mat4()
    }
}

class DefaultSideEstimator : SideEstimator