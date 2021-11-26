package com.lehaine.littlekt.graphics.gl

import com.lehaine.littlekt.graphics.GL
import kotlin.jvm.JvmInline

/**
 * @author Colton Daily
 * @date 11/21/2021
 */

@JvmInline
value class State(val glFlag: Int) {
    companion object {
        val BLEND = State(GL.BLEND)
        val CULL_FACE = State(GL.CULL_FACE)
        val DEPTH_TEST = State(GL.DEPTH_TEST)
        val DITHER = State(GL.DITHER)
        val POLYGON_OFFSET_FILL = State(GL.POLYGON_OFFSET_FILL)
        val PRIMITIVE_RESTART_FIXED_INDEX = State(GL.PRIMITIVE_RESTART_FIXED_INDEX)
        val RASTERIZER_DISCARD = State(GL.RASTERIZER_DISCARD)
        val SAMPLE_ALPHA_TO_COVERAGE = State(GL.SAMPLE_ALPHA_TO_COVERAGE)
        val SAMPLE_COVERAGE = State(GL.SAMPLE_COVERAGE)
        val SCISSOR_TEST = State(GL.SCISSOR_TEST)
        val STENCIL_TEST = State(GL.STENCIL_TEST)
    }

    operator fun plus(i: Int) = State(this.glFlag + i)
}


@JvmInline
value class AttribMask(val glFlag: Int) {

    companion object {
        val DEPTH_BUFFER_BIT = AttribMask(GL.DEPTH_BUFFER_BIT)
        val STENCIL_BUFFER_BIT = AttribMask(GL.STENCIL_BUFFER_BIT)
        val COLOR_BUFFER_BIT = AttribMask(GL.COLOR_BUFFER_BIT)
    }

    infix fun and(other: AttribMask): AttribMask = AttribMask(glFlag and other.glFlag)
    infix fun or(other: AttribMask): AttribMask = AttribMask(glFlag or other.glFlag)
    fun inv() = AttribMask(glFlag.inv())
}

@JvmInline
value class ClearBufferMask(val glFlag: Int) {

    companion object {
        val COLOR_BUFFER_BIT = ClearBufferMask(GL.COLOR_BUFFER_BIT)
        val STENCIL_BUFFER_BIT = ClearBufferMask(GL.STENCIL_BUFFER_BIT)
        val DEPTH_BUFFER_BIT = ClearBufferMask(GL.DEPTH_BUFFER_BIT)
        val COLOR_STENCIL_BUFFER_BIT = ClearBufferMask(GL.COLOR_BUFFER_BIT or GL.STENCIL_BUFFER_BIT)
        val COLOR_DEPTH_BUFFER_BIT = ClearBufferMask(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)
        val DEPTH_STENCIL_BUFFER_BIT = ClearBufferMask(GL.DEPTH_BUFFER_BIT or GL.STENCIL_BUFFER_BIT)
    }

    infix fun and(other: ClearBufferMask): ClearBufferMask = ClearBufferMask(glFlag and other.glFlag)
    infix fun or(other: ClearBufferMask): ClearBufferMask = ClearBufferMask(glFlag or other.glFlag)
    fun inv() = ClearBufferMask(glFlag.inv())
}

@JvmInline
value class DrawMode(val glFlag: Int) {

    companion object {
        val POINTS = DrawMode(GL.POINTS)
        val LINES = DrawMode(GL.LINES)
        val LINE_LOOP = DrawMode(GL.LINE_LOOP)
        val LINE_STRIP = DrawMode(GL.LINE_STRIP)
        val TRIANGLES = DrawMode(GL.TRIANGLES)
        val TRIANGLE_STRIP = DrawMode(GL.TRIANGLE_STRIP)
        val TRIANGLE_FAN = DrawMode(GL.TRIANGLE_FAN)
    }
}

