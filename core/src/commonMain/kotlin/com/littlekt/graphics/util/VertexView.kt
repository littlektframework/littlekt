package com.littlekt.graphics.util

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.webgpu.WebGPUVertexAttribute
import com.littlekt.math.*

/**
 * A view of a vertex within the geometry [IndexedMeshGeometry]. If there are multiple vertex usages
 * of the same type then a new view into that type of vertex needs to be created.
 *
 * Based the
 * [kool](https://github.com/fabmax/kool/blob/main/kool-core/src/commonMain/kotlin/de/fabmax/kool/scene/geometry/VertexView.kt)
 * implementation.
 *
 * @param vertexSize the size of the vertex
 * @param vertices the raw vertices
 * @param attributes the attributes of the vertex
 * @param index the initial vertex index to view
 */
open class VertexView(
    val vertexSize: Int,
    var vertices: FloatBuffer,
    val attributes: List<VertexAttribute>,
    index: Int,
) {

    /** The current vertex index that is being viewed. */
    var index = index
        set(value) {
            field = value
            offset = value * vertexSize
        }

    private val _attributesViews = mutableMapOf<Int, Any>()

    /** A view into the vertex attribute, mapped by [WebGPUVertexAttribute.shaderLocation]. */
    val attributeViews: Map<Int, Any>
        get() = _attributesViews

    private var offset = index * vertexSize

    init {
        attributes.forEach { attribute ->
            when (attribute.format.components) {
                1 -> {
                    if (attribute.format.isInt) {
                        _attributesViews[attribute.key] = IntView(attribute.offset.toInt() / 4)
                    } else {
                        _attributesViews[attribute.key] = FloatView(attribute.offset.toInt() / 4)
                    }
                }
                4 -> {
                    if (attribute.format.isInt) {
                        _attributesViews[attribute.key] = Vec4iView(attribute.offset.toInt() / 4)
                    } else {
                        _attributesViews[attribute.key] = Vec4fView(attribute.offset.toInt() / 4)
                    }
                }
                3 -> {
                    if (attribute.format.isInt) {

                        _attributesViews[attribute.key] = Vec3iView(attribute.offset.toInt() / 4)
                    } else {
                        _attributesViews[attribute.key] = Vec3fView(attribute.offset.toInt() / 4)
                    }
                }
                2 -> {
                    if (attribute.format.isInt) {
                        _attributesViews[attribute.key] = Vec2iView(attribute.offset.toInt() / 4)
                    } else {
                        _attributesViews[attribute.key] = Vec2fView(attribute.offset.toInt() / 4)
                    }
                }
                else ->
                    throw IllegalArgumentException(
                        "${attribute.format} is not a valid vertex attribute."
                    )
            }
        }
    }

    /** Resets all views values to zero. */
    open fun resetToZero() {
        for (key in attributeViews.keys) {
            when (val attrib = attributeViews[key]) {
                is FloatView -> attrib.value = 0f
                is IntView -> attrib.value = 0
                is Vec2fView -> attrib.set(0f, 0f)
                is Vec2iView -> attrib.setAll(0, 0)
                is Vec3fView -> attrib.set(0f, 0f, 0f)
                is Vec3iView -> attrib.set(0, 0, 0)
                is Vec4fView -> attrib.set(0f, 0f, 0f, 0f)
                is Vec4iView -> attrib.set(0, 0, 0, 0)
            }
        }
    }

    fun set(other: VertexView) {
        for (attrib in attributeViews.keys) {
            val view = other.attributeViews[attrib]
            if (view != null) {
                when (view) {
                    is FloatView -> (attributeViews[attrib] as FloatView).value = view.value
                    is IntView -> (attributeViews[attrib] as IntView).value = view.value
                    is ColorWrapView -> (attributeViews[attrib] as ColorWrapView).set(view)
                    is Vec2fView -> (attributeViews[attrib] as Vec2fView).set(view)
                    is Vec3fView -> (attributeViews[attrib] as Vec3fView).set(view)
                    is Vec4fView -> (attributeViews[attrib] as Vec4fView).set(view)
                    is Vec2iView -> (attributeViews[attrib] as Vec2iView).set(view)
                    is Vec3iView -> (attributeViews[attrib] as Vec3iView).set(view)
                    is Vec4iView -> (attributeViews[attrib] as Vec4iView).set(view)
                }
            }
        }
    }

    /** @return a [FloatView] of the given [VertexAttribute], if it exists; `null` otherwise. */
    fun getFloatAttribute(attribute: VertexAttribute): FloatView? =
        attributeViews[attribute.key] as FloatView?

    /** @return a [IntView] of the given [VertexAttribute], if it exists; `null` otherwise. */
    fun getIntAttribute(attribute: VertexAttribute): IntView? =
        attributeViews[attribute.shaderLocation] as IntView?

    /** @return a [MutableVec2f] of the given [VertexAttribute], if it exists; `null` otherwise. */
    fun getVec2fAttribute(attribute: VertexAttribute): MutableVec2f? =
        attributeViews[attribute.key] as MutableVec2f?

    /** @return a [MutableVec3f] of the given [VertexAttribute], if it exists; `null` otherwise. */
    fun getVec3fAttribute(attribute: VertexAttribute): MutableVec3f? =
        attributeViews[attribute.key] as MutableVec3f?

    /** @return a [MutableVec4f] of the given [VertexAttribute], if it exists; `null` otherwise. */
    fun getVec4fAttribute(attribute: VertexAttribute): MutableVec4f? =
        attributeViews[attribute.key] as MutableVec4f?

    /** @return a [MutableVec4i] of the given [VertexAttribute], if it exists; `null` otherwise. */
    fun getVec4iAttribute(attribute: VertexAttribute): MutableVec4i? =
        attributeViews[attribute.key] as MutableVec4i?

    /** @return a [MutableColor] of the given [VertexAttribute], if it exists; `null` otherwise. */
    fun getColorAttribute(attribute: VertexAttribute): MutableColor? =
        attributeViews[attribute.key]?.let { ColorWrapView(it as Vec4fView) }

    /** A view wrapper around a [Float]. */
    inner class FloatView(private val attribOffset: Int) {
        var value: Float
            get() =
                if (attribOffset < 0) {
                    0f
                } else {
                    vertices[offset + attribOffset]
                }
            set(value) {
                if (attribOffset >= 0) {
                    vertices[offset + attribOffset] = value
                }
            }
    }

    /** A view wrapper around a [Int]. */
    inner class IntView(private val attribOffset: Int) {
        var value: Int
            get() =
                if (attribOffset < 0) {
                    0
                } else {
                    vertices[offset + attribOffset].toInt()
                }
            set(value) {
                if (attribOffset >= 0) {
                    vertices[offset + attribOffset] = value.toFloat()
                }
            }
    }

    /** A view wrapper around a [Vec2f]. */
    inner class Vec2fView(private val attribOffset: Int) : MutableVec2f() {
        override operator fun get(i: Int): Float {
            return if (attribOffset >= 0 && i in 0..1) {
                vertices[offset + attribOffset + i]
            } else {
                0f
            }
        }

        override operator fun set(i: Int, v: Float) {
            if (attribOffset >= 0 && i in 0..1) {
                vertices[offset + attribOffset + i] = v
            }
        }
    }

    /** A view wrapper around a [Vec2i]. */
    inner class Vec2iView(private val attribOffset: Int) : MutableVec2i() {
        override operator fun get(i: Int): Int {
            return if (attribOffset >= 0 && i in 0..1) {
                vertices[offset + attribOffset + i].toInt()
            } else {
                0
            }
        }

        override operator fun set(i: Int, v: Int) {
            if (attribOffset >= 0 && i in 0..1) {
                vertices[offset + attribOffset + i] = v.toFloat()
            }
        }
    }

    /** A view wrapper around a [Vec3f]. */
    inner class Vec3fView(private val attribOffset: Int) : MutableVec3f() {
        override operator fun get(i: Int): Float {
            return if (attribOffset >= 0 && i in 0..2) {
                vertices[offset + attribOffset + i]
            } else {
                0f
            }
        }

        override operator fun set(i: Int, v: Float) {
            if (attribOffset >= 0 && i in 0..2) {
                vertices[offset + attribOffset + i] = v
            }
        }
    }

    /** A view wrapper around a [Vec3i]. */
    inner class Vec3iView(private val attribOffset: Int) : MutableVec3i() {
        override operator fun get(i: Int): Int {
            return if (attribOffset >= 0 && i in 0..2) {
                vertices[offset + attribOffset + i].toInt()
            } else {
                0
            }
        }

        override operator fun set(i: Int, v: Int) {
            if (attribOffset >= 0 && i in 0..2) {
                vertices[offset + attribOffset + i] = v.toFloat()
            }
        }
    }

    /** A view wrapper around a [Vec4f]. */
    inner class Vec4fView(private val attribOffset: Int) : MutableVec4f() {
        override operator fun get(i: Int): Float {
            return if (attribOffset >= 0 && i in 0..3) {
                vertices[offset + attribOffset + i]
            } else {
                0f
            }
        }

        override operator fun set(i: Int, v: Float) {
            if (attribOffset >= 0 && i in 0..3) {
                vertices[offset + attribOffset + i] = v
            }
        }
    }

    /** A view wrapper around a [Vec4i]. */
    inner class Vec4iView(private val attribOffset: Int) : MutableVec4i() {
        override operator fun get(i: Int): Int {
            return if (attribOffset >= 0 && i in 0..3) {
                vertices[offset + attribOffset + i].toInt()
            } else {
                0
            }
        }

        override operator fun set(i: Int, v: Int) {
            if (attribOffset >= 0 && i in 0..3) {
                vertices[offset + attribOffset + i] = v.toFloat()
            }
        }
    }

    /** A view wrapper around a [MutableColor]. */
    inner class ColorWrapView(private val vecView: Vec4fView) : MutableColor() {
        override operator fun get(i: Int) = vecView[i]

        override operator fun set(i: Int, v: Float) {
            vecView[i] = v
        }
    }
}
