package com.littlekt.graphics.g2d.shape

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.math.*
import com.littlekt.math.geom.*
import com.littlekt.resources.Textures

/**
 * Draws lines, shapes, and paths using a [Batch] for optimal performance.
 *
 * Line mitering can be performed when drawing polygons and paths, see [JoinType] for options.
 *
 * Also includes an option to snap lines to the center of pixels.
 *
 * Ported from [Shape Drawer](https://github.com/earlygrey/shapedrawer) by
 * [earlygrey](https://github.com/earlygrey)
 *
 * @param batch the batch used to batch draw calls with
 * @param slice a 1x1 slice of a texture. Generally a single white pixel.
 * @author Colton Daily
 * @date 7/16/2022
 */
class ShapeRenderer(val batch: Batch, val slice: TextureSlice = Textures.white) {
    /** Whether line endpoints are snapped to the center of pixels by default. */
    var snap = false

    /**
     * The default thickness, in world units, of lines and outlines when drawing when thickness is
     * not specified
     */
    var thickness: Float = 1f

    /**
     * The [SideEstimator] used to calculate the number of sides. Defaults to
     * [DefaultSideEstimator].
     */
    var sideEstimator: SideEstimator = DefaultSideEstimator()

    /** The color to be used when drawing polygons. */
    var color: Color
        set(value) {
            batchManager.color = value
        }
        get() = batchManager.color

    /** The current pixel size in world units. */
    var pixelSize: Float
        private set(value) {
            batchManager.pixelSize = value
        }
        get() = batchManager.pixelSize

    private val batchManager = BatchManager(batch, slice)
    private val lineDrawer = LineDrawer(batchManager)
    private val polygonDrawer = PolygonDrawer(batchManager, lineDrawer)
    private val filledPolygonDrawer = FilledPolygonDrawer(batchManager)
    private val pathDrawer = PathDrawer(batchManager, lineDrawer)

    /**
     * Uses the current projection and transformation matrices of [Batch] to calculate the size of
     * the screen pixel along the x-axis in world units, and calls sets the [pixelSize] with that
     * value.
     *
     * This should be called if [Batch.viewProjection] or [Batch.transformMatrix]are changed.
     */
    fun updatePixelSize(width: Int) {
        val trans = batch.transformMatrix
        val proj = batch.viewProjection
        mat4.set(proj).mul(trans)
        val scaleX = mat4.scaleX
        val worldWidth = 2f / scaleX
        val newPixelSize = worldWidth / width
        pixelSize = newPixelSize
    }

    /**
     * Draws a line from point A to point B.
     *
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
        color: Color = this.color,
        color2: Color = color,
        thickness: Float = this.thickness,
        snap: Boolean = this.snap,
    ) = line(v1.x, v1.y, v2.x, v2.y, color, color2, thickness, snap)

    /**
     * Draws a line from point A to point B.
     *
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
        color: Color = this.color,
        color2: Color = color,
        thickness: Float = this.thickness,
        snap: Boolean = this.snap,
    ) {
        lineDrawer.line(x, y, x2, y2, thickness, snap, color, color2)
    }

    /**
     * Draws a path by drawing a line between each point and the next.
     *
     * The points at which two lines connect can be mitered to give a smooth join. Note that this
     * may cause strange looking joins when the angle between connected lines approaches &pi;, as
     * the miter can get arbitrarily long. For thin line where the miter cannot be seen, you can set
     * [joinType] to [JoinType.NONE].
     *
     * Only a subset of the path containing unique consecutive points (up to some small error) will
     * be considered. For example, the paths [(0,0), (1.001, 1, (1, 1), (2, 2)] and
     * [(0,0), (1,1), (2,2)] will be drawn identically.
     *
     * If [pathPoints] is empty nothing will be drawn, if it contains wo points [line] will be used.
     *
     * @param pathPoints a list of [Vec2f] containing the ordered points in the path
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param open if false then the first and last points are connected
     * @param color the color to draw the outline.
     */
    fun path(
        pathPoints: List<Vec2f>,
        thickness: Float = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
        open: Boolean = true,
        color: Color = this.color,
    ) {
        val old = this.color
        this.color = color
        pathDrawer.path(pathPoints, thickness, joinType, open)
        this.color = old
    }

