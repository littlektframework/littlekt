package com.littlekt.graphics.util

import com.littlekt.graphics.Color
import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.math.*
import com.littlekt.math.geom.Angle

/**
 * A helper class to build common geometry within a [CommonIndexedMeshGeometry].
 *
 * Based off:
 * https://github.com/fabmax/kool/blob/main/kool-core/src/commonMain/kotlin/de/fabmax/kool/scene/geometry/MeshBuilder.kt
 *
 * @param geometry the list to build the geometry to.
 */
class CommonIndexedMeshBuilder(val geometry: CommonIndexedMeshGeometry) {

    val hasNormals = geometry.layout.attributes.any { it.usage == VertexAttrUsage.NORMAL }
    val transform = Mat4Stack()
    var isInvertFaceOrientation = false

    var color = Color.GRAY
    var vertexModFun: (CommonVertexView.() -> Unit)? = null

    /** Add a vertex to the [geometry]. */
    inline fun vertex(block: CommonVertexView.() -> Unit): Int {
        return geometry.addVertex {
            color.set(this@CommonIndexedMeshBuilder.color)
            block()

            transform.transform(position)
            if (hasNormals && normal.sqrLength() != 0f) {
                transform.transform(normal, 0f)
                normal.norm()
            }
            vertexModFun?.invoke(this)
        }
    }

    /** Sets the vertex directly to the [geometry]. */
    fun vertex(pos: Vec3f, nrm: Vec3f, uv: Vec2f = Vec2f.ZERO) = vertex {
        position.set(pos)
        normal.set(nrm)
        texCoords.set(uv)
    }

    fun addTriIndices(i0: Int, i1: Int, i2: Int) {
        if (isInvertFaceOrientation) {
            geometry.addTriIndices(i2, i1, i0)
        } else {
            geometry.addTriIndices(i0, i1, i2)
        }
    }

    /** Pushes a [transform] and then pops outside the block. */
    inline fun withTransform(block: CommonIndexedMeshBuilder.() -> Unit) {
        transform.push()
        this.block()
        transform.pop()
    }

    inline fun withColor(color: Color, block: CommonIndexedMeshBuilder.() -> Unit) {
        val c = this.color
        this.color = color
        this.block()
        this.color = c
    }

    fun clear() {
        geometry.clearVertices()
        identity()
    }

    fun identity() = transform.setToIdentity()

    fun translate(t: Vec3f) = transform.translate(t.x, t.y, t.z)

    fun translate(x: Float, y: Float, z: Float) = transform.translate(x, y, z)

    fun rotate(angle: Angle, axis: Vec3f) = transform.rotate(axis, angle)

    fun rotate(angle: Angle, axX: Float, axY: Float, axZ: Float) =
        transform.rotate(axX, axY, axZ, angle)

    fun rotate(eulerX: Angle, eulerY: Angle, eulerZ: Angle) =
        transform.rotate(eulerX, eulerY, eulerZ)

    fun scale(s: Float) = transform.scale(s, s, s)

    fun scale(x: Float, y: Float, z: Float) = transform.scale(x, y, z)

    inline fun cube(block: CubeProps.() -> Unit = {}) {
        val props = CubeProps()
        props.block()
        cube(props)
    }