@JvmInline
value class BlendFactor(val glFlag: Int) {

    companion object {
        val ZERO = BlendFactor(GL.ZERO)
        val ONE = BlendFactor(GL.ONE)
        val SRC_COLOR = BlendFactor(GL.SRC_COLOR)
        val ONE_MINUS_SRC_COLOR = BlendFactor(GL.ONE_MINUS_SRC_COLOR)
        val DST_COLOR = BlendFactor(GL.DST_COLOR)
        val ONE_MINUS_DST_COLOR = BlendFactor(GL.ONE_MINUS_DST_COLOR)
        val SRC_ALPHA = BlendFactor(GL.SRC_ALPHA)
        val ONE_MINUS_SRC_ALPHA = BlendFactor(GL.ONE_MINUS_SRC_ALPHA)
        val DST_ALPHA = BlendFactor(GL.DST_ALPHA)
        val ONE_MINUS_DST_ALPHA = BlendFactor(GL.ONE_MINUS_DST_ALPHA)
        val CONSTANT_COLOR = BlendFactor(GL.CONSTANT_COLOR)
        val ONE_MINUS_CONSTANT_COLOR = BlendFactor(GL.ONE_MINUS_CONSTANT_COLOR)
        val CONSTANT_ALPHA = BlendFactor(GL.CONSTANT_ALPHA)
        val ONE_MINUS_CONSTANT_ALPHA = BlendFactor(GL.ONE_MINUS_CONSTANT_ALPHA)
        val SRC_ALPHA_SATURATE = BlendFactor(GL.SRC_ALPHA_SATURATE)
    }
}

//###############################################################################

@JvmInline
value class BlendEquationMode(val glFlag: Int) {

    companion object {
        val FUNC_ADD = BlendEquationMode(GL.FUNC_ADD)
        val MIN = BlendEquationMode(GL.MIN)
        val MAX = BlendEquationMode(GL.MAX)
        val FUNC_SUBTRACT = BlendEquationMode(GL.FUNC_SUBTRACT)
        val FUNC_REVERSE_SUBTRACT = BlendEquationMode(GL.FUNC_REVERSE_SUBTRACT)
    }
}

@JvmInline
value class FaceMode(val glFlag: Int) {

    companion object {
        val FRONT = FaceMode(GL.FRONT)
        val BACK = FaceMode(GL.BACK)
        val FRONT_AND_BACK = FaceMode(GL.FRONT_AND_BACK)
    }
}

@JvmInline
value class CullFaceMode(val glFlag: Int) {

    companion object {
        val DISABLED = CullFaceMode(GL.FALSE)
        val FRONT = CullFaceMode(GL.FRONT)
        val BACK = CullFaceMode(GL.BACK)
        val FRONT_AND_BACK = CullFaceMode(GL.FRONT_AND_BACK)
    }
}

//###############################################################################

/** Depth, Stencil and textureCompareMode func */
@JvmInline
value class CompareFunction(val glFlag: Int) {

    companion object {
        val NEVER = CompareFunction(GL.NEVER)
        val LESS = CompareFunction(GL.LESS)
        val EQUAL = CompareFunction(GL.EQUAL)
        val LEQUAL = CompareFunction(GL.LEQUAL)
        val GREATER = CompareFunction(GL.GREATER)
        val NOTEQUAL = CompareFunction(GL.NOTEQUAL)
        val GEQUAL = CompareFunction(GL.GEQUAL)
        val ALWAYS = CompareFunction(GL.ALWAYS)
    }
}

@JvmInline
value class TexParameter(val glFlag: Int) {
    companion object {
        val BASE_LEVEL = TexParameter(GL.TEXTURE_BASE_LEVEL)
        val COMPARE_MODE = TexParameter(GL.TEXTURE_COMPARE_MODE)
        val COMPARE_FUNC = TexParameter(GL.TEXTURE_COMPARE_FUNC)
        val IMMUTABLE_FORMAT = TexParameter(GL.TEXTURE_IMMUTABLE_FORMAT)
        val IMMUTABLE_LEVELS = TexParameter(GL.TEXTURE_IMMUTABLE_LEVELS)
        val MAG_FILTER = TexParameter(GL.TEXTURE_MAG_FILTER)
        val MAX_LEVEL = TexParameter(GL.TEXTURE_MAX_LEVEL)
        val MAX_LOD = TexParameter(GL.TEXTURE_MAX_LOD)
        val MIN_FILTER = TexParameter(GL.TEXTURE_MIN_FILTER)
        val MIN_LOD = TexParameter(GL.TEXTURE_MIN_LOD)
        val SWIZZLE_R = TexParameter(GL.TEXTURE_SWIZZLE_R)
        val SWIZZLE_G = TexParameter(GL.TEXTURE_SWIZZLE_G)
        val SWIZZLE_B = TexParameter(GL.TEXTURE_SWIZZLE_B)
        val SWIZZLE_A = TexParameter(GL.TEXTURE_SWIZZLE_A)
        val WRAP_S = TexParameter(GL.TEXTURE_WRAP_S)
        val WRAP_T = TexParameter(GL.TEXTURE_WRAP_T)
        val WRAP_R = TexParameter(GL.TEXTURE_WRAP_R)
    }
}