    /**
     * Draws a path by drawing al ine between each point and the next. See [path] for details.
     *
     * @param pathPoints a [FloatArray] containing the ordered points in the path
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param start the index of [pathPoints] which represents the first point to draw, inclusive
     * @param end the index of [pathPoints] which represents the last point to draw, exclusive
     * @param open if false then the first and last points are connected
     * @param color the color to draw the outline.
     */
    fun path(
        pathPoints: FloatArray,
        thickness: Float = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
        start: Int = 0,
        end: Int = pathPoints.size,
        open: Boolean = true,
        color: Color = this.color,
    ) {
        val old = this.color
        this.color = color
        pathDrawer.path(pathPoints, thickness, joinType, start, end, open)
        this.color = old
    }

    /**
     * Draws a circle around the specified point with the given radius.
     *
     * @param center the center point
     * @param radius the radius of the circle
     * @param rotation the rotation of the circle
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the color to draw the outline.
     */
    fun circle(
        center: Vec2f,
        radius: Float,
        rotation: Angle = 0.radians,
        thickness: Float = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
        color: Color = this.color,
    ) = circle(center.x, center.y, radius, rotation, thickness, joinType, color)

    /**
     * Draws a circle around the specified point with the given radius.
     *
     * @param x center x-coord
     * @param y center y-coord
     * @param radius the radius of the circle
     * @param rotation the rotation of the circle
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the color to draw the outline.
     */
    fun circle(
        x: Float,
        y: Float,
        radius: Float,
        rotation: Angle = 0.radians,
        thickness: Float = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
        color: Color = this.color,
    ) {
        ellipse(x, y, radius, radius, rotation, thickness, joinType, color)
    }

    /**
     * Draws an ellipse around the specified point with the given radius's.
     *
     * @param center the center point
     * @param rx the horizontal radius
     * @param ry the vertical radius
     * @param rotation the rotation of the ellipse
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the color to draw the outline.
     */
    fun ellipse(
        center: Vec2f,
        rx: Float,
        ry: Float = rx,
        rotation: Angle = 0.radians,
        thickness: Float = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
        color: Color = this.color,
    ) = ellipse(center.x, center.y, rx, ry, rotation, thickness, joinType, color)

    /**
     * Draws an ellipse around the specified point with the given radius's.
     *
     * @param center the center point
     * @param radius the horizontal and vertical radii
     * @param rotation the rotation of the ellipse
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the color to draw the outline.
     */
    fun ellipse(
        center: Vec2f,
        radius: Vec2f,
        rotation: Angle = 0.radians,
        thickness: Float = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
        color: Color = this.color,
    ) = ellipse(center.x, center.y, radius.x, radius.y, rotation, thickness, joinType, color)

    /**
     * Draws an ellipse as a stretched regular polygon estimating the number of sides required (see
     * [estimateSidesRequired]) to appear smooth enough based on the pixel size that has been set.
     *
     * @param x the x-coord of the center point
     * @param y the y-coord of the center point
     * @param rx the horizontal radius
     * @param ry the vertical radius
     * @param rotation the rotation of the ellipse
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the color to draw the outline.
     */
    fun ellipse(
        x: Float,
        y: Float,
        rx: Float,
        ry: Float = rx,
        rotation: Angle = 0.radians,
        thickness: Float = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.SMOOTH else JoinType.NONE,
        color: Color = this.color,
    ) {
        polygon(x, y, estimateSidesRequired(rx, ry), rx, ry, rotation, thickness, joinType, color)
    }

    /**
     * Draws a filled circle as a stretched regular polygon estimating the number of sides required
     * (see [estimateSidesRequired]) to appear smooth enough based on the pixel size that has been
     * set.
     *
     * @param center the center point
     * @param radius the radius
     * @param rotation the rotation of the ellipse
     * @param color the color of circle.
     */
    fun filledCircle(
        center: Vec2f,
        radius: Float,
        rotation: Angle = 0.radians,
        color: Color = this.color,
    ) = filledEllipse(center.x, center.y, radius, radius, rotation, color, color)

