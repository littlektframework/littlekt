package com.lehaine.littlekt.graphics.util

import com.lehaine.littlekt.graphics.MutableColor
import com.lehaine.littlekt.graphics.VertexAttrUsage
import com.lehaine.littlekt.graphics.VertexAttribute
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.MutableVec3f
import com.lehaine.littlekt.math.MutableVec4f
import com.lehaine.littlekt.math.MutableVec4i

/**
 * A view of a vertex within the geometry [MeshGeometry].
 *
 * Based the [kool](https://github.com/fabmax/kool/blob/main/kool-core/src/commonMain/kotlin/de/fabmax/kool/scene/geometry/VertexView.kt) implementation.
 * @param geometry the geometry, vertices & indices, of the mesh
 * @param index the initial vertex index to view
 */
class VertexView(val geometry: MeshGeometry, index: Int) : MutableVec3f() {
    val position: MutableVec3f
    val colorPacked: FloatView
    val color: MutableColor
    val texCoords: MutableVec2f
    val normal: MutableVec3f
    val joints: MutableVec4i
    val weights: MutableVec4f

    var index = index
        set(value) {
            field = value
            offset = value * geometry.vertexSize
        }

    private val _attributesViews = mutableMapOf<Int, Any>()
    val attributeViews: Map<Int, Any> get() = _attributesViews

    private var offset = index * geometry.vertexSize


    init {
        geometry.attributes.forEach { attribute ->
            when {
                attribute.normalized || attribute.numComponents == 1 -> {
                    _attributesViews[attribute.key] = FloatView(attribute.offset / 4)
                }

                attribute.numComponents == 4 -> {
                    if(attribute.asInt) {
                        _attributesViews[attribute.key] = Vec4iView(attribute.offset / 4)
                    } else {
                        _attributesViews[attribute.key] = Vec4fView(attribute.offset / 4)
                    }
                }

                attribute.numComponents == 3 || attribute.usage == VertexAttrUsage.POSITION -> {
                    _attributesViews[attribute.key] = Vec3fView(attribute.offset / 4, attribute.numComponents)
                }

                attribute.numComponents == 2 -> {
                    _attributesViews[attribute.key] = Vec2fView(attribute.offset / 4)
                }


                else -> throw IllegalArgumentException("${attribute.type} is not a valid vertex attribute.")
            }
        }

        val posAttribute = geometry.attributes.findByUsage(VertexAttrUsage.POSITION) ?: VertexAttribute.POSITION
        position = getVec3fAttribute(posAttribute) ?: Vec3fView(-1, posAttribute.numComponents)
        normal = getVec3fAttribute(VertexAttribute.NORMAL) ?: Vec3fView(-1, 0)
        colorPacked = getFloatAttribute(VertexAttribute.COLOR_PACKED) ?: FloatView(-1)
        color = getColorAttribute(VertexAttribute.COLOR_UNPACKED) ?: ColorWrapView(Vec4fView(-1))
        texCoords = getVec2fAttribute(VertexAttribute.TEX_COORDS(0)) ?: Vec2fView(-1)
        joints = getVec4iAttribute(VertexAttribute.JOINT) ?: Vec4iView(-1)
        weights = getVec4fAttribute(VertexAttribute.WEIGHT) ?: Vec4fView(-1)

    }

    override fun get(i: Int) = position[i]

    override fun set(i: Int, v: Float) {
        position[i] = v
    }

    fun resetToZero() {
        for (key in attributeViews.keys) {
            when (val attrib = attributeViews[key]) {
                is FloatView -> attrib.value = 0f
                is ColorWrapView -> attrib.set(0f, 0f, 0f, 0f)
                is Vec2fView -> attrib.set(0f, 0f)
                is Vec3fView -> attrib.set(0f, 0f, 0f)
                is Vec4fView -> attrib.set(0f, 0f, 0f, 0f)
            }

        }
    }