    /** Generates a cube geometry. */
    fun cube(props: CubeProps) {
        val tmpPos = MutableVec3f()
        props.fixNegativeSize()

        // front
        withColor(props.frontColor ?: color) {
            val i0 =
                vertex(
                    tmpPos.set(props.origin.x, props.origin.y, props.origin.z + props.size.z),
                    Vec3f.Z_AXIS,
                    Vec2f(0f, 1f),
                )
            val i1 =
                vertex(
                    tmpPos.set(
                        props.origin.x + props.size.x,
                        props.origin.y,
                        props.origin.z + props.size.z,
                    ),
                    Vec3f.Z_AXIS,
                    Vec2f(1f, 1f),
                )
            val i2 =
                vertex(
                    tmpPos.set(
                        props.origin.x + props.size.x,
                        props.origin.y + props.size.y,
                        props.origin.z + props.size.z,
                    ),
                    Vec3f.Z_AXIS,
                    Vec2f(1f, 0f),
                )
            val i3 =
                vertex(
                    tmpPos.set(
                        props.origin.x,
                        props.origin.y + props.size.y,
                        props.origin.z + props.size.z,
                    ),
                    Vec3f.Z_AXIS,
                    Vec2f(0f, 0f),
                )
            addTriIndices(i0, i1, i2)
            addTriIndices(i0, i2, i3)
        }

        // right
        withColor(props.rightColor ?: color) {
            val i0 =
                vertex(
                    tmpPos.set(props.origin.x + props.size.x, props.origin.y, props.origin.z),
                    Vec3f.X_AXIS,
                    Vec2f(1f, 1f),
                )
            val i1 =
                vertex(
                    tmpPos.set(
                        props.origin.x + props.size.x,
                        props.origin.y + props.size.y,
                        props.origin.z,
                    ),
                    Vec3f.X_AXIS,
                    Vec2f(1f, 0f),
                )
            val i2 =
                vertex(
                    tmpPos.set(
                        props.origin.x + props.size.x,
                        props.origin.y + props.size.y,
                        props.origin.z + props.size.z,
                    ),
                    Vec3f.X_AXIS,
                    Vec2f(0f, 0f),
                )
            val i3 =
                vertex(
                    tmpPos.set(
                        props.origin.x + props.size.x,
                        props.origin.y,
                        props.origin.z + props.size.z,
                    ),
                    Vec3f.X_AXIS,
                    Vec2f(0f, 1f),
                )
            addTriIndices(i0, i1, i2)
            addTriIndices(i0, i2, i3)
        }

        // back
        withColor(props.backColor ?: color) {
            val i0 =
                vertex(
                    tmpPos.set(props.origin.x, props.origin.y + props.size.y, props.origin.z),
                    Vec3f.NEG_Z_AXIS,
                    Vec2f(1f, 0f),
                )
            val i1 =
                vertex(
                    tmpPos.set(
                        props.origin.x + props.size.x,
                        props.origin.y + props.size.y,
                        props.origin.z,
                    ),
                    Vec3f.NEG_Z_AXIS,
                    Vec2f(0f, 0f),
                )
            val i2 =
                vertex(
                    tmpPos.set(props.origin.x + props.size.x, props.origin.y, props.origin.z),
                    Vec3f.NEG_Z_AXIS,
                    Vec2f(0f, 1f),
                )
            val i3 =
                vertex(
                    tmpPos.set(props.origin.x, props.origin.y, props.origin.z),
                    Vec3f.NEG_Z_AXIS,
                    Vec2f(1f, 1f),
                )
            addTriIndices(i0, i1, i2)
            addTriIndices(i0, i2, i3)
        }

        // left
        withColor(props.leftColor ?: color) {
            val i0 =
                vertex(
                    tmpPos.set(props.origin.x, props.origin.y, props.origin.z + props.size.z),
                    Vec3f.NEG_X_AXIS,
                    Vec2f(1f, 1f),
                )
            val i1 =
                vertex(
                    tmpPos.set(
                        props.origin.x,
                        props.origin.y + props.size.y,
                        props.origin.z + props.size.z,
                    ),
                    Vec3f.NEG_X_AXIS,
                    Vec2f(1f, 0f),
                )
            val i2 =
                vertex(
                    tmpPos.set(props.origin.x, props.origin.y + props.size.y, props.origin.z),
                    Vec3f.NEG_X_AXIS,
                    Vec2f(0f, 0f),
                )
            val i3 =
                vertex(
                    tmpPos.set(props.origin.x, props.origin.y, props.origin.z),
                    Vec3f.NEG_X_AXIS,
                    Vec2f(0f, 1f),
                )
            addTriIndices(i0, i1, i2)
            addTriIndices(i0, i2, i3)
        }

        // top
        withColor(props.topColor ?: color) {
            val i0 =
                vertex(
                    tmpPos.set(
                        props.origin.x,
                        props.origin.y + props.size.y,
                        props.origin.z + props.size.z,
                    ),
                    Vec3f.Y_AXIS,
                    Vec2f(0f, 1f),
                )
            val i1 =
                vertex(
                    tmpPos.set(
                        props.origin.x + props.size.x,
                        props.origin.y + props.size.y,
                        props.origin.z + props.size.z,
                    ),
                    Vec3f.Y_AXIS,
                    Vec2f(1f, 1f),
                )
            val i2 =
                vertex(
                    tmpPos.set(
                        props.origin.x + props.size.x,
                        props.origin.y + props.size.y,
                        props.origin.z,
                    ),
                    Vec3f.Y_AXIS,
                    Vec2f(1f, 0f),
                )
            val i3 =
                vertex(
                    tmpPos.set(props.origin.x, props.origin.y + props.size.y, props.origin.z),
                    Vec3f.Y_AXIS,
                    Vec2f(0f, 0f),
                )
            addTriIndices(i0, i1, i2)
            addTriIndices(i0, i2, i3)
        }

        // bottom
        withColor(props.bottomColor ?: color) {
            val i0 =
                vertex(
                    tmpPos.set(props.origin.x, props.origin.y, props.origin.z),
                    Vec3f.NEG_Y_AXIS,
                    Vec2f(0f, 1f),
                )
            val i1 =
                vertex(
                    tmpPos.set(props.origin.x + props.size.x, props.origin.y, props.origin.z),
                    Vec3f.NEG_Y_AXIS,
                    Vec2f(1f, 1f),
                )
            val i2 =
                vertex(
                    tmpPos.set(
                        props.origin.x + props.size.x,
                        props.origin.y,
                        props.origin.z + props.size.z,
                    ),
                    Vec3f.NEG_Y_AXIS,
                    Vec2f(1f, 0f),
                )
            val i3 =
                vertex(
                    tmpPos.set(props.origin.x, props.origin.y, props.origin.z + props.size.z),
                    Vec3f.NEG_Y_AXIS,
                    Vec2f(0f, 0f),
                )
            addTriIndices(i0, i1, i2)
            addTriIndices(i0, i2, i3)
        }
    }