    /**
     * Draws a filled circle as a stretched regular polygon estimating the number of sides required
     * (see [estimateSidesRequired]) to appear smooth enough based on the pixel size that has been
     * set.
     *
     * @param x the x-coord of the center point
     * @param y the y-coord of the center point
     * @param radius the radius
     * @param rotation the rotation of the ellipse
     * @param color the color of circle.
     */
    fun filledCircle(
        x: Float,
        y: Float,
        radius: Float,
        rotation: Angle = 0.radians,
        color: Color = this.color,
    ) = filledEllipse(x, y, radius, radius, rotation, color, color)

    /**
     * Draws a filled ellipse as a stretched regular polygon estimating the number of sides required
     * (see [estimateSidesRequired]) to appear smooth enough based on the pixel size that has been
     * set.
     *
     * @param center the center point
     * @param radius the horizontal and vertical radii
     * @param rotation the rotation of the ellipse
     * @param innerColor the color of the center of the ellipse.
     * @param outerColor the color of the perimeter of the ellipse.
     */
    fun filledEllipse(
        center: Vec2f,
        radius: Vec2f,
        rotation: Angle = 0.radians,
        innerColor: Color = color,
        outerColor: Color = color,
    ) = filledEllipse(center.x, center.y, radius.x, radius.y, rotation, innerColor, outerColor)

    /**
     * Draws a filled ellipse as a stretched regular polygon estimating the number of sides required
     * (see [estimateSidesRequired]) to appear smooth enough based on the pixel size that has been
     * set.
     *
     * @param x the x-coord of the center point
     * @param y the y-coord of the center point
     * @param rx the horizontal radius
     * @param ry the vertical radius
     * @param rotation the rotation of the ellipse
     * @param innerColor the color of the center of the ellipse.
     * @param outerColor the color of the perimeter of the ellipse.
     */
    fun filledEllipse(
        x: Float,
        y: Float,
        rx: Float,
        ry: Float = rx,
        rotation: Angle = 0.radians,
        innerColor: Color = color,
        outerColor: Color = color,
    ) {
        filledPolygonDrawer.polygon(
            x,
            y,
            estimateSidesRequired(rx, ry),
            rx,
            ry,
            rotation,
            0.radians,
            PI2_F,
            innerColor,
            outerColor
        )
    }

    /**
     * Draws a triangle at the specified points.
     *
     * @param v1 the coordinates of the first vertex
     * @param v2 the coordinates of the second vertex
     * @param v3 the coordinates of the third vertex
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the color to draw the outline.
     */
    fun triangle(
        v1: Vec2f,
        v2: Vec2f,
        v3: Vec3f,
        thickness: Float = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.POINTY else JoinType.NONE,
        color: Color = this.color,
    ) = triangle(v1.x, v1.y, v2.x, v2.y, v3.x, v3.y, thickness, joinType, color)

    /**
     * Draws a triangle at the specified points.
     *
     * @param x1 x-coord of first vertex
     * @param y1 y-coord of first vertex
     * @param x2 x-coord of second vertex
     * @param y2 y-coord of second vertex
     * @param x3 x-coord of third vertex
     * @param y3 y-coord of third vertex
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the color to draw the outline.
     */
    fun triangle(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        thickness: Float = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.POINTY else JoinType.NONE,
        color: Color = this.color,
    ) {
        val prevColor = this.color
        this.color = color
        if (joinType == JoinType.NONE) {
            line(x1, y1, x2, y2, thickness = thickness)
            line(x2, y2, x3, y3, thickness = thickness)
            line(x3, y3, x1, y1, thickness = thickness)
        } else {
            trianglePathPoints[0] = x1
            trianglePathPoints[1] = y1
            trianglePathPoints[2] = x2
            trianglePathPoints[3] = y2
            trianglePathPoints[4] = x3
            trianglePathPoints[5] = y3
            pathDrawer.path(trianglePathPoints, thickness, joinType, open = false)
        }
        this.color = prevColor
    }

