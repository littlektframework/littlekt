package com.lehaine.littlekt.file.gltf

import com.lehaine.littlekt.file.ByteSequenceStream
import com.lehaine.littlekt.file.IndexedByteSequenceStream
import com.lehaine.littlekt.math.*

/**
 * Taken from: https://github.com/fabmax/kool/blob/main/kool-core/src/commonMain/kotlin/de/fabmax/kool/modules/gltf/GltfAccessor.kt
 */
internal abstract class GltfAccessorDataStream(val accessor: GltfAccessor) {
    private val elemByteSize: Int
    private val byteStride: Int

    private val buffer: GltfBufferView? = accessor.bufferViewRef
    private val stream: IndexedByteSequenceStream? = if (buffer != null) {
        IndexedByteSequenceStream(buffer.bufferRef.data, accessor.byteOffset + buffer.byteOffset)
    } else {
        null
    }

    private val sparseIndexStream: IndexedByteSequenceStream?
    private val sparseValueStream: IndexedByteSequenceStream?
    private val sparseIndexType: Int
    private var nextSparseIndex: Int

    var index: Int = 0
        set(value) {
            field = value
            stream?.index = value * byteStride
        }

    init {

        if (accessor.sparse != null) {
            sparseIndexStream = IndexedByteSequenceStream(
                accessor.sparse.indices.bufferViewRef.bufferRef.data, accessor.sparse.indices.bufferViewRef.byteOffset
            )
            sparseValueStream = IndexedByteSequenceStream(
                accessor.sparse.values.bufferViewRef.bufferRef.data, accessor.sparse.values.bufferViewRef.byteOffset
            )
            sparseIndexType = accessor.sparse.indices.componentType
            nextSparseIndex = sparseIndexStream.nextIntComponent(sparseIndexType)
        } else {
            sparseIndexStream = null
            sparseValueStream = null
            sparseIndexType = 0
            nextSparseIndex = -1
        }

        val compByteSize = when (accessor.componentType) {
            GltfAccessor.COMP_TYPE_BYTE -> 1
            GltfAccessor.COMP_TYPE_UNSIGNED_BYTE -> 1
            GltfAccessor.COMP_TYPE_SHORT -> 2
            GltfAccessor.COMP_TYPE_UNSIGNED_SHORT -> 2
            GltfAccessor.COMP_TYPE_INT -> 4
            GltfAccessor.COMP_TYPE_UNSIGNED_INT -> 4
            GltfAccessor.COMP_TYPE_FLOAT -> 4
            else -> throw IllegalArgumentException("Unknown accessor component type: ${accessor.componentType}")
        }
        val numComponents = when (accessor.type) {
            GltfAccessor.TYPE_SCALAR -> 1
            GltfAccessor.TYPE_VEC2 -> 2
            GltfAccessor.TYPE_VEC3 -> 3
            GltfAccessor.TYPE_VEC4 -> 4
            GltfAccessor.TYPE_MAT2 -> 4
            GltfAccessor.TYPE_MAT3 -> 9
            GltfAccessor.TYPE_MAT4 -> 16
            else -> throw IllegalArgumentException("Unsupported accessor type: ${accessor.type}")
        }
        // fixme: some mat types require padding (also depending on component type) which is currently not considered
        elemByteSize = compByteSize * numComponents
        byteStride = if (buffer != null && buffer.byteStride > 0) {
            buffer.byteStride
        } else {
            elemByteSize
        }
    }

    private fun selectDataStream() = if (index != nextSparseIndex) stream else sparseValueStream

    protected fun nextInt(): Int {
        if (index < accessor.count) {
            return selectDataStream()?.nextIntComponent(accessor.componentType) ?: 0
        } else {
            throw IndexOutOfBoundsException("Accessor overflow")
        }
    }

    protected fun nextFloat(): Float {
        if (accessor.componentType == GltfAccessor.COMP_TYPE_FLOAT) {
            if (index < accessor.count) {
                return selectDataStream()?.readFloat() ?: 0f
            } else {
                throw IndexOutOfBoundsException("Accessor overflow")
            }
        } else {
            // implicitly convert int type to normalized float
            return nextInt() / when (accessor.componentType) {
                GltfAccessor.COMP_TYPE_BYTE -> 128f
                GltfAccessor.COMP_TYPE_UNSIGNED_BYTE -> 255f
                GltfAccessor.COMP_TYPE_SHORT -> 32767f
                GltfAccessor.COMP_TYPE_UNSIGNED_SHORT -> 65535f
                GltfAccessor.COMP_TYPE_INT -> 2.14748365E9f
                GltfAccessor.COMP_TYPE_UNSIGNED_INT -> 4.2949673E9f
                else -> throw IllegalStateException("Unknown component type: ${accessor.componentType}")
            }
        }
    }

    private fun ByteSequenceStream.nextIntComponent(componentType: Int): Int {
        return when (componentType) {
            GltfAccessor.COMP_TYPE_BYTE -> readByte()
            GltfAccessor.COMP_TYPE_UNSIGNED_BYTE -> readUByte()
            GltfAccessor.COMP_TYPE_SHORT -> readShort()
            GltfAccessor.COMP_TYPE_UNSIGNED_SHORT -> readUShort()
            GltfAccessor.COMP_TYPE_INT -> readInt()
            GltfAccessor.COMP_TYPE_UNSIGNED_INT -> readUInt()
            else -> throw IllegalArgumentException("Invalid component type: $componentType")
        }
    }