    /** Generates a grid geometry. */
    inline fun grid(block: GridProps.() -> Unit = {}) {
        val props = GridProps()
        props.block()
        grid(props)
    }

    /** Generates a grid geometry. */
    fun grid(props: GridProps) {
        val gridNormal = MutableVec3f()

        val bx = -props.sizeX / 2
        val by = -props.sizeY / 2
        val sx = props.sizeX / props.stepsX
        val sy = props.sizeY / props.stepsY
        val nx = props.stepsX + 1
        props.xDir.cross(props.yDir, gridNormal).norm()

        for (y in 0..props.stepsY) {
            for (x in 0..props.stepsX) {
                val px = bx + x * sx
                val py = by + y * sy
                val h = props.heightFun(x, y)

                val idx = vertex {
                    position.set(props.center)
                    position.x += props.xDir.x * px + props.yDir.x * py + gridNormal.x * h
                    position.y += props.xDir.y * px + props.yDir.y * py + gridNormal.y * h
                    position.z += props.xDir.z * px + props.yDir.z * py + gridNormal.z * h
                    texCoords.set(
                        x / props.stepsX.toFloat() * props.texCoordScale.x + props.texCoordOffset.x,
                        (1f - y / props.stepsY.toFloat()) * props.texCoordScale.y +
                            props.texCoordOffset.y,
                    )
                }

                if (x > 0 && y > 0) {
                    if (x % 2 == y % 2) {
                        addTriIndices(idx - nx - 1, idx, idx - 1)
                        addTriIndices(idx - nx, idx, idx - nx - 1)
                    } else {
                        addTriIndices(idx - nx, idx, idx - 1)
                        addTriIndices(idx - nx, idx - 1, idx - nx - 1)
                    }
                }
            }
        }

        val iTri = geometry.numIndices - props.stepsX * props.stepsY * 6
        val e1 = MutableVec3f()
        val e2 = MutableVec3f()
        val v1 = geometry[0]
        val v2 = geometry[0]
        val v3 = geometry[0]
        for (i in iTri until geometry.numIndices step 3) {
            v1.index = geometry.indices[i].toInt()
            v2.index = geometry.indices[i + 1].toInt()
            v3.index = geometry.indices[i + 2].toInt()
            v2.position.subtract(v1.position, e1).norm()
            v3.position.subtract(v1.position, e2).norm()
            e1.cross(e2, gridNormal).norm()
            v1.normal.add(gridNormal)
            v2.normal.add(gridNormal)
            v3.normal.add(gridNormal)
        }

        val iVert = geometry.numVertices - (props.stepsX + 1) * (props.stepsY + 1)
        for (i in iVert until geometry.numVertices) {
            v1.index = i
            v1.normal.norm()
        }
    }

    class CubeProps {
        val origin = MutableVec3f()
        val size = MutableVec3f(1f, 1f, 1f)

        var width: Float
            get() = size.x
            set(value) {
                size.x = value
            }

        var height: Float
            get() = size.y
            set(value) {
                size.y = value
            }

        var depth: Float
            get() = size.z
            set(value) {
                size.z = value
            }

        var topColor: Color? = null
        var bottomColor: Color? = null
        var leftColor: Color? = null
        var rightColor: Color? = null
        var frontColor: Color? = null
        var backColor: Color? = null

        fun fixNegativeSize() {
            if (size.x < 0) {
                origin.x += size.x
                size.x = -size.x
            }
            if (size.y < 0) {
                origin.y += size.y
                size.y = -size.y
            }
            if (size.z < 0) {
                origin.z += size.z
                size.z = -size.z
            }
        }

        fun centered() {
            origin.x -= size.x / 2f
            origin.y -= size.y / 2f
            origin.z -= size.z / 2f
        }

        fun colored(linearSpace: Boolean = true) {
            if (linearSpace) {
                frontColor = Color.RED.toLinear()
                rightColor = Color.ORANGE.toLinear()
                backColor = Color.BLUE.toLinear()
                leftColor = Color.CYAN.toLinear()
                topColor = Color.MAGENTA.toLinear()
                bottomColor = Color.GREEN.toLinear()
            } else {
                frontColor = Color.RED
                rightColor = Color.ORANGE
                backColor = Color.BLUE
                leftColor = Color.CYAN
                topColor = Color.MAGENTA
                bottomColor = Color.GREEN
            }
        }
    }

    class GridProps {
        val center = MutableVec3f()
        val xDir = MutableVec3f(Vec3f.X_AXIS)
        val yDir = MutableVec3f(Vec3f.NEG_Z_AXIS)
        val texCoordOffset = MutableVec2f(0f, 0f)
        val texCoordScale = MutableVec2f(1f, 1f)
        var sizeX = 10f
        var sizeY = 10f
        var stepsX = 10
        var stepsY = 10
        var heightFun: (Int, Int) -> Float = ZERO_HEIGHT

        companion object {
            val ZERO_HEIGHT: (Int, Int) -> Float = { _, _ -> 0f }
        }
    }
}