@JvmInline
value class SamplerParameter(val glFlag: Int) {
    companion object {
        val COMPARE_MODE = SamplerParameter(GL.TEXTURE_COMPARE_MODE)
        val COMPARE_FUNC = SamplerParameter(GL.TEXTURE_COMPARE_FUNC)
        val MAG_FILTER = SamplerParameter(GL.TEXTURE_MAG_FILTER)
        val MAX_LOD = SamplerParameter(GL.TEXTURE_MAX_LOD)
        val MIN_FILTER = SamplerParameter(GL.TEXTURE_MIN_FILTER)
        val MIN_LOD = SamplerParameter(GL.TEXTURE_MIN_LOD)
        val WRAP_S = SamplerParameter(GL.TEXTURE_WRAP_S)
        val WRAP_T = SamplerParameter(GL.TEXTURE_WRAP_T)
        val WRAP_R = SamplerParameter(GL.TEXTURE_WRAP_R)
    }
}


@JvmInline
value class DataType(val glFlag: Int) {
    companion object {
        val BYTE = DataType(GL.BYTE)
        val UNSIGNED_BYTE = DataType(GL.UNSIGNED_BYTE)
        val SHORT = DataType(GL.SHORT)
        val UNSIGNED_SHORT = DataType(GL.UNSIGNED_SHORT)
        val INT = DataType(GL.INT)
        val UNSIGNED_INT = DataType(GL.UNSIGNED_INT)
        val FLOAT = DataType(GL.FLOAT)
        val HALF_FLOAT = DataType(GL.HALF_FLOAT)
        val UNSIGNED_SHORT_5_6_5 = DataType(GL.UNSIGNED_SHORT_5_6_5)
        val UNSIGNED_SHORT_4_4_4_4 = DataType(GL.UNSIGNED_SHORT_4_4_4_4)
    }

    val size: Int
        get() = when (glFlag) {
            GL.BYTE, GL.UNSIGNED_BYTE -> Byte.SIZE_BYTES
            GL.SHORT, GL.UNSIGNED_SHORT -> Short.SIZE_BYTES
            GL.INT, GL.UNSIGNED_INT -> Int.SIZE_BYTES
            GL.FLOAT -> Float.SIZE_BYTES
            else -> throw Exception("[DataType::size] Invalid value")
        }
}

@JvmInline
value class BufferType(val glFlag: Int) {
    companion object {
        val COLOR = BufferType(GL.COLOR)
        val DEPTH = BufferType(GL.DEPTH)
        val STENCIL = BufferType(GL.STENCIL)
        val DEPTH_STENCIL = BufferType(GL.DEPTH_STENCIL)
    }
}

@JvmInline
value class PixelStoreParameter(val glFlag: Int) {
    companion object {
        val UNPACK_ROW_LENGTH = PixelStoreParameter(GL.UNPACK_ROW_LENGTH)
        val UNPACK_SKIP_ROWS = PixelStoreParameter(GL.UNPACK_SKIP_ROWS)
        val UNPACK_SKIP_PIXELS = PixelStoreParameter(GL.UNPACK_SKIP_PIXELS)
        val UNPACK_ALIGNMENT = PixelStoreParameter(GL.UNPACK_ALIGNMENT)
        val UNPACK_SKIP_IMAGES = PixelStoreParameter(GL.UNPACK_SKIP_IMAGES)
        val UNPACK_IMAGE_HEIGHT = PixelStoreParameter(GL.UNPACK_IMAGE_HEIGHT)
        val PACK_ROW_LENGTH = PixelStoreParameter(GL.PACK_ROW_LENGTH)
        val PACK_SKIP_ROWS = PixelStoreParameter(GL.PACK_SKIP_ROWS)
        val PACK_SKIP_PIXELS = PixelStoreParameter(GL.PACK_SKIP_PIXELS)
        val PACK_ALIGNMENT = PixelStoreParameter(GL.PACK_ALIGNMENT)
    }
}