    fun set(other: VertexView) {
        for (attrib in attributeViews.keys) {
            val view = other.attributeViews[attrib]
            if (view != null) {
                when (view) {
                    is FloatView -> (attributeViews[attrib] as FloatView).value = view.value
                    is Vec2fView -> (attributeViews[attrib] as Vec2fView).set(view)
                    is Vec3fView -> (attributeViews[attrib] as Vec3fView).set(view)
                    is Vec4fView -> (attributeViews[attrib] as Vec4fView).set(view)
                }
            }
        }
    }

    fun getFloatAttribute(attribute: VertexAttribute): FloatView? = attributeViews[attribute.key] as FloatView?
    fun getVec2fAttribute(attribute: VertexAttribute): MutableVec2f? = attributeViews[attribute.key] as MutableVec2f?
    fun getVec3fAttribute(attribute: VertexAttribute): MutableVec3f? = attributeViews[attribute.key] as MutableVec3f?
    fun getVec4fAttribute(attribute: VertexAttribute): MutableVec4f? = attributeViews[attribute.key] as MutableVec4f?
    fun getVec4iAttribute(attribute: VertexAttribute): MutableVec4i? = attributeViews[attribute.key] as MutableVec4i?
    fun getColorAttribute(attribute: VertexAttribute): MutableColor? =
        attributeViews[attribute.key]?.let { ColorWrapView(it as Vec4fView) }

    inner class FloatView(private val attribOffset: Int) {
        var value: Float
            get() = if (attribOffset < 0) {
                0f
            } else {
                geometry.vertices[offset + attribOffset]
            }
            set(value) {
                if (attribOffset >= 0) {
                    geometry.vertices[offset + attribOffset] = value
                }
            }
    }

    private inner class Vec2fView(private val attribOffset: Int) : MutableVec2f() {
        override operator fun get(i: Int): Float {
            return if (attribOffset >= 0 && i in 0..1) {
                geometry.vertices[offset + attribOffset + i]
            } else {
                0f
            }
        }

        override operator fun set(i: Int, v: Float) {
            if (attribOffset >= 0 && i in 0..1) {
                geometry.vertices[offset + attribOffset + i] = v
            }
        }
    }

    /**
     * @param numComponents A special use case to handle setting position with either 2 or 3 components
     */
    private inner class Vec3fView(val attribOffset: Int, val numComponents: Int) : MutableVec3f() {
        override operator fun get(i: Int): Float {
            return if (attribOffset >= 0 && i in 0 until numComponents) {
                geometry.vertices[offset + attribOffset + i]
            } else {
                0f
            }
        }

        override operator fun set(i: Int, v: Float) {
            if (attribOffset >= 0 && i in 0 until numComponents) {
                geometry.vertices[offset + attribOffset + i] = v
            }
        }
    }

    private inner class Vec4fView(val attribOffset: Int) : MutableVec4f() {
        override operator fun get(i: Int): Float {
            return if (attribOffset >= 0 && i in 0..3) {
                geometry.vertices[offset + attribOffset + i]
            } else {
                0f
            }
        }

        override operator fun set(i: Int, v: Float) {
            if (attribOffset >= 0 && i in 0..3) {
                geometry.vertices[offset + attribOffset + i] = v
            }
        }
    }

    private inner class Vec4iView(val attribOffset: Int) : MutableVec4i() {
        override operator fun get(i: Int): Int {
            return if (attribOffset >= 0 && i in 0..3) {
                geometry.vertices[offset + attribOffset + i].toInt()
            } else {
                0
            }
        }

        override operator fun set(i: Int, v: Int) {
            if (attribOffset >= 0 && i in 0..3) {
                geometry.vertices[offset + attribOffset + i] = v.toFloat()
            }
        }
    }

    private inner class ColorWrapView(val vecView: Vec4fView) : MutableColor() {
        override operator fun get(i: Int) = vecView[i]
        override operator fun set(i: Int, v: Float) {
            vecView[i] = v
        }
    }
}
