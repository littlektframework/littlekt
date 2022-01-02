package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.file.*
import com.lehaine.littlekt.graphics.gl.*
import com.lehaine.littlekt.math.Mat3
import com.lehaine.littlekt.math.Mat4


/**
 * @author Colton Daily
 * @date 11/20/2021
 */
enum class GLVersion {
    GL_32_PLUS,
    GL_30,
    GL_20,
    WEBGL,
    WEBGL2
}

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
interface GL {

    fun getGLVersion(): GLVersion

    /**
     * @return if the current GL version is 3.0 or higher
     */
    fun isGL30OrHigher() =
        getGLVersion() == GLVersion.GL_30 || getGLVersion() == GLVersion.GL_32_PLUS || getGLVersion() == GLVersion.WEBGL2

    fun clearColor(r: Float, g: Float, b: Float, a: Float)
    fun clearColor(color: Color) = clearColor(color.r, color.g, color.b, color.a)
    fun clear(mask: Int)
    fun clear(mask: ClearBufferMask) = clear(mask.glFlag)
    fun clearDepth(depth: Float)
    fun clearStencil(stencil: Int)
    fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean)
    fun cullFace(mode: Int)
    fun cullFace(mode: CullFaceMode) = cullFace(mode.glFlag)
    fun enable(cap: Int)
    fun enable(cap: State) = enable(cap.glFlag)
    fun disable(cap: Int)
    fun disable(cap: State) = disable(cap.glFlag)
    fun finish()
    fun flush()
    fun frontFace(mode: Int)
    fun frontFace(mode: FrontFaceMode) = frontFace(mode.glFlag)
    fun getError(): Int
    fun getIntegerv(pname: Int, data: IntBuffer)
    fun getString(pname: Int): String?
    fun hint(target: Int, mode: Int)
    fun hint(target: HintTarget, mode: HintMode) = hint(target.glFlag, mode.glFlag)
    fun lineWidth(width: Float)
    fun polygonOffset(factor: Float, units: Float)

    fun blendColor(red: Float, green: Float, blue: Float, alpha: Float)
    fun blendColor(color: Color) = blendColor(color.r, color.g, color.b, color.a)
    fun blendEquation(mode: Int)
    fun blendEquation(mode: BlendEquationMode) = blendEquation(mode.glFlag)
    fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int)
    fun blendEquationSeparate(modeRGB: BlendEquationMode, modeAlpha: BlendEquationMode) =
        blendEquationSeparate(modeRGB.glFlag, modeAlpha.glFlag)

    fun blendFunc(sfactor: Int, dfactor: Int)
    fun blendFunc(sfactor: BlendFactor, dfactor: BlendFactor) = blendFunc(sfactor.glFlag, dfactor.glFlag)
    fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int)
    fun blendFuncSeparate(srcRGB: BlendFactor, dstRGB: BlendFactor, srcAlpha: BlendFactor, dstAlpha: BlendFactor) =
        blendFuncSeparate(srcRGB.glFlag, dstRGB.glFlag, srcAlpha.glFlag, dstAlpha.glFlag)

    fun stencilFunc(func: Int, ref: Int, mask: Int)
    fun stencilFunc(func: CompareFunction, ref: Int, mask: Int) = stencilFunc(func.glFlag, ref, mask)
    fun stencilMask(mask: Int)
    fun stencilOp(fail: Int, zfail: Int, zpass: Int)
    fun stencilOp(fail: StencilAction, zfail: StencilAction, zpass: StencilAction) =
        stencilOp(fail.glFlag, zfail.glFlag, zpass.glFlag)

    fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int)
    fun stencilFuncSeparate(face: FaceMode, func: CompareFunction, ref: Int, mask: Int) =
        stencilFuncSeparate(face.glFlag, func.glFlag, ref, mask)

    fun stencilMaskSeparate(face: Int, mask: Int)
    fun stencilMaskSeparate(face: FaceMode, mask: Int) = stencilMaskSeparate(face.glFlag, mask)
    fun stencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int)
    fun stencilOpSeparate(face: FaceMode, fail: StencilAction, zfail: StencilAction, zpass: StencilAction) =
        stencilOpSeparate(face.glFlag, fail.glFlag, zfail.glFlag, zpass.glFlag)

    fun createProgram(): GlShaderProgram
    fun getAttribLocation(glShaderProgram: GlShaderProgram, name: String): Int
    fun getUniformLocation(glShaderProgram: GlShaderProgram, name: String): UniformLocation
    fun attachShader(glShaderProgram: GlShaderProgram, glShader: GlShader)
    fun detachShader(glShaderProgram: GlShaderProgram, glShader: GlShader)
    fun useProgram(glShaderProgram: GlShaderProgram)
    fun validateProgram(glShaderProgram: GlShaderProgram)
    fun useDefaultProgram()
    fun linkProgram(glShaderProgram: GlShaderProgram)
    fun deleteProgram(glShaderProgram: GlShaderProgram)


    fun getProgramParameter(glShaderProgram: GlShaderProgram, pname: Int): Any
    fun getProgramParameterB(glShaderProgram: GlShaderProgram, pname: Int): Boolean =
        getProgramParameter(glShaderProgram, pname) as Boolean

    fun getProgramParameter(glShaderProgram: GlShaderProgram, pname: GetProgram): Any =
        getProgramParameter(glShaderProgram, pname.glFlag)

    fun getProgramParameterB(glShaderProgram: GlShaderProgram, pname: GetProgram): Boolean =
        getProgramParameterB(glShaderProgram, pname.glFlag)

    fun getShaderParameter(glShader: GlShader, pname: Int): Any
    fun getShaderParameterB(glShader: GlShader, pname: Int): Boolean = getShaderParameter(glShader, pname) as Boolean

    fun getShaderParameter(glShader: GlShader, pname: GetShader) = getShaderParameter(glShader, pname.glFlag)
    fun getShaderParameterB(glShader: GlShader, pname: GetShader): Boolean = getShaderParameterB(glShader, pname.glFlag)

    fun createShader(type: Int): GlShader
    fun createShader(type: ShaderType) = createShader(type.glFlag)
    fun shaderSource(glShader: GlShader, source: String)
    fun compileShader(glShader: GlShader)
    fun getShaderInfoLog(glShader: GlShader): String
    fun deleteShader(glShader: GlShader)
    fun getProgramInfoLog(glShader: GlShaderProgram): String
    fun createVertexArray(): GlVertexArray
    fun bindVertexArray(glVertexArray: GlVertexArray)
    fun bindDefaultVertexArray()

    fun createFrameBuffer(): GlFrameBuffer
    fun bindFrameBuffer(glFrameBuffer: GlFrameBuffer)
    fun bindDefaultFrameBuffer()
    fun createRenderBuffer(): GlRenderBuffer
    fun bindRenderBuffer(glRenderBuffer: GlRenderBuffer)
    fun bindDefaultRenderBuffer()
    fun renderBufferStorage(internalFormat: RenderBufferInternalFormat, width: Int, height: Int)
    fun frameBufferRenderBuffer(attachementType: FrameBufferRenderBufferAttachment, glRenderBuffer: GlRenderBuffer)
    fun getBoundFrameBuffer(data: IntBuffer): GlFrameBuffer
    fun deleteFrameBuffer(glFrameBuffer: GlFrameBuffer)
    fun deleteRenderBuffer(glRenderBuffer: GlRenderBuffer)
    fun frameBufferTexture2D(attachementType: FrameBufferRenderBufferAttachment, glTexture: GlTexture, level: Int)
    fun checkFrameBufferStatus(): FrameBufferStatus

    fun createBuffer(): GlBuffer
    fun bindBuffer(target: Int, glBuffer: GlBuffer)
    fun bindBuffer(target: BufferTarget, glBuffer: GlBuffer) = bindBuffer(target.glFlag, glBuffer)
    fun bindDefaultBuffer(target: Int)
    fun bindDefaultBuffer(target: BufferTarget) = bindDefaultBuffer(target.glFlag)
    fun deleteBuffer(glBuffer: GlBuffer)
    fun bufferData(target: Int, data: DataSource, usage: Int)
    fun bufferData(target: BufferTarget, data: DataSource, usage: Usage) = bufferData(target.glFlag, data, usage.glFlag)

    fun bufferSubData(target: Int, offset: Int, data: DataSource)
    fun bufferSubData(target: BufferTarget, offset: Int, data: DataSource) =
        bufferSubData(target.glFlag, offset, data)

    fun depthFunc(func: Int)
    fun depthFunc(func: CompareFunction) = depthFunc(func.glFlag)
    fun depthMask(flag: Boolean)
    fun depthRangef(zNear: Float, zFar: Float)
    fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int)
    fun vertexAttribPointer(
        index: Int,
        size: Int,
        type: VertexAttrType,
        normalized: Boolean,
        stride: Int,
        offset: Int
    ) = vertexAttribPointer(index, size, type.glFlag, normalized, stride, offset)

    fun enableVertexAttribArray(index: Int)
    fun disableVertexAttribArray(index: Int)

    fun scissor(x: Int, y: Int, width: Int, height: Int)

    fun uniformMatrix3fv(uniformLocation: UniformLocation, transpose: Boolean, data: Mat3)
    fun uniformMatrix3fv(uniformLocation: UniformLocation, transpose: Boolean, data: FloatBuffer)
    fun uniformMatrix3fv(uniformLocation: UniformLocation, transpose: Boolean, data: Array<Float>)

    fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Mat4)
    fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: FloatBuffer)
    fun uniformMatrix4fv(uniformLocation: UniformLocation, transpose: Boolean, data: Array<Float>)

    fun uniform1i(uniformLocation: UniformLocation, data: Int)
    fun uniform2i(uniformLocation: UniformLocation, x: Int, y: Int)
    fun uniform3i(uniformLocation: UniformLocation, x: Int, y: Int, z: Int)

    fun uniform1f(uniformLocation: UniformLocation, x: Float)
    fun uniform2f(uniformLocation: UniformLocation, x: Float, y: Float)
    fun uniform3f(uniformLocation: UniformLocation, x: Float, y: Float, z: Float)
    fun uniform4f(uniformLocation: UniformLocation, x: Float, y: Float, z: Float, w: Float)

    fun drawArrays(mode: Int, offset: Int, count: Int)
    fun drawArrays(mode: DrawMode, offset: Int, count: Int) = drawArrays(mode.glFlag, offset, count)
    fun drawElements(mode: Int, count: Int, type: Int, offset: Int)
    fun drawElements(mode: DrawMode, count: Int, type: IndexType, offset: Int) =
        drawElements(mode.glFlag, count, type.glFlag, offset)

    fun pixelStorei(pname: Int, param: Int)
    fun pixelStorei(pname: PixelStoreParameter, param: Int) = pixelStorei(pname.glFlag, param)

    fun viewport(x: Int, y: Int, width: Int, height: Int)

    fun createTexture(): GlTexture
    fun activeTexture(texture: Int)
    fun bindTexture(target: Int, glTexture: GlTexture)
    fun bindTexture(target: TextureTarget, glTexture: GlTexture) = bindTexture(target.glFlag, glTexture)
    fun deleteTexture(glTexture: GlTexture)

    fun compressedTexImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        width: Int,
        height: Int,
        source: ByteBuffer?
    )

    fun compressedTexImage2D(
        target: TextureTarget,
        level: Int,
        internalFormat: TextureFormat,
        width: Int,
        height: Int,
        source: ByteBuffer?
    ) = compressedTexImage2D(
        target.glFlag,
        level,
        internalFormat.glFlag,
        width,
        height,
        source
    )

    fun compressedTexSubImage2D(
        target: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        width: Int,
        height: Int,
        format: Int,
        source: ByteBuffer
    )

    fun compressedTexSubImage2D(
        target: TextureTarget,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        width: Int,
        height: Int,
        format: TextureFormat,
        source: ByteBuffer
    ) = compressedTexSubImage2D(target.glFlag, level, xOffset, yOffset, width, height, format.glFlag, source)

    fun copyTexImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        border: Int,
    )

    fun copyTexImage2D(
        target: TextureTarget,
        level: Int,
        internalFormat: TextureFormat,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        border: Int
    ) = copyTexImage2D(target.glFlag, level, internalFormat.glFlag, x, y, width, height, border)

    fun copyTexSubImage2D(
        target: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    )

    fun copyTexSubImage2D(
        target: TextureTarget,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) = copyTexSubImage2D(target.glFlag, level, xOffset, yOffset, x, y, width, height)


    fun texSubImage2D(
        target: Int,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        source: ByteBuffer
    )

    fun texSubImage2D(
        target: TextureTarget,
        level: Int,
        xOffset: Int,
        yOffset: Int,
        width: Int,
        height: Int,
        format: TextureFormat,
        type: DataType,
        source: ByteBuffer
    ) = texSubImage2D(target.glFlag, level, xOffset, yOffset, width, height, format.glFlag, type.glFlag, source)

    fun texImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        format: Int,
        width: Int,
        height: Int,
        type: Int
    )

    fun texImage2D(
        target: TextureTarget,
        level: Int,
        internalFormat: TextureFormat,
        format: TextureFormat,
        width: Int,
        height: Int,
        type: DataType
    ) = texImage2D(target.glFlag, level, internalFormat.glFlag, format.glFlag, width, height, type.glFlag)

    fun texImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        format: Int,
        width: Int,
        height: Int,
        type: Int,
        source: ByteBuffer
    )

    fun texImage2D(
        target: TextureTarget,
        level: Int,
        internalFormat: TextureFormat,
        format: TextureFormat,
        width: Int,
        height: Int,
        type: DataType,
        source: ByteBuffer
    ) = texImage2D(target.glFlag, level, internalFormat.glFlag, format.glFlag, width, height, type.glFlag, source)

    fun texParameteri(target: Int, pname: Int, param: Int)
    fun texParameteri(target: TextureTarget, paramName: TexParameter, paramValue: Int) =
        texParameteri(target.glFlag, paramName.glFlag, paramValue)

    fun texParameterf(target: Int, pname: Int, param: Float)
    fun texParameterf(target: TextureTarget, paramName: TexParameter, paramValue: Float) =
        texParameterf(target.glFlag, paramName.glFlag, paramValue)

    fun generateMipmap(target: Int)
    fun generateMipmap(target: TextureTarget) = generateMipmap(target.glFlag)

    companion object {
        const val ES_VERSION_2_0: Int = 1
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

        // Extensi
        const val COVERAGE_BUFFER_BIT_NV = 0x8000
        const val TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE
        const val MAX_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FF

        const val READ_BUFFER = 0x0C02
        const val UNPACK_ROW_LENGTH = 0x0CF2
        const val UNPACK_SKIP_ROWS = 0x0CF3
        const val UNPACK_SKIP_PIXELS = 0x0CF4
        const val PACK_ROW_LENGTH = 0x0D02
        const val PACK_SKIP_ROWS = 0x0D03
        const val PACK_SKIP_PIXELS = 0x0D04
        const val COLOR = 0x1800
        const val DEPTH = 0x1801
        const val STENCIL = 0x1802
        const val RED = 0x1903
        const val RGB8 = 0x8051
        const val RGBA8 = 0x8058
        const val RGB10_A2 = 0x8059
        const val TEXTURE_BINDING_3D = 0x806A
        const val UNPACK_SKIP_IMAGES = 0x806D
        const val UNPACK_IMAGE_HEIGHT = 0x806E
        const val TEXTURE_3D = 0x806F
        const val TEXTURE_WRAP_R = 0x8072
        const val MAX_3D_TEXTURE_SIZE = 0x8073
        const val UNSIGNED_INT_2_10_10_10_REV = 0x8368
        const val MAX_ELEMENTS_VERTICES = 0x80E8
        const val MAX_ELEMENTS_INDICES = 0x80E9
        const val TEXTURE_MIN_LOD = 0x813A
        const val TEXTURE_MAX_LOD = 0x813B
        const val TEXTURE_BASE_LEVEL = 0x813C
        const val TEXTURE_MAX_LEVEL = 0x813D
        const val MIN = 0x8007
        const val MAX = 0x8008
        const val DEPTH_COMPONENT24 = 0x81A6
        const val MAX_TEXTURE_LOD_BIAS = 0x84FD
        const val TEXTURE_COMPARE_MODE = 0x884C
        const val TEXTURE_COMPARE_FUNC = 0x884D
        const val CURRENT_QUERY = 0x8865
        const val QUERY_RESULT = 0x8866
        const val QUERY_RESULT_AVAILABLE = 0x8867
        const val BUFFER_MAPPED = 0x88BC
        const val BUFFER_MAP_POINTER = 0x88BD
        const val STREAM_READ = 0x88E1
        const val STREAM_COPY = 0x88E2
        const val STATIC_READ = 0x88E5
        const val STATIC_COPY = 0x88E6
        const val DYNAMIC_READ = 0x88E9
        const val DYNAMIC_COPY = 0x88EA
        const val MAX_DRAW_BUFFERS = 0x8824
        const val DRAW_BUFFER0 = 0x8825
        const val DRAW_BUFFER1 = 0x8826
        const val DRAW_BUFFER2 = 0x8827
        const val DRAW_BUFFER3 = 0x8828
        const val DRAW_BUFFER4 = 0x8829
        const val DRAW_BUFFER5 = 0x882A
        const val DRAW_BUFFER6 = 0x882B
        const val DRAW_BUFFER7 = 0x882C
        const val DRAW_BUFFER8 = 0x882D
        const val DRAW_BUFFER9 = 0x882E
        const val DRAW_BUFFER10 = 0x882F
        const val DRAW_BUFFER11 = 0x8830
        const val DRAW_BUFFER12 = 0x8831
        const val DRAW_BUFFER13 = 0x8832
        const val DRAW_BUFFER14 = 0x8833
        const val DRAW_BUFFER15 = 0x8834
        const val MAX_FRAGMENT_UNIFORM_COMPONENTS = 0x8B49
        const val MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A
        const val SAMPLER_3D = 0x8B5F
        const val SAMPLER_2D_SHADOW = 0x8B62
        const val FRAGMENT_SHADER_DERIVATIVE_HINT = 0x8B8B
        const val PIXEL_PACK_BUFFER = 0x88EB
        const val PIXEL_UNPACK_BUFFER = 0x88EC
        const val PIXEL_PACK_BUFFER_BINDING = 0x88ED
        const val PIXEL_UNPACK_BUFFER_BINDING = 0x88EF
        const val FLOAT_MAT2x3 = 0x8B65
        const val FLOAT_MAT2x4 = 0x8B66
        const val FLOAT_MAT3x2 = 0x8B67
        const val FLOAT_MAT3x4 = 0x8B68
        const val FLOAT_MAT4x2 = 0x8B69
        const val FLOAT_MAT4x3 = 0x8B6A
        const val SRGB = 0x8C40
        const val SRGB8 = 0x8C41
        const val SRGB8_ALPHA8 = 0x8C43
        const val COMPARE_REF_TO_TEXTURE = 0x884E
        const val MAJOR_VERSION = 0x821B
        const val MINOR_VERSION = 0x821C
        const val NUM_EXTENSIONS = 0x821D
        const val RGBA32F = 0x8814
        const val RGB32F = 0x8815
        const val RGBA16F = 0x881A
        const val RGB16F = 0x881B
        const val VERTEX_ATTRIB_ARRAY_INTEGER = 0x88FD
        const val MAX_ARRAY_TEXTURE_LAYERS = 0x88FF
        const val MIN_PROGRAM_TEXEL_OFFSET = 0x8904
        const val MAX_PROGRAM_TEXEL_OFFSET = 0x8905
        const val MAX_VARYING_COMPONENTS = 0x8B4B
        const val TEXTURE_2D_ARRAY = 0x8C1A
        const val TEXTURE_BINDING_2D_ARRAY = 0x8C1D
        const val R11F_G11F_B10F = 0x8C3A
        const val UNSIGNED_INT_10F_11F_11F_REV = 0x8C3B
        const val RGB9_E5 = 0x8C3D
        const val UNSIGNED_INT_5_9_9_9_REV = 0x8C3E
        const val TRANSFORM_FEEDBACK_VARYING_MAX_LENGTH = 0x8C76
        const val TRANSFORM_FEEDBACK_BUFFER_MODE = 0x8C7F
        const val MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS = 0x8C80
        const val TRANSFORM_FEEDBACK_VARYINGS = 0x8C83
        const val TRANSFORM_FEEDBACK_BUFFER_START = 0x8C84
        const val TRANSFORM_FEEDBACK_BUFFER_SIZE = 0x8C85
        const val TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN = 0x8C88
        const val RASTERIZER_DISCARD = 0x8C89
        const val MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS = 0x8C8A
        const val MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS = 0x8C8B
        const val INTERLEAVED_ATTRIBS = 0x8C8C
        const val SEPARATE_ATTRIBS = 0x8C8D
        const val TRANSFORM_FEEDBACK_BUFFER = 0x8C8E
        const val TRANSFORM_FEEDBACK_BUFFER_BINDING = 0x8C8F
        const val RGBA32UI = 0x8D70
        const val RGB32UI = 0x8D71
        const val RGBA16UI = 0x8D76
        const val RGB16UI = 0x8D77
        const val RGBA8UI = 0x8D7C
        const val RGB8UI = 0x8D7D
        const val RGBA32I = 0x8D82
        const val RGB32I = 0x8D83
        const val RGBA16I = 0x8D88
        const val RGB16I = 0x8D89
        const val RGBA8I = 0x8D8E
        const val RGB8I = 0x8D8F
        const val RED_INTEGER = 0x8D94
        const val RGB_INTEGER = 0x8D98
        const val RGBA_INTEGER = 0x8D99
        const val SAMPLER_2D_ARRAY = 0x8DC1
        const val SAMPLER_2D_ARRAY_SHADOW = 0x8DC4
        const val SAMPLER_CUBE_SHADOW = 0x8DC5
        const val UNSIGNED_INT_VEC2 = 0x8DC6
        const val UNSIGNED_INT_VEC3 = 0x8DC7
        const val UNSIGNED_INT_VEC4 = 0x8DC8
        const val INT_SAMPLER_2D = 0x8DCA
        const val INT_SAMPLER_3D = 0x8DCB
        const val INT_SAMPLER_CUBE = 0x8DCC
        const val INT_SAMPLER_2D_ARRAY = 0x8DCF
        const val UNSIGNED_INT_SAMPLER_2D = 0x8DD2
        const val UNSIGNED_INT_SAMPLER_3D = 0x8DD3
        const val UNSIGNED_INT_SAMPLER_CUBE = 0x8DD4
        const val UNSIGNED_INT_SAMPLER_2D_ARRAY = 0x8DD7
        const val BUFFER_ACCESS_FLAGS = 0x911F
        const val BUFFER_MAP_LENGTH = 0x9120
        const val BUFFER_MAP_OFFSET = 0x9121
        const val DEPTH_COMPONENT32F = 0x8CAC
        const val DEPTH32F_STENCIL8 = 0x8CAD
        const val FLOAT_32_UNSIGNED_INT_24_8_REV = 0x8DAD
        const val FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING = 0x8210
        const val FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE = 0x8211
        const val FRAMEBUFFER_ATTACHMENT_RED_SIZE = 0x8212
        const val FRAMEBUFFER_ATTACHMENT_GREEN_SIZE = 0x8213
        const val FRAMEBUFFER_ATTACHMENT_BLUE_SIZE = 0x8214
        const val FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE = 0x8215
        const val FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE = 0x8216
        const val FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE = 0x8217
        const val FRAMEBUFFER_DEFAULT = 0x8218
        const val FRAMEBUFFER_UNDEFINED = 0x8219
        const val DEPTH_STENCIL_ATTACHMENT = 0x821A
        const val DEPTH_STENCIL = 0x84F9
        const val UNSIGNED_INT_24_8 = 0x84FA
        const val DEPTH24_STENCIL8 = 0x88F0
        const val UNSIGNED_NORMALIZED = 0x8C17
        const val DRAW_FRAMEBUFFER_BINDING = FRAMEBUFFER_BINDING
        const val READ_FRAMEBUFFER = 0x8CA8
        const val DRAW_FRAMEBUFFER = 0x8CA9
        const val READ_FRAMEBUFFER_BINDING = 0x8CAA
        const val RENDERBUFFER_SAMPLES = 0x8CAB
        const val FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER = 0x8CD4
        const val MAX_COLOR_ATTACHMENTS = 0x8CDF
        const val COLOR_ATTACHMENT1 = 0x8CE1
        const val COLOR_ATTACHMENT2 = 0x8CE2
        const val COLOR_ATTACHMENT3 = 0x8CE3
        const val COLOR_ATTACHMENT4 = 0x8CE4
        const val COLOR_ATTACHMENT5 = 0x8CE5
        const val COLOR_ATTACHMENT6 = 0x8CE6
        const val COLOR_ATTACHMENT7 = 0x8CE7
        const val COLOR_ATTACHMENT8 = 0x8CE8
        const val COLOR_ATTACHMENT9 = 0x8CE9
        const val COLOR_ATTACHMENT10 = 0x8CEA
        const val COLOR_ATTACHMENT11 = 0x8CEB
        const val COLOR_ATTACHMENT12 = 0x8CEC
        const val COLOR_ATTACHMENT13 = 0x8CED
        const val COLOR_ATTACHMENT14 = 0x8CEE
        const val COLOR_ATTACHMENT15 = 0x8CEF
        const val FRAMEBUFFER_INCOMPLETE_MULTISAMPLE = 0x8D56
        const val MAX_SAMPLES = 0x8D57
        const val HALF_FLOAT = 0x140B
        const val MAP_READ_BIT = 0x0001
        const val MAP_WRITE_BIT = 0x0002
        const val MAP_INVALIDATE_RANGE_BIT = 0x0004
        const val MAP_INVALIDATE_BUFFER_BIT = 0x0008
        const val MAP_FLUSH_EXPLICIT_BIT = 0x0010
        const val MAP_UNSYNCHRONIZED_BIT = 0x0020
        const val RG = 0x8227
        const val RG_INTEGER = 0x8228
        const val R8 = 0x8229
        const val RG8 = 0x822B
        const val R16F = 0x822D
        const val R32F = 0x822E
        const val RG16F = 0x822F
        const val RG32F = 0x8230
        const val R8I = 0x8231
        const val R8UI = 0x8232
        const val R16I = 0x8233
        const val R16UI = 0x8234
        const val R32I = 0x8235
        const val R32UI = 0x8236
        const val RG8I = 0x8237
        const val RG8UI = 0x8238
        const val RG16I = 0x8239
        const val RG16UI = 0x823A
        const val RG32I = 0x823B
        const val RG32UI = 0x823C
        const val VERTEX_ARRAY_BINDING = 0x85B5
        const val R8_SNORM = 0x8F94
        const val RG8_SNORM = 0x8F95
        const val RGB8_SNORM = 0x8F96
        const val RGBA8_SNORM = 0x8F97
        const val SIGNED_NORMALIZED = 0x8F9C
        const val PRIMITIVE_RESTART_FIXED_INDEX = 0x8D69
        const val COPY_READ_BUFFER = 0x8F36
        const val COPY_WRITE_BUFFER = 0x8F37
        const val COPY_READ_BUFFER_BINDING = COPY_READ_BUFFER
        const val COPY_WRITE_BUFFER_BINDING = COPY_WRITE_BUFFER
        const val UNIFORM_BUFFER = 0x8A11
        const val UNIFORM_BUFFER_BINDING = 0x8A28
        const val UNIFORM_BUFFER_START = 0x8A29
        const val UNIFORM_BUFFER_SIZE = 0x8A2A
        const val MAX_VERTEX_UNIFORM_BLOCKS = 0x8A2B
        const val MAX_FRAGMENT_UNIFORM_BLOCKS = 0x8A2D
        const val MAX_COMBINED_UNIFORM_BLOCKS = 0x8A2E
        const val MAX_UNIFORM_BUFFER_BINDINGS = 0x8A2F
        const val MAX_UNIFORM_BLOCK_SIZE = 0x8A30
        const val MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS = 0x8A31
        const val MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS = 0x8A33
        const val UNIFORM_BUFFER_OFFSET_ALIGNMENT = 0x8A34
        const val ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH = 0x8A35
        const val ACTIVE_UNIFORM_BLOCKS = 0x8A36
        const val UNIFORM_TYPE = 0x8A37
        const val UNIFORM_SIZE = 0x8A38
        const val UNIFORM_NAME_LENGTH = 0x8A39
        const val UNIFORM_BLOCK_INDEX = 0x8A3A
        const val UNIFORM_OFFSET = 0x8A3B
        const val UNIFORM_ARRAY_STRIDE = 0x8A3C
        const val UNIFORM_MATRIX_STRIDE = 0x8A3D
        const val UNIFORM_IS_ROW_MAJOR = 0x8A3E
        const val UNIFORM_BLOCK_BINDING = 0x8A3F
        const val UNIFORM_BLOCK_DATA_SIZE = 0x8A40
        const val UNIFORM_BLOCK_NAME_LENGTH = 0x8A41
        const val UNIFORM_BLOCK_ACTIVE_UNIFORMS = 0x8A42
        const val UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES = 0x8A43
        const val UNIFORM_BLOCK_REFERENCED_BY_VERTEX_SHADER = 0x8A44
        const val UNIFORM_BLOCK_REFERENCED_BY_FRAGMENT_SHADER = 0x8A46

        // GL_INVA_INDEX is defined as 0xFFFFFFFFu in C.
        const val INVALID_INDEX = -1
        const val MAX_VERTEX_OUTPUT_COMPONENTS = 0x9122
        const val MAX_FRAGMENT_INPUT_COMPONENTS = 0x9125
        const val MAX_SERVER_WAIT_TIMEOUT = 0x9111
        const val OBJECT_TYPE = 0x9112
        const val SYNC_CONDITION = 0x9113
        const val SYNC_STATUS = 0x9114
        const val SYNC_FLAGS = 0x9115
        const val SYNC_FENCE = 0x9116
        const val SYNC_GPU_COMMANDS_COMPLETE = 0x9117
        const val UNSIGNALED = 0x9118
        const val SIGNALED = 0x9119
        const val ALREADY_SIGNALED = 0x911A
        const val TIMEOUT_EXPIRED = 0x911B
        const val CONDITION_SATISFIED = 0x911C
        const val WAIT_FAILED = 0x911D
        const val SYNC_FLUSH_COMMANDS_BIT = 0x00000001

        // GL_TIME_IGNORED is defined as 0xFFFFFFFFFFFFFFFFull in C.
        const val TIMEOUT_IGNORED: Long = -1
        const val VERTEX_ATTRIB_ARRAY_DIVISOR = 0x88FE
        const val ANY_SAMPLES_PASSED = 0x8C2F
        const val ANY_SAMPLES_PASSED_CONSERVATIVE = 0x8D6A
        const val SAMPLER_BINDING = 0x8919
        const val RGB10_A2UI = 0x906F
        const val TEXTURE_SWIZZLE_R = 0x8E42
        const val TEXTURE_SWIZZLE_G = 0x8E43
        const val TEXTURE_SWIZZLE_B = 0x8E44
        const val TEXTURE_SWIZZLE_A = 0x8E45
        const val GREEN = 0x1904
        const val BLUE = 0x1905
        const val INT_2_10_10_10_REV = 0x8D9F
        const val TRANSFORM_FEEDBACK = 0x8E22
        const val TRANSFORM_FEEDBACK_PAUSED = 0x8E23
        const val TRANSFORM_FEEDBACK_ACTIVE = 0x8E24
        const val TRANSFORM_FEEDBACK_BINDING = 0x8E25
        const val PROGRAM_BINARY_RETRIEVABLE_HINT = 0x8257
        const val PROGRAM_BINARY_LENGTH = 0x8741
        const val NUM_PROGRAM_BINARY_FORMATS = 0x87FE
        const val PROGRAM_BINARY_FORMATS = 0x87FF
        const val COMPRESSED_R11_EAC = 0x9270
        const val COMPRESSED_SIGNED_R11_EAC = 0x9271
        const val COMPRESSED_RG11_EAC = 0x9272
        const val COMPRESSED_SIGNED_RG11_EAC = 0x9273
        const val COMPRESSED_RGB8_ETC2 = 0x9274
        const val COMPRESSED_SRGB8_ETC2 = 0x9275
        const val COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2 = 0x9276
        const val COMPRESSED_SRGB8_PUNCHTHROUGH_ALPHA1_ETC2 = 0x9277
        const val COMPRESSED_RGBA8_ETC2_EAC = 0x9278
        const val COMPRESSED_SRGB8_ALPHA8_ETC2_EAC = 0x9279
        const val TEXTURE_IMMUTABLE_FORMAT = 0x912F
        const val MAX_ELEMENT_INDEX = 0x8D6B
        const val NUM_SAMPLE_COUNTS = 0x9380
        const val TEXTURE_IMMUTABLE_LEVELS = 0x82DF
    }
}