@JvmInline
value class TexMinFilter(val glFlag: Int) {
    companion object {
        val NEAREST = TexMinFilter(GL.NEAREST)
        val LINEAR = TexMinFilter(GL.LINEAR)
        val NEAREST_MIPMAP_NEAREST = TexMinFilter(GL.NEAREST_MIPMAP_NEAREST)
        val LINEAR_MIPMAP_NEAREST = TexMinFilter(GL.LINEAR_MIPMAP_NEAREST)
        val NEAREST_MIPMAP_LINEAR = TexMinFilter(GL.NEAREST_MIPMAP_LINEAR)
        val LINEAR_MIPMAP_LINEAR = TexMinFilter(GL.LINEAR_MIPMAP_LINEAR)
    }
}

@JvmInline
value class TexMagFilter(val glFlag: Int) {
    companion object {
        val NEAREST = TexMagFilter(GL.NEAREST)
        val LINEAR = TexMagFilter(GL.LINEAR)
    }
}

@JvmInline
value class TexWrap(val glFlag: Int) {
    companion object {
        val CLAMP_TO_EDGE = TexWrap(GL.CLAMP_TO_EDGE)
        val MIRRORED_REPEAT = TexWrap(GL.MIRRORED_REPEAT)
        val REPEAT = TexWrap(GL.REPEAT)
    }
}

@JvmInline
value class TextureTarget(val glFlag: Int) {
    companion object {
        val _2D = TextureTarget(GL.TEXTURE_2D)
        val _3D = TextureTarget(GL.TEXTURE_3D)
        val _2D_ARRAY = TextureTarget(GL.TEXTURE_2D_ARRAY)
        val CUBE_MAP = TextureTarget(GL.TEXTURE_CUBE_MAP)
        val CUBE_MAP_POSITIVE_X = TextureTarget(GL.TEXTURE_CUBE_MAP_POSITIVE_X)
        val CUBE_MAP_NEGATIVE_X = TextureTarget(GL.TEXTURE_CUBE_MAP_NEGATIVE_X)
        val CUBE_MAP_POSITIVE_Y = TextureTarget(GL.TEXTURE_CUBE_MAP_POSITIVE_Y)
        val CUBE_MAP_NEGATIVE_Y = TextureTarget(GL.TEXTURE_CUBE_MAP_NEGATIVE_Y)
        val CUBE_MAP_POSITIVE_Z = TextureTarget(GL.TEXTURE_CUBE_MAP_POSITIVE_Z)
        val CUBE_MAP_NEGATIVE_Z = TextureTarget(GL.TEXTURE_CUBE_MAP_NEGATIVE_Z)
    }
}

@JvmInline
value class TextureWrapMode(val glFlag: Int) {
    companion object {
        val CLAMP = TextureWrapMode(GL.CLAMP_TO_EDGE)
        val REPEAT = TextureWrapMode(GL.REPEAT)
    }
}

@JvmInline
value class InternalFormat(val glFlag: Int) {
    companion object {
        val R8 = InternalFormat(GL.R8)
        val R16F = InternalFormat(GL.R16F)
        val R32F = InternalFormat(GL.R32F)
        val R8I = InternalFormat(GL.R8I)
        val R16I = InternalFormat(GL.R16I)
        val R32I = InternalFormat(GL.R32I)
        val R8UI = InternalFormat(GL.R8UI)
        val R16UI = InternalFormat(GL.R16UI)
        val R32UI = InternalFormat(GL.R32UI)
        val RG8 = InternalFormat(GL.RG8)
        val RG16F = InternalFormat(GL.RG16F)
        val RG32F = InternalFormat(GL.RG32F)
        val RG8I = InternalFormat(GL.RG8I)
        val RG16I = InternalFormat(GL.RG16I)
        val RG32I = InternalFormat(GL.RG32I)
        val RG8UI = InternalFormat(GL.RG8UI)
        val RG16UI = InternalFormat(GL.RG16UI)
        val RGB32F = InternalFormat(GL.RGB32F)
        val RGB32I = InternalFormat(GL.RGB32I)
        val RGB32UI = InternalFormat(GL.RGB32UI)
        val RGBA8 = InternalFormat(GL.RGBA8)
        val RGBA16F = InternalFormat(GL.RGBA16F)
        val RGBA32F = InternalFormat(GL.RGBA32F)
        val RGBA8I = InternalFormat(GL.RGBA8I)
        val RGBA16I = InternalFormat(GL.RGBA16I)
        val RGBA32I = InternalFormat(GL.RGBA32I)
        val RGBA8UI = InternalFormat(GL.RGBA8UI)
        val RGBA16UI = InternalFormat(GL.RGBA16UI)
        val RGBA32UI = InternalFormat(GL.RGBA32UI)
    }
}


