package com.littlekt.file.gltf

import com.littlekt.file.ByteSequenceStream
import com.littlekt.file.IndexedByteSequenceStream
import com.littlekt.math.*

internal abstract class GltfAccessorDataStream(val accessor: GltfAccessor) {
    private val elemByteSize: Int
    private val byteStride: Int

    private val buffer: GltfBufferView? = accessor.bufferViewRef
    private val stream: IndexedByteSequenceStream? =
        if (buffer != null) {
            IndexedByteSequenceStream(
                buffer.bufferRef.data,
                accessor.byteOffset + buffer.byteOffset,
            )
        } else {
            null
        }

    private val sparseIndexStream: IndexedByteSequenceStream?
    private val sparseValueStream: IndexedByteSequenceStream?
    private val sparseIndexType: GltfComponentType
    private var nextSparseIndex: Int

    var index: Int = 0
        set(value) {
            field = value
            stream?.index = value * byteStride
        }

    init {
        if (accessor.sparse != null) {
            sparseIndexStream =
                IndexedByteSequenceStream(
                    accessor.sparse.indices.bufferViewRef.bufferRef.data,
                    accessor.sparse.indices.bufferViewRef.byteOffset,
                )
            sparseValueStream =
                IndexedByteSequenceStream(
                    accessor.sparse.values.bufferViewRef.bufferRef.data,
                    accessor.sparse.values.bufferViewRef.byteOffset,
                )
            sparseIndexType = accessor.sparse.indices.componentType
            nextSparseIndex = sparseIndexStream.nextIntComponent(sparseIndexType)
        } else {
            sparseIndexStream = null
            sparseValueStream = null
            sparseIndexType = GltfComponentType.Unknown
            nextSparseIndex = -1
        }

        val compByteSize = accessor.componentType.byteSize
        val numComponents =
            when (accessor.type) {
                GltfAccessorType.Scalar -> 1
                GltfAccessorType.Vec2 -> 2
                GltfAccessorType.Vec3 -> 3
                GltfAccessorType.Vec4 -> 4
                GltfAccessorType.Mat2 -> 4
                GltfAccessorType.Mat3 -> 9
                GltfAccessorType.Mat4 -> 16
            }
        elemByteSize = compByteSize * numComponents
        byteStride =
            if (buffer != null && buffer.byteStride > 0) {
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
        if (accessor.componentType == GltfComponentType.Float) {
            if (index < accessor.count) {
                return selectDataStream()?.readFloat() ?: 0f
            } else {
                throw IndexOutOfBoundsException("Accessor overflow")
            }
        } else {
            // implicitly convert int type to normalized float
            return nextInt() /
                when (accessor.componentType) {
                    GltfComponentType.Byte -> 128f
                    GltfComponentType.UnsignedByte -> 255f
                    GltfComponentType.Short -> 32767f
                    GltfComponentType.UnsignedShort -> 65535f
                    GltfComponentType.Int -> 2.14748365E9f
                    GltfComponentType.UnsignedInt -> 4.2949673E9f
                    else ->
                        throw IllegalStateException(
                            "Unknown component type: ${accessor.componentType}"
                        )
                }
        }
    }

    private fun ByteSequenceStream.nextIntComponent(componentType: GltfComponentType): Int {
        return when (componentType) {
            GltfComponentType.Byte -> readByte()
            GltfComponentType.UnsignedByte -> readUByte()
            GltfComponentType.Short -> readShort()
            GltfComponentType.UnsignedShort -> readUShort()
            GltfComponentType.Int -> readInt()
            GltfComponentType.UnsignedInt -> readUInt()
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
 * Utility class to retrieve scalar integer values from an accessor. The provided accessor must have
 * a non floating point component type (byte, short or int either signed or unsigned) and must be of
 * type SCALAR.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class GltfIntAccessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessorType.Scalar) {
            throw IllegalArgumentException(
                "GltfIntAccessor requires accessor type ${GltfAccessorType.Scalar.value}, provided was ${accessor.type}"
            )
        }
        if (accessor.componentType !in GltfComponentType.IntTypes) {
            throw IllegalArgumentException(
                "GltfIntAccessor requires a (byte / short / int) component type, provided was ${accessor.componentType}"
            )
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
 * Utility class to retrieve scalar float values from an accessor. The provided accessor must have a
 * float component type and must be of type SCALAR.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class GltfFloatAccessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessorType.Scalar) {
            throw IllegalArgumentException(
                "GltfFloatAccessor requires accessor type ${GltfAccessorType.Scalar.value}, provided was ${accessor.type}"
            )
        }
    }

    fun next(): Float {
        val f = nextFloat()
        advance()
        return f
    }
}

/**
 * Utility class to retrieve Vec2 float values from an accessor. The provided accessor must have a
 * float component type and must be of type VEC2.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class GltfVec2fAccessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessorType.Vec2) {
            throw IllegalArgumentException(
                "GltfVec2fAccessor requires accessor type ${GltfAccessorType.Vec2.value}, provided was ${accessor.type}"
            )
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
 * Utility class to retrieve Vec3 float values from an accessor. The provided accessor must have a
 * float component type and must be of type VEC3.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class GltfVec3fAccessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessorType.Vec3) {
            throw IllegalArgumentException(
                "GltfVec3fAccessor requires accessor type ${GltfAccessorType.Vec3.value}, provided was ${accessor.type}"
            )
        }
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
 * Utility class to retrieve Vec4 float values from an accessor. The provided accessor must have a
 * float component type and must be of type VEC4.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class GltfVec4fAccessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessorType.Vec4) {
            throw IllegalArgumentException(
                "GltfVec4fAccessor requires accessor type ${GltfAccessorType.Vec4.value}, provided was ${accessor.type}"
            )
        }
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
 * Utility class to retrieve Vec4 int values from an accessor. The provided accessor must have a non
 * floating point component type (byte, short or int either signed or unsigned) and must be of type
 * VEC4.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class GltfVec4iAccessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessorType.Vec4) {
            throw IllegalArgumentException(
                "GltfVec4iAccessor requires accessor type ${GltfAccessorType.Vec4.value}, provided was ${accessor.type}"
            )
        }
        if (accessor.componentType !in GltfComponentType.IntTypes) {
            throw IllegalArgumentException(
                "GltfVec4iAccessor requires a (byte / short / int) component type, provided was ${accessor.componentType}"
            )
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
 * Utility class to retrieve Mat4 float values from an accessor. The provided accessor must have a
 * float component type and must be of type MAT4.
 *
 * @param accessor [GltfAccessor] to use.
 */
internal class GltfMat4Accessor(accessor: GltfAccessor) : GltfAccessorDataStream(accessor) {
    init {
        if (accessor.type != GltfAccessorType.Mat4) {
            throw IllegalArgumentException(
                "GltfMat4Accessor requires accessor type ${GltfAccessorType.Mat4}, provided was ${accessor.type}"
            )
        }
        if (accessor.componentType != GltfComponentType.Float) {
            throw IllegalArgumentException(
                "GltfMat4Accessor requires a float component type, provided was ${accessor.componentType}"
            )
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