    /**
     * Draws a filled triangle at the specified points.
     *
     * @param v1 the coordinates of the first vertex
     * @param v2 the coordinates of the second vertex
     * @param v3 the coordinates of the third vertex
     * @param color the color of the first vertex or all if [color2] or [color3] are not set.
     * @param color2 the color of the second vertex. If [color3] then the third vertex as well.
     * @param color3 the color of the third vertex.
     */
    fun filledTriangle(
        v1: Vec2f,
        v2: Vec2f,
        v3: Vec3f,
        color: Color = this.color,
        color2: Color = color,
        color3: Color = color2,
    ) = filledTriangle(v1.x, v1.y, v2.x, v2.y, v3.x, v3.y, color, color2, color3)

    /**
     * Draws a filled triangle at the specified points.
     *
     * @param x1 x-coord of first vertex
     * @param y1 y-coord of first vertex
     * @param x2 x-coord of second vertex
     * @param y2 y-coord of second vertex
     * @param x3 x-coord of third vertex
     * @param y3 y-coord of third vertex
     * @param color the color of the first vertex or all if [color2] or [color3] are not set.
     * @param color2 the color of the second vertex. If [color3] then the third vertex as well.
     * @param color3 the color of the third vertex.
     */
    fun filledTriangle(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        color: Color = this.color,
        color2: Color = color,
        color3: Color = color2,
    ) {
        filledPolygonDrawer.triangle(x1, y1, x2, y2, x3, y3, color, color2, color3)
    }

    /**
     * Draws a rectangle.
     *
     * @param position the position of the bottom left corner of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param rotation rotation of the rectangle
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the color to draw the outline.
     */
    fun rectangle(
        position: Vec2f,
        width: Float,
        height: Float,
        rotation: Angle = 0.radians,
        thickness: Float = this.thickness,
        joinType: JoinType = JoinType.POINTY,
        color: Color = this.color,
    ) = rectangle(position.x, position.y, width, height, rotation, thickness, joinType, color)

    /**
     * Draws a rectangle.
     *
     * @param rect the rectangle info
     * @param rotation rotation of the rectangle
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the color to draw the outline.
     */
    fun rectangle(
        rect: Rect,
        rotation: Angle = 0.radians,
        thickness: Float = this.thickness,
        joinType: JoinType = JoinType.POINTY,
        color: Color = this.color,
    ) = rectangle(rect.x, rect.y, rect.width, rect.height, rotation, thickness, joinType, color)

    /**
     * Draws a rectangle.
     *
     * @param x x-coord of the bottom left corner of the rectangle
     * @param y y-coord of the bottom left corner of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param rotation rotation of the rectangle
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the color to draw the outline.
     */
    fun rectangle(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Angle = 0.radians,
        thickness: Float = this.thickness,
        joinType: JoinType = JoinType.POINTY,
        color: Color = this.color,
    ) {
        val old = this.color
        this.color = color
        if (joinType == JoinType.POINTY && rotation.normalized.radians.isFuzzyZero()) {
            val halfThickness = 0.5f * thickness
            val nx = x + width
            val ny = y + height
            val caching = batchManager.cachingDraws
            lineDrawer.run {
                pushLine(x + halfThickness, y, nx - halfThickness, y, thickness, false) // top
                pushLine(x + halfThickness, ny, nx - halfThickness, ny, thickness, false) // bottom
                pushLine(x, y - halfThickness, x, ny + halfThickness, thickness, false) // left
                pushLine(nx, y - halfThickness, nx, ny + halfThickness, thickness, false) // right
            }
            if (!caching) {
                batchManager.pushToBatch()
            }
            this.color = old
            return
        }

        var i = 0
        rectangleCorners[i++] = x
        rectangleCorners[i++] = y
        rectangleCorners[i++] = x + width
        rectangleCorners[i++] = y
        rectangleCorners[i++] = x + width
        rectangleCorners[i++] = y + height
        rectangleCorners[i++] = x
        rectangleCorners[i] = y + height

        if (!rotation.normalized.radians.isFuzzyZero()) {
            val centerX = x + width / 2f
            val centerY = y + height / 2f
            val cos = rotation.cosine
            val sin = rotation.sine
            for (j in 0 until 8 step 2) {
                rectangleCorners[j] -= centerX
                rectangleCorners[j + 1] -= centerY
                val rotatedX = rectangleCorners[j] * cos - rectangleCorners[j + 1] * sin
                val rotatedY = rectangleCorners[j] * sin + rectangleCorners[j + 1] * cos

                rectangleCorners[j] = rotatedX + centerX
                rectangleCorners[j + 1] = rotatedY + centerY
            }
        }
        path(rectangleCorners, thickness, joinType, open = false)
        this.color = old
    }