@JvmInline
value class TextureFormat(val glFlag: Int) {
    val hasAlpha: Boolean
        get() = when (glFlag) {
            GL.RGBA, GL.LUMINANCE_ALPHA, GL.ALPHA, GL.RGBA_INTEGER -> true
            else -> false
        }

    val channels: Int
        get() = when (glFlag) {
            GL.RED, GL.RED_INTEGER -> 1
            GL.RG, GL.RG_INTEGER -> 2
            GL.RGB, GL.RGB_INTEGER -> 3
            GL.RGBA, GL.RGBA_INTEGER -> 4
            else -> 0
        }

    companion object {
        val RED = TextureFormat(GL.RED)
        val RG = TextureFormat(GL.RG)
        val RGB = TextureFormat(GL.RGB)
        val RGBA = TextureFormat(GL.RGBA)
        val LUMINANCE_ALPHA = TextureFormat(GL.LUMINANCE_ALPHA)
        val ALPHA = TextureFormat(GL.ALPHA)
        val RED_INTEGER = TextureFormat(GL.RED_INTEGER)
        val RG_INTEGER = TextureFormat(GL.RG_INTEGER)
        val RGB_INTEGER = TextureFormat(GL.RGB_INTEGER)
        val RGBA_INTEGER = TextureFormat(GL.RGBA_INTEGER)
        val STENCIL_INDEX = TextureFormat(GL.STENCIL_INDEX)
        val DEPTH_COMPONENT = TextureFormat(GL.DEPTH_COMPONENT)
        val DEPTH_STENCIL = TextureFormat(GL.DEPTH_STENCIL)
    }
}

@JvmInline
value class BufferTarget(val glFlag: Int) {
    companion object {
        val ARRAY = BufferTarget(GL.ARRAY_BUFFER)
        val COPY_READ = BufferTarget(GL.COPY_READ_BUFFER)
        val COPY_WRITE = BufferTarget(GL.COPY_WRITE_BUFFER)
        val ELEMENT_ARRAY = BufferTarget(GL.ELEMENT_ARRAY_BUFFER)
        val PIXEL_PACK = BufferTarget(GL.PIXEL_PACK_BUFFER)
        val PIXEL_UNPACK = BufferTarget(GL.PIXEL_UNPACK_BUFFER)
        val TRANSFORM_FEEDBACK = BufferTarget(GL.TRANSFORM_FEEDBACK_BUFFER)
        val UNIFORM = BufferTarget(GL.UNIFORM_BUFFER)
    }
}

@JvmInline
value class Usage(val glFlag: Int) {
    companion object {
        val STREAM_DRAW = Usage(GL.STREAM_DRAW)
        val STREAM_READ = Usage(GL.STREAM_READ)
        val STREAM_COPY = Usage(GL.STREAM_COPY)
        val STATIC_DRAW = Usage(GL.STATIC_DRAW)
        val STATIC_READ = Usage(GL.STATIC_READ)
        val STATIC_COPY = Usage(GL.STATIC_COPY)
        val DYNAMIC_DRAW = Usage(GL.DYNAMIC_DRAW)
        val DYNAMIC_READ = Usage(GL.DYNAMIC_READ)
        val DYNAMIC_COPY = Usage(GL.DYNAMIC_COPY)
    }
}

@JvmInline
value class ShaderType(val glFlag: Int) {
    companion object {
        val VERTEX_SHADER = ShaderType(GL.VERTEX_SHADER)
        val FRAGMENT_SHADER = ShaderType(GL.FRAGMENT_SHADER)
    }
}

@JvmInline
value class GetShader(val glFlag: Int) {
    companion object {
        val SHADER_TYPE = GetShader(GL.SHADER_TYPE)
        val DELETE_STATUS = GetShader(GL.DELETE_STATUS)
        val COMPILE_STATUS = GetShader(GL.COMPILE_STATUS)
        val INFO_LOG_LENGTH = GetShader(GL.INFO_LOG_LENGTH)
        val SHADER_SOURCE_LENGTH = GetShader(GL.SHADER_SOURCE_LENGTH)
    }
}