    protected fun advance() {
        if (index == nextSparseIndex && sparseIndexStream?.hasRemaining() == true) {
            nextSparseIndex = sparseIndexStream.nextIntComponent(sparseIndexType)
        }
        index++
    }
}


/**
 * Utility class to retrieve scalar integer values from an accessor. The provided accessor must have a non floating
 * point component type (byte, short or int either signed or unsigned) and must be of type SCALAR.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class IntAccessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessor.TYPE_SCALAR) {
            throw IllegalArgumentException("IntAccessor requires accessor type ${GltfAccessor.TYPE_SCALAR}, provided was ${accessor.type}")
        }
        if (accessor.componentType !in GltfAccessor.COMP_INT_TYPES) {
            throw IllegalArgumentException("IntAccessor requires a (byte / short / int) component type, provided was ${accessor.componentType}")
        }
    }

    fun next(): Int {
        if (index < accessor.count) {
            val i = nextInt()
            advance()
            return i
        } else {
            throw IndexOutOfBoundsException("Accessor overflow")
        }
    }
}

/**
 * Utility class to retrieve scalar float values from an accessor. The provided accessor must have a float component
 * type and must be of type SCALAR.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class FloatAccessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessor.TYPE_SCALAR) {
            throw IllegalArgumentException("Vec2fAccessor requires accessor type ${GltfAccessor.TYPE_SCALAR}, provided was ${accessor.type}")
        }
    }

    fun next(): Float {
        val f = nextFloat()
        advance()
        return f
    }
}

/**
 * Utility class to retrieve Vec2 float values from an accessor. The provided accessor must have a float component type
 * and must be of type VEC2.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class Vec2fAccessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessor.TYPE_VEC2) {
            throw IllegalArgumentException("Vec2fAccessor requires accessor type ${GltfAccessor.TYPE_VEC2}, provided was ${accessor.type}")
        }
    }

    fun next(): MutableVec2f = next(MutableVec2f())

    fun next(result: MutableVec2f): MutableVec2f {
        result.x = nextFloat()
        result.y = nextFloat()
        advance()
        return result
    }
}

/**
 * Utility class to retrieve Vec3 float values from an accessor. The provided accessor must have a float component type
 * and must be of type VEC3.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class Vec3fAccessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessor.TYPE_VEC3) {
            throw IllegalArgumentException("Vec3fAccessor requires accessor type ${GltfAccessor.TYPE_VEC3}, provided was ${accessor.type}")
        }
//        if (accessor.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
//            throw IllegalArgumentException("Vec3fAccessor requires a float component type, provided was ${accessor.componentType}")
//        }
    }

    fun next(): MutableVec3f = next(MutableVec3f())

    fun next(result: MutableVec3f): MutableVec3f {
        result.x = nextFloat()
        result.y = nextFloat()
        result.z = nextFloat()
        advance()
        return result
    }
}

/**
 * Utility class to retrieve Vec4 float values from an accessor. The provided accessor must have a float component type
 * and must be of type VEC4.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class Vec4fAccessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessor.TYPE_VEC4) {
            throw IllegalArgumentException("Vec4fAccessor requires accessor type ${GltfAccessor.TYPE_VEC4}, provided was ${accessor.type}")
        }
//        if (accessor.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
//            throw IllegalArgumentException("Vec4fAccessor requires a float component type, provided was ${accessor.componentType}")
//        }
    }

    fun next(): MutableVec4f = next(MutableVec4f())

    fun next(result: MutableVec4f): MutableVec4f {
        result.x = nextFloat()
        result.y = nextFloat()
        result.z = nextFloat()
        result.w = nextFloat()
        advance()
        return result
    }
}

/**
 * Utility class to retrieve Vec4 int values from an accessor. The provided accessor must have a non floating
 * point component type (byte, short or int either signed or unsigned) and must be of type VEC4.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class Vec4iAccessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessor.TYPE_VEC4) {
            throw IllegalArgumentException("Vec4iAccessor requires accessor type ${GltfAccessor.TYPE_VEC4}, provided was ${accessor.type}")
        }
        if (accessor.componentType !in GltfAccessor.COMP_INT_TYPES) {
            throw IllegalArgumentException("Vec4fAccessor requires a (byte / short / int) component type, provided was ${accessor.componentType}")
        }
    }

    fun next(): MutableVec4i = next(MutableVec4i())

    fun next(result: MutableVec4i): MutableVec4i {
        result.x = nextInt()
        result.y = nextInt()
        result.z = nextInt()
        result.w = nextInt()
        advance()
        return result
    }
}


/**
 * Utility class to retrieve Mat4 float values from an accessor. The provided accessor must have a float component type
 * and must be of type MAT4.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class Mat4Accessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessor.TYPE_MAT4) {
            throw IllegalArgumentException("Mat4fAccessor requires accessor type ${GltfAccessor.TYPE_MAT4}, provided was ${accessor.type}")
        }
        if (accessor.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
            throw IllegalArgumentException("Mat4fAccessor requires a float component type, provided was ${accessor.componentType}")
        }
    }

    fun next(): Mat4 = next(Mat4())

    fun next(result: Mat4): Mat4 {
        for (i in 0..15) {
            result.data[i] = nextFloat()
        }
        advance()
        return result
    }
}