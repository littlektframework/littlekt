package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.shader.DataSource
import com.lehaine.littlekt.graphics.shader.ShaderProgramReference
import com.lehaine.littlekt.graphics.shader.ShaderReference
import com.lehaine.littlekt.graphics.shader.Uniform
import com.lehaine.littlekt.math.Mat4


/**
 * @author Colton Daily
 * @date 11/20/2021
 */
enum class GLVersion {
    GL_32,
    GL_21
}

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
interface GL {

    fun getGLVersion(): GLVersion

    /**
     * @return if the current GL version is 3.2 or higher
     */
    fun isGL32() = getGLVersion() == GLVersion.GL_32

    fun clearColor(r: Float, g: Float, b: Float, a: Float)
    fun clear(mask: Int)
    fun clearDepth(depth: Number)
    fun enable(mask: Int)
    fun disable(mask: Int)
    fun blendFunc(sfactor: Int, dfactor: Int)
    fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int)
    fun createProgram(): ShaderProgramReference
    fun getAttribLocation(shaderProgram: ShaderProgramReference, name: String): Int
    fun getUniformLocation(shaderProgram: ShaderProgramReference, name: String): Uniform
    fun attachShader(shaderProgram: ShaderProgramReference, shaderReference: ShaderReference)
    fun linkProgram(shaderProgram: ShaderProgramReference)
    fun deleteProgram(shaderProgram: ShaderProgramReference)

    fun getString(parameterName: Int): String?

    fun getProgramParameter(shaderProgram: ShaderProgramReference, mask: Int): Any
    fun getProgramParameterB(shaderProgram: ShaderProgramReference, mask: Int): Boolean =
        getProgramParameter(shaderProgram, mask) as Boolean

    fun getShaderParameter(shaderReference: ShaderReference, mask: Int): Any
    fun getShaderParameterB(shaderReference: ShaderReference, mask: Int): Boolean =
        getShaderParameter(shaderReference, mask) as Boolean

    fun createShader(type: Int): ShaderReference
    fun shaderSource(shaderReference: ShaderReference, source: String)
    fun compileShader(shaderReference: ShaderReference)
    fun getShaderInfoLog(shaderReference: ShaderReference): String
    fun deleteShader(shaderReference: ShaderReference)
    fun getProgramInfoLog(shader: ShaderProgramReference): String
    fun createBuffer(): BufferReference
    fun createFrameBuffer(): FrameBufferReference
    fun createVertexArray(): VertexArrayReference
    fun bindVertexArray(vertexArrayReference: VertexArrayReference)
    fun bindDefaultVertexArray()
    fun bindFrameBuffer(frameBufferReference: FrameBufferReference)
    fun bindDefaultFrameBuffer()

    fun createRenderBuffer(): RenderBufferReference
    fun bindRenderBuffer(renderBufferReference: RenderBufferReference)
    fun renderBufferStorage(internalformat: Int, width: Int, height: Int)
    fun framebufferRenderbuffer(attachementType: Int, renderBufferReference: RenderBufferReference)

    fun frameBufferTexture2D(
        attachmentPoint: Int,
        textureReference: TextureReference,
        level: Int
    )

    fun bindBuffer(target: Int, bufferReference: BufferReference)
    fun bindDefaultBuffer(target: Int)
    fun deleteBuffer(bufferReference: BufferReference)
    fun bufferData(target: Int, data: DataSource, usage: Int)
    fun depthFunc(target: Int)
    fun depthMask(flag: Boolean)
    fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int)
    fun enableVertexAttribArray(index: Int)
    fun disableVertexAttribArray(index: Int)
    fun useProgram(shaderProgram: ShaderProgramReference)
    fun useDefaultProgram()

    fun createTexture(): TextureReference
    fun activeTexture(Int: Int)
    fun bindTexture(target: Int, textureReference: TextureReference)

    fun uniformMatrix4fv(uniform: Uniform, transpose: Boolean, data: Mat4) =
        uniformMatrix4fv(uniform, transpose, data.asGLArray())

    fun uniformMatrix4fv(uniform: Uniform, transpose: Boolean, data: FloatArray) =
        uniformMatrix4fv(uniform, transpose, data.toTypedArray())

    fun uniformMatrix4fv(uniform: Uniform, transpose: Boolean, data: Array<Float>)

    fun uniform1i(uniform: Uniform, data: Int)
    fun uniform2i(uniform: Uniform, a: Int, b: Int)
    fun uniform3i(uniform: Uniform, a: Int, b: Int, c: Int)

    fun uniform1f(uniform: Uniform, first: Float)
    fun uniform2f(uniform: Uniform, first: Float, second: Float)
    fun uniform3f(uniform: Uniform, first: Float, second: Float, third: Float)
    fun uniform4f(uniform: Uniform, first: Float, second: Float, third: Float, fourth: Float)

    fun drawArrays(mask: Int, offset: Int, vertexCount: Int)
    fun drawElements(mask: Int, vertexCount: Int, type: Int, offset: Int)

    fun pixelStorei(pname:Int, param:Int)

    fun vertex2f(x: Float, y: Float)

    fun viewport(x: Int, y: Int, width: Int, height: Int)

    fun texImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        format: Int,
        width: Int,
        height: Int,
        type: Int,
        source: ByteArray
    )

    fun texParameteri(target: Int, paramName: Int, paramValue: Int)

    fun generateMipmap(target: Int)

    companion object {
        const val ES_VERSION_2_0 = 1
        const val DEPTH_BUFFER_BIT = 0x00000100
        const val STENCIL_BUFFER_BIT = 0x00000400
        const val COLOR_BUFFER_BIT = 0x00004000
        const val FALSE = 0
        const val TRUE = 1
        const val POINTS = 0x0000
        const val LINES = 0x0001
        const val LINE_LOOP = 0x0002
        const val LINE_STRIP = 0x0003
        const val TRIANGLES = 0x0004
        const val TRIANGLE_STRIP = 0x0005
        const val TRIANGLE_FAN = 0x0006
        const val ZERO = 0
        const val ONE = 1
        const val SRC_COLOR = 0x0300
        const val ONE_MINUS_SRC_COLOR = 0x0301
        const val SRC_ALPHA = 0x0302
        const val ONE_MINUS_SRC_ALPHA = 0x0303
        const val DST_ALPHA = 0x0304
        const val ONE_MINUS_DST_ALPHA = 0x0305
        const val DST_COLOR = 0x0306
        const val ONE_MINUS_DST_COLOR = 0x0307
        const val SRC_ALPHA_SATURATE = 0x0308
        const val FUNC_ADD = 0x8006
        const val MIN = 0x8007
        const val MAX = 0x8008
        const val BLEND_EQUATION = 0x8009
        const val BLEND_EQUATION_RGB = 0x8009
        const val BLEND_EQUATION_ALPHA = 0x883D
        const val FUNC_SUBTRACT = 0x800A
        const val FUNC_REVERSE_SUBTRACT = 0x800B
        const val BLEND_DST_RGB = 0x80C8
        const val BLEND_SRC_RGB = 0x80C9
        const val BLEND_DST_ALPHA = 0x80CA
        const val BLEND_SRC_ALPHA = 0x80CB
        const val CONSTANT_COLOR = 0x8001
        const val ONE_MINUS_CONSTANT_COLOR = 0x8002
        const val CONSTANT_ALPHA = 0x8003
        const val ONE_MINUS_CONSTANT_ALPHA = 0x8004
        const val BLEND_COLOR = 0x8005
        const val ARRAY_BUFFER = 0x8892
        const val ELEMENT_ARRAY_BUFFER = 0x8893
        const val ARRAY_BUFFER_BINDING = 0x8894
        const val ELEMENT_ARRAY_BUFFER_BINDING = 0x8895
        const val STREAM_DRAW = 0x88E0
        const val STATIC_DRAW = 0x88E4
        const val DYNAMIC_DRAW = 0x88E8
        const val BUFFER_SIZE = 0x8764
        const val BUFFER_USAGE = 0x8765
        const val CURRENT_VERTEX_ATTRIB = 0x8626
        const val FRONT = 0x0404
        const val BACK = 0x0405
        const val FRONT_AND_BACK = 0x0408
        const val TEXTURE_2D = 0x0DE1
        const val CULL_FACE = 0x0B44
        const val BLEND = 0x0BE2
        const val DITHER = 0x0BD0
        const val STENCIL_TEST = 0x0B90
        const val DEPTH_TEST = 0x0B71
        const val SCISSOR_TEST = 0x0C11
        const val POLYGON_OFFSET_FILL = 0x8037
        const val SAMPLE_ALPHA_TO_COVERAGE = 0x809E
        const val SAMPLE_COVERAGE = 0x80A0
        const val NO_ERROR = 0
        const val INVALID_ENUM = 0x0500
        const val INVALID_VALUE = 0x0501
        const val INVALID_OPERATION = 0x0502
        const val OUT_OF_MEMORY = 0x0505
        const val CW = 0x0900
        const val CCW = 0x0901
        const val LINE_WIDTH = 0x0B21
        const val ALIASED_POINT_SIZE_RANGE = 0x846D
        const val ALIASED_LINE_WIDTH_RANGE = 0x846E
        const val CULL_FACE_MODE = 0x0B45
        const val FRONT_FACE = 0x0B46
        const val DEPTH_RANGE = 0x0B70
        const val DEPTH_WRITEMASK = 0x0B72
        const val DEPTH_CLEAR_VALUE = 0x0B73
        const val DEPTH_FUNC = 0x0B74
        const val STENCIL_CLEAR_VALUE = 0x0B91
        const val STENCIL_FUNC = 0x0B92
        const val STENCIL_FAIL = 0x0B94
        const val STENCIL_PASS_DEPTH_FAIL = 0x0B95
        const val STENCIL_PASS_DEPTH_PASS = 0x0B96
        const val STENCIL_REF = 0x0B97
        const val STENCIL_VALUE_MASK = 0x0B93
        const val STENCIL_WRITEMASK = 0x0B98
        const val STENCIL_BACK_FUNC = 0x8800
        const val STENCIL_BACK_FAIL = 0x8801
        const val STENCIL_BACK_PASS_DEPTH_FAIL = 0x8802
        const val STENCIL_BACK_PASS_DEPTH_PASS = 0x8803
        const val STENCIL_BACK_REF = 0x8CA3
        const val STENCIL_BACK_VALUE_MASK = 0x8CA4
        const val STENCIL_BACK_WRITEMASK = 0x8CA5
        const val VIEWPORT = 0x0BA2
        const val SCISSOR_BOX = 0x0C10
        const val COLOR_CLEAR_VALUE = 0x0C22
        const val COLOR_WRITEMASK = 0x0C23
        const val UNPACK_ALIGNMENT = 0x0CF5
        const val PACK_ALIGNMENT = 0x0D05
        const val MAX_TEXTURE_SIZE = 0x0D33
        const val MAX_TEXTURE_UNITS = 0x84E2
        const val MAX_VIEWPORT_DIMS = 0x0D3A
        const val SUBPIXEL_BITS = 0x0D50
        const val RED_BITS = 0x0D52
        const val GREEN_BITS = 0x0D53
        const val BLUE_BITS = 0x0D54
        const val ALPHA_BITS = 0x0D55
        const val DEPTH_BITS = 0x0D56
        const val STENCIL_BITS = 0x0D57
        const val POLYGON_OFFSET_UNITS = 0x2A00
        const val POLYGON_OFFSET_FACTOR = 0x8038
        const val TEXTURE_BINDING_2D = 0x8069
        const val SAMPLE_BUFFERS = 0x80A8
        const val SAMPLES = 0x80A9
        const val SAMPLE_COVERAGE_VALUE = 0x80AA
        const val SAMPLE_COVERAGE_INVERT = 0x80AB
        const val NUM_COMPRESSED_TEXTURE_FORMATS = 0x86A2
        const val COMPRESSED_TEXTURE_FORMATS = 0x86A3
        const val DONT_CARE = 0x1100
        const val FASTEST = 0x1101
        const val NICEST = 0x1102
        const val GENERATE_MIPMAP = 0x8191
        const val GENERATE_MIPMAP_HINT = 0x8192
        const val BYTE = 0x1400
        const val UNSIGNED_BYTE = 0x1401
        const val SHORT = 0x1402
        const val UNSIGNED_SHORT = 0x1403
        const val INT = 0x1404
        const val UNSIGNED_INT = 0x1405
        const val FLOAT = 0x1406
        const val FIXED = 0x140C
        const val DEPTH_COMPONENT = 0x1902
        const val ALPHA = 0x1906
        const val RGB = 0x1907
        const val RGBA = 0x1908
        const val LUMINANCE = 0x1909
        const val LUMINANCE_ALPHA = 0x190A
        const val UNSIGNED_SHORT_4_4_4_4 = 0x8033
        const val UNSIGNED_SHORT_5_5_5_1 = 0x8034
        const val UNSIGNED_SHORT_5_6_5 = 0x8363
        const val FRAGMENT_SHADER = 0x8B30
        const val VERTEX_SHADER = 0x8B31
        const val MAX_VERTEX_ATTRIBS = 0x8869
        const val MAX_VERTEX_UNIFORM_VECTORS = 0x8DFB
        const val MAX_VARYING_VECTORS = 0x8DFC
        const val MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D
        const val MAX_VERTEX_TEXTURE_IMAGE_UNITS = 0x8B4C
        const val MAX_TEXTURE_IMAGE_UNITS = 0x8872
        const val MAX_FRAGMENT_UNIFORM_VECTORS = 0x8DFD
        const val SHADER_TYPE = 0x8B4F
        const val DELETE_STATUS = 0x8B80
        const val LINK_STATUS = 0x8B82
        const val VALIDATE_STATUS = 0x8B83
        const val ATTACHED_SHADERS = 0x8B85
        const val ACTIVE_UNIFORMS = 0x8B86
        const val ACTIVE_UNIFORM_MAX_LENGTH = 0x8B87
        const val ACTIVE_ATTRIBUTES = 0x8B89
        const val ACTIVE_ATTRIBUTE_MAX_LENGTH = 0x8B8A
        const val SHADING_LANGUAGE_VERSION = 0x8B8C
        const val CURRENT_PROGRAM = 0x8B8D
        const val NEVER = 0x0200
        const val LESS = 0x0201
        const val EQUAL = 0x0202
        const val LEQUAL = 0x0203
        const val GREATER = 0x0204
        const val NOTEQUAL = 0x0205
        const val GEQUAL = 0x0206
        const val ALWAYS = 0x0207
        const val KEEP = 0x1E00
        const val REPLACE = 0x1E01
        const val INCR = 0x1E02
        const val DECR = 0x1E03
        const val INVERT = 0x150A
        const val INCR_WRAP = 0x8507
        const val DECR_WRAP = 0x8508
        const val VENDOR = 0x1F00
        const val RENDERER = 0x1F01
        const val VERSION = 0x1F02
        const val EXTENSIONS = 0x1F03
        const val NEAREST = 0x2600
        const val LINEAR = 0x2601
        const val NEAREST_MIPMAP_NEAREST = 0x2700
        const val LINEAR_MIPMAP_NEAREST = 0x2701
        const val NEAREST_MIPMAP_LINEAR = 0x2702
        const val LINEAR_MIPMAP_LINEAR = 0x2703
        const val TEXTURE_MAG_FILTER = 0x2800
        const val TEXTURE_MIN_FILTER = 0x2801
        const val TEXTURE_WRAP_S = 0x2802
        const val TEXTURE_WRAP_T = 0x2803
        const val TEXTURE = 0x1702
        const val TEXTURE_CUBE_MAP = 0x8513
        const val TEXTURE_BINDING_CUBE_MAP = 0x8514
        const val TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515
        const val TEXTURE_CUBE_MAP_NEGATIVE_X = 0x8516
        const val TEXTURE_CUBE_MAP_POSITIVE_Y = 0x8517
        const val TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518
        const val TEXTURE_CUBE_MAP_POSITIVE_Z = 0x8519
        const val TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x851A
        const val MAX_CUBE_MAP_TEXTURE_SIZE = 0x851C
        const val TEXTURE0 = 0x84C0
        const val TEXTURE1 = 0x84C1
        const val TEXTURE2 = 0x84C2
        const val TEXTURE3 = 0x84C3
        const val TEXTURE4 = 0x84C4
        const val TEXTURE5 = 0x84C5
        const val TEXTURE6 = 0x84C6
        const val TEXTURE7 = 0x84C7
        const val TEXTURE8 = 0x84C8
        const val TEXTURE9 = 0x84C9
        const val TEXTURE10 = 0x84CA
        const val TEXTURE11 = 0x84CB
        const val TEXTURE12 = 0x84CC
        const val TEXTURE13 = 0x84CD
        const val TEXTURE14 = 0x84CE
        const val TEXTURE15 = 0x84CF
        const val TEXTURE16 = 0x84D0
        const val TEXTURE17 = 0x84D1
        const val TEXTURE18 = 0x84D2
        const val TEXTURE19 = 0x84D3
        const val TEXTURE20 = 0x84D4
        const val TEXTURE21 = 0x84D5
        const val TEXTURE22 = 0x84D6
        const val TEXTURE23 = 0x84D7
        const val TEXTURE24 = 0x84D8
        const val TEXTURE25 = 0x84D9
        const val TEXTURE26 = 0x84DA
        const val TEXTURE27 = 0x84DB
        const val TEXTURE28 = 0x84DC
        const val TEXTURE29 = 0x84DD
        const val TEXTURE30 = 0x84DE
        const val TEXTURE31 = 0x84DF
        const val ACTIVE_TEXTURE = 0x84E0
        const val REPEAT = 0x2901
        const val CLAMP_TO_EDGE = 0x812F
        const val MIRRORED_REPEAT = 0x8370
        const val FLOAT_VEC2 = 0x8B50
        const val FLOAT_VEC3 = 0x8B51
        const val FLOAT_VEC4 = 0x8B52
        const val INT_VEC2 = 0x8B53
        const val INT_VEC3 = 0x8B54
        const val INT_VEC4 = 0x8B55
        const val BOOL = 0x8B56
        const val BOOL_VEC2 = 0x8B57
        const val BOOL_VEC3 = 0x8B58
        const val BOOL_VEC4 = 0x8B59
        const val FLOAT_MAT2 = 0x8B5A
        const val FLOAT_MAT3 = 0x8B5B
        const val FLOAT_MAT4 = 0x8B5C
        const val SAMPLER_2D = 0x8B5E
        const val SAMPLER_CUBE = 0x8B60
        const val VERTEX_ATTRIB_ARRAY_ENABLED = 0x8622
        const val VERTEX_ATTRIB_ARRAY_SIZE = 0x8623
        const val VERTEX_ATTRIB_ARRAY_STRIDE = 0x8624
        const val VERTEX_ATTRIB_ARRAY_TYPE = 0x8625
        const val VERTEX_ATTRIB_ARRAY_NORMALIZED = 0x886A
        const val VERTEX_ATTRIB_ARRAY_POINTER = 0x8645
        const val VERTEX_ATTRIB_ARRAY_BUFFER_BINDING = 0x889F
        const val IMPLEMENTATION_COLOR_READ_TYPE = 0x8B9A
        const val IMPLEMENTATION_COLOR_READ_FORMAT = 0x8B9B
        const val COMPILE_STATUS = 0x8B81
        const val INFO_LOG_LENGTH = 0x8B84
        const val SHADER_SOURCE_LENGTH = 0x8B88
        const val SHADER_COMPILER = 0x8DFA
        const val SHADER_BINARY_FORMATS = 0x8DF8
        const val NUM_SHADER_BINARY_FORMATS = 0x8DF9
        const val LOW_FLOAT = 0x8DF0
        const val MEDIUM_FLOAT = 0x8DF1
        const val HIGH_FLOAT = 0x8DF2
        const val LOW_INT = 0x8DF3
        const val MEDIUM_INT = 0x8DF4
        const val HIGH_INT = 0x8DF5
        const val FRAMEBUFFER = 0x8D40
        const val RENDERBUFFER = 0x8D41
        const val RGBA4 = 0x8056
        const val RGB5_A1 = 0x8057
        const val RGB565 = 0x8D62
        const val DEPTH_COMPONENT16 = 0x81A5
        const val STENCIL_INDEX = 0x1901
        const val STENCIL_INDEX8 = 0x8D48
        const val RENDERBUFFER_WIDTH = 0x8D42
        const val RENDERBUFFER_HEIGHT = 0x8D43
        const val RENDERBUFFER_INTERNAL_FORMAT = 0x8D44
        const val RENDERBUFFER_RED_SIZE = 0x8D50
        const val RENDERBUFFER_GREEN_SIZE = 0x8D51
        const val RENDERBUFFER_BLUE_SIZE = 0x8D52
        const val RENDERBUFFER_ALPHA_SIZE = 0x8D53
        const val RENDERBUFFER_DEPTH_SIZE = 0x8D54
        const val RENDERBUFFER_STENCIL_SIZE = 0x8D55
        const val FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE = 0x8CD0
        const val FRAMEBUFFER_ATTACHMENT_OBJECT_NAME = 0x8CD1
        const val FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL = 0x8CD2
        const val FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE = 0x8CD3
        const val COLOR_ATTACHMENT0 = 0x8CE0
        const val DEPTH_ATTACHMENT = 0x8D00
        const val STENCIL_ATTACHMENT = 0x8D20
        const val NONE = 0
        const val FRAMEBUFFER_COMPLETE = 0x8CD5
        const val FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x8CD6
        const val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7
        const val FRAMEBUFFER_INCOMPLETE_DIMENSIONS = 0x8CD9
        const val FRAMEBUFFER_UNSUPPORTED = 0x8CDD
        const val FRAMEBUFFER_BINDING = 0x8CA6
        const val RENDERBUFFER_BINDING = 0x8CA7
        const val MAX_RENDERBUFFER_SIZE = 0x84E8
        const val INVALID_FRAMEBUFFER_OPERATION = 0x0506
        const val VERTEX_PROGRAM_POINT_SIZE = 0x8642

        // Extensions
        const val COVERAGE_BUFFER_BIT_NV = 0x8000
        const val TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE
        const val MAX_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FF
    }
}