@JvmInline
value class GetProgram(val glFlag: Int) {
    companion object {
        val DELETE_STATUS = GetProgram(GL.DELETE_STATUS)
        val LINK_STATUS = GetProgram(GL.LINK_STATUS)
        val VALIDATE_STATUS = GetProgram(GL.VALIDATE_STATUS)
        val INFO_LOG_LENGTH = GetProgram(GL.INFO_LOG_LENGTH)
        val ATTACHED_SHADERS = GetProgram(GL.ATTACHED_SHADERS)
        val ACTIVE_ATTRIBUTES = GetProgram(GL.ACTIVE_ATTRIBUTES)
        val ACTIVE_ATTRIBUTE_MAX_LENGTH = GetProgram(GL.ACTIVE_ATTRIBUTE_MAX_LENGTH)
        val ACTIVE_UNIFORMS = GetProgram(GL.ACTIVE_UNIFORMS)
        val ACTIVE_UNIFORM_BLOCKS = GetProgram(GL.ACTIVE_UNIFORM_BLOCKS)
        val ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH = GetProgram(GL.ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH)
        val ACTIVE_UNIFORM_MAX_LENGTH = GetProgram(GL.ACTIVE_UNIFORM_MAX_LENGTH)
        val PROGRAM_BINARY_LENGTH = GetProgram(GL.PROGRAM_BINARY_LENGTH)
        val TRANSFORM_FEEDBACK_BUFFER_MODE = GetProgram(GL.TRANSFORM_FEEDBACK_BUFFER_MODE)
        val TRANSFORM_FEEDBACK_VARYINGS = GetProgram(GL.TRANSFORM_FEEDBACK_VARYINGS)
        val TRANSFORM_FEEDBACK_VARYING_MAX_LENGTH = GetProgram(GL.TRANSFORM_FEEDBACK_VARYING_MAX_LENGTH)
        val PROGRAM_BINARY_RETRIEVABLE_HINT = GetProgram(GL.PROGRAM_BINARY_RETRIEVABLE_HINT)
    }
}

@JvmInline
value class UniformType(val glFlag: Int) {
    companion object {
        val FLOAT = UniformType(GL.FLOAT)
        val FLOAT_VEC2 = UniformType(GL.FLOAT_VEC2)
        val FLOAT_VEC3 = UniformType(GL.FLOAT_VEC3)
        val FLOAT_VEC4 = UniformType(GL.FLOAT_VEC4)
        val INT = UniformType(GL.INT)
        val INT_VEC2 = UniformType(GL.INT_VEC2)
        val INT_VEC3 = UniformType(GL.INT_VEC3)
        val INT_VEC4 = UniformType(GL.INT_VEC4)
        val UNSIGNED_INT = UniformType(GL.UNSIGNED_INT)
        val UNSIGNED_INT_VEC2 = UniformType(GL.UNSIGNED_INT_VEC2)
        val UNSIGNED_INT_VEC3 = UniformType(GL.UNSIGNED_INT_VEC3)
        val UNSIGNED_INT_VEC4 = UniformType(GL.UNSIGNED_INT_VEC4)
        val BOOL = UniformType(GL.BOOL)
        val BOOL_VEC2 = UniformType(GL.BOOL_VEC2)
        val BOOL_VEC3 = UniformType(GL.BOOL_VEC3)
        val BOOL_VEC4 = UniformType(GL.BOOL_VEC4)
        val FLOAT_MAT2 = UniformType(GL.FLOAT_MAT2)
        val FLOAT_MAT3 = UniformType(GL.FLOAT_MAT3)
        val FLOAT_MAT4 = UniformType(GL.FLOAT_MAT4)
        val FLOAT_MAT2x3 = UniformType(GL.FLOAT_MAT2x3)
        val FLOAT_MAT2x4 = UniformType(GL.FLOAT_MAT2x4)
        val FLOAT_MAT3x2 = UniformType(GL.FLOAT_MAT3x2)
        val FLOAT_MAT3x4 = UniformType(GL.FLOAT_MAT3x4)
        val FLOAT_MAT4x2 = UniformType(GL.FLOAT_MAT4x2)
        val FLOAT_MAT4x3 = UniformType(GL.FLOAT_MAT4x3)
        val SAMPLER_2D = UniformType(GL.SAMPLER_2D)
        val SAMPLER_3D = UniformType(GL.SAMPLER_3D)
        val SAMPLER_CUBE = UniformType(GL.SAMPLER_CUBE)
        val SAMPLER_2D_SHADOW = UniformType(GL.SAMPLER_2D_SHADOW)
        val SAMPLER_2D_ARRAY_SHADOW = UniformType(GL.SAMPLER_2D_ARRAY_SHADOW)
        val SAMPLER_CUBE_SHADOW = UniformType(GL.SAMPLER_CUBE_SHADOW)
        val INT_SAMPLER_2D = UniformType(GL.INT_SAMPLER_2D)
        val INT_SAMPLER_3D = UniformType(GL.INT_SAMPLER_3D)
        val INT_SAMPLER_CUBE = UniformType(GL.INT_SAMPLER_CUBE)
        val INT_SAMPLER_2D_ARRAY = UniformType(GL.INT_SAMPLER_2D_ARRAY)
        val UNSIGNED_INT_SAMPLER_2D = UniformType(GL.UNSIGNED_INT_SAMPLER_2D)
        val UNSIGNED_INT_SAMPLER_3D = UniformType(GL.UNSIGNED_INT_SAMPLER_3D)
        val UNSIGNED_INT_SAMPLER_CUBE = UniformType(GL.UNSIGNED_INT_SAMPLER_CUBE)
        val UNSIGNED_INT_SAMPLER_2D_ARRAY = UniformType(GL.UNSIGNED_INT_SAMPLER_2D_ARRAY)
    }
}