    /**
     * Draws a filled rectangle.
     *
     * @param position the position of the bottom left corner of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param rotation rotation of the rectangle
     * @param color the color of top right vertex. If no subsequent colors are set then the
     *   remaining vertices as well.
     * @param color2 the color of top left vertex. If no subsequent colors are set then the
     *   remaining vertices as well.
     * @param color3 the color of bottom left vertex. If no subsequent colors are set then the
     *   remaining vertices as well.
     * @param color4 the color of bottom right vertex.
     */
    fun filledRectangle(
        position: Vec2f,
        width: Float,
        height: Float,
        rotation: Angle = 0.radians,
        color: Color = this.color,
        color2: Color = color,
        color3: Color = color2,
        color4: Color = color3,
    ) =
        filledRectangle(
            position.x,
            position.y,
            width,
            height,
            rotation,
            color,
            color2,
            color3,
            color4
        )

    /**
     * Draws a filled rectangle.
     *
     * @param rect the rectangle info
     * @param color the color of top right vertex. If no subsequent colors are set then the
     *   remaining vertices as well.
     * @param color2 the color of top left vertex. If no subsequent colors are set then the
     *   remaining vertices as well.
     * @param color3 the color of bottom left vertex. If no subsequent colors are set then the
     *   remaining vertices as well.
     * @param color4 the color of bottom right vertex.
     */
    fun filledRectangle(
        rect: Rect,
        rotation: Angle = 0.radians,
        color: Color = this.color,
        color2: Color = color,
        color3: Color = color2,
        color4: Color = color3,
    ) =
        filledRectangle(
            rect.x,
            rect.y,
            rect.width,
            rect.height,
            rotation,
            color,
            color2,
            color3,
            color4
        )

    /**
     * Draws a filled rectangle.
     *
     * @param x x-coord of the bottom left corner of the rectangle
     * @param y y-coord of the bottom left corner of the rectangle
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param rotation rotation of the rectangle
     * @param color the color of top right vertex. If no subsequent colors are set then the
     *   remaining vertices as well.
     * @param color2 the color of top left vertex. If no subsequent colors are set then the
     *   remaining vertices as well.
     * @param color3 the color of bottom left vertex. If no subsequent colors are set then the
     *   remaining vertices as well.
     * @param color4 the color of bottom right vertex.
     */
    fun filledRectangle(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Angle = 0.radians,
        color: Color = this.color,
        color2: Color = color,
        color3: Color = color2,
        color4: Color = color3,
    ) {
        filledPolygonDrawer.rectangle(x, y, width, height, rotation, color, color2, color3, color4)
    }

    /**
     * Draws a regular polygon by drawing lines between the vertices
     *
     * @param center the center point
     * @param sides the number of sides
     * @param scaleX the horizontal scale
     * @param scaleY the vertical scale
     * @param rotation the rotation
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the color to draw the outline.
     */
    fun polygon(
        center: Vec2f,
        sides: Int,
        scaleX: Float,
        scaleY: Float = scaleX,
        rotation: Angle = 0.radians,
        thickness: Float = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.POINTY else JoinType.NONE,
        color: Color = this.color,
    ) = polygon(center.x, center.y, sides, scaleX, scaleY, rotation, thickness, joinType, color)