@JvmInline
value class VertexAttrType(val glFlag: Int) {
    companion object {
        val BYTE = VertexAttrType(GL.BYTE)
        val UNSIGNED_BYTE = VertexAttrType(GL.UNSIGNED_BYTE)
        val SHORT = VertexAttrType(GL.SHORT)
        val UNSIGNED_SHORT = VertexAttrType(GL.UNSIGNED_SHORT)
        val INT = VertexAttrType(GL.INT)
        val UNSIGNED_INT = VertexAttrType(GL.UNSIGNED_INT)
        val HALF_FLOAT = VertexAttrType(GL.HALF_FLOAT)
        val FLOAT = VertexAttrType(GL.FLOAT)
        val FIXED = VertexAttrType(GL.FIXED)
        val INT_2_10_10_10_REV = VertexAttrType(GL.INT_2_10_10_10_REV)
        val UNSIGNED_INT_2_10_10_10_REV = VertexAttrType(GL.UNSIGNED_INT_2_10_10_10_REV)
        val UNSIGNED_INT_10F_11F_11F_REV = VertexAttrType(GL.UNSIGNED_INT_10F_11F_11F_REV)
    }
}

@JvmInline
value class AttributeType(val glFlag: Int) {
    companion object {
        val FLOAT = AttributeType(GL.FLOAT)
        val FLOAT_VEC2 = AttributeType(GL.FLOAT_VEC2)
        val FLOAT_VEC3 = AttributeType(GL.FLOAT_VEC3)
        val FLOAT_VEC4 = AttributeType(GL.FLOAT_VEC4)
        val FLOAT_MAT2 = AttributeType(GL.FLOAT_MAT2)
        val FLOAT_MAT3 = AttributeType(GL.FLOAT_MAT3)
        val FLOAT_MAT4 = AttributeType(GL.FLOAT_MAT4)
        val FLOAT_MAT2x3 = AttributeType(GL.FLOAT_MAT2x3)
        val FLOAT_MAT2x4 = AttributeType(GL.FLOAT_MAT2x4)
        val FLOAT_MAT3x2 = AttributeType(GL.FLOAT_MAT3x2)
        val FLOAT_MAT3x4 = AttributeType(GL.FLOAT_MAT3x4)
        val FLOAT_MAT4x2 = AttributeType(GL.FLOAT_MAT4x2)
        val FLOAT_MAT4x3 = AttributeType(GL.FLOAT_MAT4x3)
        val INT_VEC2 = AttributeType(GL.INT_VEC2)
        val INT_VEC3 = AttributeType(GL.INT_VEC3)
        val INT_VEC4 = AttributeType(GL.INT_VEC4)
        val UNSIGNED_INT = AttributeType(GL.UNSIGNED_INT)
        val UNSIGNED_INT_VEC2 = AttributeType(GL.UNSIGNED_INT_VEC2)
        val UNSIGNED_INT_VEC3 = AttributeType(GL.UNSIGNED_INT_VEC3)
        val UNSIGNED_INT_VEC4 = AttributeType(GL.UNSIGNED_INT_VEC4)
    }
}