    /**
     * Draws a regular polygon by drawing lines between the vertices
     *
     * @param x the x-coord of the center point
     * @param y the y-coord of the center point
     * @param sides the number of sides
     * @param scaleX the horizontal scale
     * @param scaleY the vertical scale
     * @param rotation the rotation
     * @param thickness the thickness of the line in world units
     * @param joinType the type of join, see [JoinType]
     * @param color the color to draw the outline.
     */
    fun polygon(
        x: Float,
        y: Float,
        sides: Int,
        scaleX: Float,
        scaleY: Float = scaleX,
        rotation: Angle = 0.radians,
        thickness: Float = this.thickness,
        joinType: JoinType = if (isJoinNecessary(thickness)) JoinType.POINTY else JoinType.NONE,
        color: Color = this.color,
    ) {
        val old = this.color
        this.color = color
        polygonDrawer.polygon(
            x,
            y,
            sides,
            scaleX,
            scaleY,
            rotation,
            thickness,
            joinType,
            0.radians,
            PI2_F
        )
        this.color = old
    }

    /**
     * Draws a filled polygon.
     *
     * @param center the center point
     * @param sides the number of sides
     * @param scaleX the horizontal scale
     * @param scaleY the vertical scaledw
     * @param rotation the rotation of the polygon
     * @param innerColor the color of the center of the polygon.
     * @param outerColor the color of the perimeter of the polygon.
     */
    fun filledPolygon(
        center: Vec2f,
        sides: Int,
        scaleX: Float,
        scaleY: Float = scaleX,
        rotation: Angle = 0.radians,
        innerColor: Color = color,
        outerColor: Color = color,
    ) = filledPolygon(center.x, center.y, sides, scaleX, scaleY, rotation, innerColor, outerColor)

    /**
     * Draws a filled polygon.
     *
     * @param x the x-coord of the center point
     * @param y the y-coord of the center point
     * @param sides the number of sides
     * @param scaleX the horizontal scale
     * @param scaleY the vertical scale
     * @param rotation the rotation of the polygon
     * @param innerColor the color of the center of the polygon.
     * @param outerColor the color of the perimeter of the polygon.
     */
    fun filledPolygon(
        x: Float,
        y: Float,
        sides: Int,
        scaleX: Float,
        scaleY: Float = scaleX,
        rotation: Angle = 0.radians,
        innerColor: Color = color,
        outerColor: Color = color,
    ) {
        filledPolygonDrawer.polygon(
            x,
            y,
            sides,
            scaleX,
            scaleY,
            rotation,
            0.radians,
            PI2_F,
            innerColor,
            outerColor
        )
    }

    /**
     * Draws a filled polygon used the specified vertices.
     *
     * Note: this triangulates the polygon everytime it is called - it is recommended to cache the
     * triangles. [Triangulator.computeTriangles] can be used to calculate the triangles.
     *
     * @param vertices consecutive ordered pairs of the x-y coordinates of the vertices of the
     *   polygon
     * @param offset the index of the vertices [FloatArray] at which to start drawing
     * @param count the number of vertices to draw from the [offset]
     * @param color the of the polygon
     */
    fun filledPolygon(
        vertices: FloatArray,
        offset: Int = 0,
        count: Int = vertices.size,
        color: Color = batchManager.color
    ) {
        filledPolygonDrawer.polygon(vertices, offset, count, color)
    }

    /**
     * Draws a filled polygon used the specified vertices.
     *
     * @param vertices consecutive ordered pairs of the x-y coordinates of the vertices of the
     *   polygon
     * @param triangles ordered triples of the indices of the float[] defining the polygon vertices
     *   corresponding to triangles. [Triangulator.computeTriangles] can be used to calculate the
     *   triangles.
     * @param color the color of the polygon
     */
    fun filledPolygon(
        vertices: FloatArray,
        triangles: ShortArray,
        color: Color = batchManager.color
    ) {
        filledPolygonDrawer.polygon(vertices, triangles, color = color)
    }

    private fun isJoinNecessary(thickness: Float) = thickness > 3 * pixelSize

    private fun isJoinNecessary() = isJoinNecessary(thickness)

    private fun estimateSidesRequired(rx: Float, ry: Float) =
        sideEstimator.estimateSidesRequired(pixelSize, rx, ry)

    companion object {
        private val mat4 = Mat4()
        private val trianglePathPoints = FloatArray(6)
        private val rectangleCorners = FloatArray(8)
    }
}

class DefaultSideEstimator : SideEstimator