@JvmInline
value class VertexAttrib(val glFlag: Int) {
    companion object {
        val BUFFER_BINDING = VertexAttrib(GL.VERTEX_ATTRIB_ARRAY_BUFFER_BINDING)
        val ENABLED = VertexAttrib(GL.VERTEX_ATTRIB_ARRAY_ENABLED)
        val SIZE = VertexAttrib(GL.VERTEX_ATTRIB_ARRAY_SIZE)
        val STRIDE = VertexAttrib(GL.VERTEX_ATTRIB_ARRAY_STRIDE)
        val TYPE = VertexAttrib(GL.VERTEX_ATTRIB_ARRAY_TYPE)
        val NORMALIZED = VertexAttrib(GL.VERTEX_ATTRIB_ARRAY_NORMALIZED)
        val INTEGER = VertexAttrib(GL.VERTEX_ATTRIB_ARRAY_INTEGER)
        val DIVISOR = VertexAttrib(GL.VERTEX_ATTRIB_ARRAY_DIVISOR)
        val CURRENT = VertexAttrib(GL.CURRENT_VERTEX_ATTRIB)
    }
}

@JvmInline
value class IndexType(val glFlag: Int) {
    companion object {
        val UNSIGNED_BYTE = IndexType(GL.UNSIGNED_BYTE)
        val UNSIGNED_SHORT = IndexType(GL.UNSIGNED_SHORT)
        val UNSIGNED_INT = IndexType(GL.UNSIGNED_INT)
    }

    val size: Int
        get() = when (glFlag) {
            GL.UNSIGNED_BYTE -> Byte.SIZE_BYTES
            GL.UNSIGNED_SHORT -> Short.SIZE_BYTES
            GL.UNSIGNED_INT -> Int.SIZE_BYTES
            else -> throw Exception("[IndexType::size] Invalid value")
        }
}


@JvmInline
value class PrimitiveMode(val glFlag: Int) {
    companion object {
        val POINTS = PrimitiveMode(GL.POINTS)
        val LINES = PrimitiveMode(GL.LINES)
        val TRIANGLES = PrimitiveMode(GL.TRIANGLES)
    }
}

@JvmInline
value class RenderBufferInternalFormat(val glFlag: Int) {
    companion object {
        val RGBA4 = RenderBufferInternalFormat(GL.RGBA4)
        val RGB565 = RenderBufferInternalFormat(GL.RGB565)
        val RGB5_A1 = RenderBufferInternalFormat(GL.RGB5_A1)
        val DEPTH_COMPONENT16 = RenderBufferInternalFormat(GL.DEPTH_COMPONENT16)
        val STENCIL_INDEX8 = RenderBufferInternalFormat(GL.STENCIL_INDEX8)
        val DEPTH24_STENCIL8 = RenderBufferInternalFormat(GL.DEPTH24_STENCIL8)
        val DEPTH24_STENCIL8_OES = RenderBufferInternalFormat(GL.DEPTH24_STENCIL8)
    }
}

@JvmInline
value class FrameBufferRenderBufferAttachment(val glFlag: Int) {
    companion object {
        fun COLOR_ATTACHMENT(unit: Int = 0) = FrameBufferRenderBufferAttachment(GL.COLOR_ATTACHMENT0 + unit)
        val DEPTH_ATTACHMENT = FrameBufferRenderBufferAttachment(GL.DEPTH_ATTACHMENT)
        val STENCIL_ATTACHMENT = FrameBufferRenderBufferAttachment(GL.STENCIL_ATTACHMENT)
        val DEPTH_STENCIL_ATTACHMENT = FrameBufferRenderBufferAttachment(GL.DEPTH_STENCIL_ATTACHMENT)
    }
}

@JvmInline
value class FrameBufferStatus(val glFlag: Int) {
    companion object {
        val FRAMEBUFFER_UNDEFINED = FrameBufferStatus(GL.FRAMEBUFFER_UNDEFINED)
        val FRAMEBUFFER_UNSUPPORTED = FrameBufferStatus(GL.FRAMEBUFFER_UNSUPPORTED)
        val FRAMEBUFFER_INCOMPLETE_ATTACHMENT = FrameBufferStatus(GL.FRAMEBUFFER_INCOMPLETE_ATTACHMENT)
        val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = FrameBufferStatus(GL.FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)
        val FRAMEBUFFER_INCOMPLETE_MULTISAMPLE = FrameBufferStatus(GL.FRAMEBUFFER_INCOMPLETE_MULTISAMPLE)
        val FRAMEBUFFER_INCOMPLETE_DIMENSIONS = FrameBufferStatus(GL.FRAMEBUFFER_INCOMPLETE_DIMENSIONS)
        val FRAMEBUFFER_COMPLETE = FrameBufferStatus(GL.FRAMEBUFFER_COMPLETE)
    }
}