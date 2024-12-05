package com.littlekt.graphics.webgpu

import kotlin.jvm.JvmInline

/** Primitive type the input mesh is composed of. */
enum class PrimitiveTopology {
    /** Vertex data is a list of points. Each vertex is a new point. */
    POINT_LIST,

    /**
     * Vertex data is a list of lines. Each pair of vertices composes a new line.
     *
     * Vertices `0 1 2 3` create two lines `0 1` and `2 3`.
     */
    LINE_LIST,

    /**
     * Vertex data is a strip of lines. Each set of two adjacent vertices form a line.
     *
     * Vertices `0 1 2 3` create three lines `0 1`, `1 2`, and `2 3`.
     */
    LINE_STRIP,

    /**
     * Vertex data is a list of triangles. Each set of 3 vertices composes a new triangle.
     *
     * Vertices `0 1 2 3 4 5` create two triangles `0 1 2` and `3 4 5`.
     */
    TRIANGLE_LIST,

    /**
     * Vertex data is a triangle strip. Each set of three adjacent vertices form a triangle.
     *
     * Vertices `0 1 2 3 4 5` create four triangles `0 1 2`, `2 1 3`, `2 3 4`, and `4 3 5`.
     */
    TRIANGLE_STRIP,
}

/** Vertex winding order which classifies the "front" face of a triangle. */
enum class FrontFace {
    /**
     * Triangles with vertices in counter-clockwise order are considered the front face.
     *
     * This is the default with right-handed coordinate spaces.
     */
    CCW,

    /**
     * Triangles with vertices in clockwise order are considered the front face.
     *
     * This is the default with left-handed coordinate spaces.
     */
    CW,
}

/** Face of a vertex. */
enum class CullMode {
    /** Neither used. */
    NONE,

    /** Front face. */
    FRONT,

    /** Back face. */
    BACK,
}

/**
 * The usages determine what kind of memory the texture is allocated from and what actions the
 * texture can partake in.
 */
@JvmInline
value class TextureUsage(val usageFlag: Int) {
    infix fun or(other: TextureUsage): TextureUsage = TextureUsage(usageFlag or other.usageFlag)

    infix fun and(other: TextureUsage): TextureUsage = TextureUsage(usageFlag and other.usageFlag)

    companion object {
        /** Allows a texture to be the source in a copy operation */
        val COPY_SRC: TextureUsage = TextureUsage(0x01)

        /**
         * Allows a texture to be the destination of a copy operation such as
         * [CommandEncoder.copyBufferToTexture]
         */
        val COPY_DST: TextureUsage = TextureUsage(0x02)

        /** Allows a texture to be a sampled texture in a bind group */
        val TEXTURE: TextureUsage = TextureUsage(0x04)

        /** Allows a texture to be a storage texture in a bind group */
        val STORAGE: TextureUsage = TextureUsage(0x08)

        /** Allows a texture to be an output attachment of a render pass */
        val RENDER_ATTACHMENT: TextureUsage = TextureUsage(0x10)
    }
}

/** Dimensions of a particular texture view. */
enum class TextureViewDimension {
    /** A one dimensional texture. */
    D1,

    /** A two-dimensional texture. */
    D2,

    /** A two-dimensional array texture. */
    D2_ARRAY,

    /** A cubemap texture. */
    CUBE,

    /** A cubemap array texture. */
    CUBE_ARRAY,

    /** A three-dimensional texture. */
    D3,
}

/** Type of data the texture holds. */
enum class TextureAspect {
    /** Depth, stecil, and color. */
    ALL,

    /** Stencil only. */
    STENCIL_ONLY,

    /** Depth only. */
    DEPTH_ONLY,
}

/** Dimensionality of a texture. */
enum class TextureDimension {
    /** 1D texture. */
    D1,

    /** 2D texture. */
    D2,

    /** 3D texture. */
    D3,
}

/**
 * The underlying texture data format.
 *
 * If there is a conversion in the format (such as srgb -> linear), the conversion listed here is
 * for loading from texture in a shader. When writing to the texture, the opposite conversion takes
 * place.
 *
 * @param bytes the number of bytes each format requires
 * @param srgb if this format is an SRGB format and expects linear colors.
 */
enum class TextureFormat(val bytes: Int, val srgb: Boolean = false) {
    /** 8 Bit Red channel only. `[0, 255]` converted to/from float `[0, 1]` in shader. */
    R8_UNORM(1),

    /** 8 Bit Red channel only. `[-127, 127]` converted to/from float `[-1, 1]` in shader. */
    R8_SNORM(1),

    /** Red channel only. 8 bit integer per channel. Unsigned in shader. */
    R8_UINT(1),

    /** Red channel only. 8 bit integer per channel. Signed in shader. */
    R8_SINT(1),

    /** Red channel only. 16 bit integer per channel. Unsigned in shader. */
    R16_UINT(2),

    /** Red channel only. 16 bit integer per channel. Signed in shader. */
    R16_SINT(2),

    /** Red channel only. 16 bit float per channel. Float in shader. */
    R16_FLOAT(2),

    /**
     * Red and green channels. 8 bit integer per channel. `[0, 255]` converted to/from float `[0,
     * 1]` in shader.
     */
    RG8_UNORM(2),

    /**
     * Red and green channels. 8 bit integer per channel. `[-127, 127]` converted to/from float
     * `[-1, 1]` in shader.
     */
    RG8_SNORM(2),

    /** Red and green channels. 8 bit integer per channel. Unsigned in shader. */
    RG8_UINT(2),

    /** Red and green channel s. 8 bit integer per channel. Signed in shader. */
    RG8_SINT(2),

    /** Red channel only. 32 bit integer per channel. Unsigned in shader. */
    R32_UINT(4),

    /** Red channel only. 32 bit integer per channel. Signed in shader. */
    R32_SINT(4),

    /** Red channel only. 32 bit float per channel. Float in shader. */
    R32_FLOAT(4),

    /** Red and green channels. 16 bit integer per channel. Unsigned in shader. */
    RG16_UINT(4),

    /** Red and green channels. 16 bit integer per channel. Signed in shader. */
    RG16_SINT(4),

    /** Red and green channels. 16 bit float per channel. Float in shader. */
    RG16_FLOAT(4),

    /**
     * Red, green, blue, and alpha channels. 8 bit integer per channel. `[0, 255]` converted to/from
     * float `[0, 1]` in shader.
     */
    RGBA8_UNORM(4),

    /**
     * Red, green, blue, and alpha channels. 8 bit integer per channel. Srgb-color `[0, 255]`
     * converted to/from linear-color float `[0, 1]` in shader.
     */
    RGBA8_UNORM_SRGB(4, true),

    /**
     * Red, green, blue, and alpha channels. 8 bit integer per channel. `[-127, 127]` converted
     * to/from float `[-1, 1]` in shader.
     */
    RGBA8_SNORM(4),

    /** Red, green, blue, and alpha channels. 8 bit integer per channel. Unsigned in shader. */
    RGBA8_UINT(4),

    /** Red, green, blue, and alpha channels. 8 bit integer per channel. Signed in shader. */
    RGBA8_SINT(4),

    /**
     * Blue, green, red, and alpha channels. 8 bit integer per channel. `[0, 255]` converted to/from
     * float `[0, 1]` in shader.
     */
    BGRA8_UNORM(4),

    /**
     * Blue, green, red, and alpha channels. 8 bit integer per channel. Srgb-color `[0, 255]`
     * converted to/from linear-color float `[0, 1]` in shader.
     */
    BGRA8_UNORM_SRGB(4, true),

    /**
     * Red, green, blue, and alpha channels. 10 bit integer for RGB channels, 2 bit integer for
     * alpha channel. `[0, 1023]` (`[0, 3]` for alpha) converted to/from float `[0, 1]` in shader.
     */
    RGB10A2_UNORM(4),

    /**
     * Red, green, and blue channels. 11 bit float with no sign bit for RG channels. 10 bit float
     * with no sign bit for blue channel. Float in shader.
     */
    RG11B10_FLOAT(4),

    /** Red and green channels. 32 bit integer per channel. Unsigned in shader. */
    RG32_UINT(8),

    /** Red and green channels. 32 bit integer per channel. Signed in shader. */
    RG32_SINT(8),

    /** Red and green channels. 32 bit float per channel. Float in shader. */
    RG32_FLOAT(8),

    /** Red, green, blue, and alpha channels. 16 bit integer per channel. Unsigned in shader. */
    RGBA16_UINT(8),

    /** Red, green, blue, and alpha channels. 16 bit integer per channel. Signed in shader. */
    RGBA16_SINT(8),

    /** Red, green, blue, and alpha channels. 16 bit float per channel. Float in shader. */
    RGBA16_FLOAT(8),

    /** Red, green, blue, and alpha channels. 32 bit integer per channel. Unsigned in shader. */
    RGBA32_UINT(16),

    /** Red, green, blue, and alpha channels. 32 bit integer per channel. Signed in shader. */
    RGBA32_SINT(16),

    /** Red, green, blue, and alpha channels. 32 bit float per channel. Float in shader. */
    RGBA32_FLOAT(16),

    /** Special depth format with 32 bit floating point depth. */
    DEPTH32_FLOAT(4),

    /** Special depth format with at least 24 bit integer depth. */
    DEPTH24_PLUS(3),

    /**
     * Special depth/stencil format with at least 24 bit integer depth and 8 bits integer stencil.
     */
    DEPTH24_PLUS_STENCIL8(4);

    companion object
}

/** Alpha blend operation. */
enum class BlendOperation {
    /** Src + Dst */
    ADD,

    /** Src - Dst */
    SUBTRACT,

    /** Dst - Src */
    REVERSE_SUBTRACT,

    /** min(Src, Dst) */
    MIN,

    /** max(Src, Dst) */
    MAX,
}

/** Operation to perform on the stencil value. */
enum class StencilOperation {
    /** Keep stencil value unchanged. */
    KEEP,

    /** Set stencil value to zero. */
    ZERO,

    /**
     * Replace stencil value with value provided in most recent call to
     * [RenderPassEncoder.setStencilReference].
     */
    REPLACE,

    /** Bitwise inverts stencil value. */
    INVERT,

    /** Increments stencil value by one, clamping on overflow. */
    INCREMENT_CLAMP,

    /** Decrements stencil value by one, clamping on underflow. */
    DECREMENT_CLAMP,
    /** Increments stencil value by one, wrapping on overflow. */
    INCREMENT_WRAP,
    /** Decrements stencil value by one, wrapping on underflow. */
    DECREMENT_WRAP,
}

/** Alpha blend factor. */
enum class BlendFactor {
    /** 0.0 */
    ZERO,

    /** 1.0 */
    ONE,

    /** S.component */
    SRC_COLOR,

    /** 1.0 - S.component */
    ONE_MINUS_SRC_COLOR,

    /** S.alpha */
    SRC_ALPHA,

    /** 1.0 - S.alpha */
    ONE_MINUS_SRC_ALPHA,

    /** D.component */
    DST_COLOR,

    /** 1.0 - D.component */
    ONE_MINUS_DST_COLOR,

    /** D.alpha */
    DST_ALPHA,

    /** 1.0 - D.alpha */
    ONE_MINUS_DST_ALPHA,

    /** min(S.alpha, 1.0 - D.alpha) */
    SRC_ALPHA_SATURATED,

    /** Constant */
    CONSTANT_COLOR,

    /** 1.0 - Constant */
    ONE_MINUS_CONSTANT_COLOR,
}

/** Format of indices used with pipeline. */
enum class IndexFormat {
    /** Indices are 16 bit unsigned integers. */
    UINT16,

    /** Indices are 32 bit unsigned integers. */
    UINT32,
}

/** Vertex format for a [WebGPUVertexAttribute] (input). */
enum class VertexFormat(
    /** The number of components of the format. */
    val components: Int,
    /** The byte size of the format. */
    val bytes: Int,
    /** If the format uses integer or float. */
    val isInt: Boolean = false,
) {
    /** Two unsigned bytes. uvec2 in shaders */
    UINT8x2(2, 2, true),

    /** Four unsigned bytes. uvec4 in shaders */
    UINT8x4(4, 4, true),

    /** Two signed bytes. ivec2 in shaders */
    SINT8x2(2, 2, true),

    /** Four signed bytes. ivec4 in shaders */
    SINT8x4(4, 4, true),

    /** Two unsigned bytes `[0, 255]` converted to floats `[0, 1]`. vec2 in shaders */
    UNORM8x2(2, 2),

    /** Four unsigned bytes `[0, 255]` converted to floats `[0, 1]`. vec4 in shaders */
    UNORM8x4(4, 4),

    /** two signed bytes converted to float `[-1,1]`. vec2 in shaders */
    SNORM8x2(2, 2),

    /** two signed bytes converted to float `[-1,1]`. vec2 in shaders */
    SNORM8x4(4, 4),

    /** two unsigned shorts. uvec2 in shaders */
    UINT16x2(2, 4, true),

    /** four unsigned shorts. uvec4 in shaders */
    UINT16x4(4, 8, true),

    /** two signed shorts. ivec2 in shaders */
    SINT16x2(2, 4, true),

    /** four signed shorts. ivec4 in shaders */
    SINT16x4(4, 8, true),

    /** two unsigned shorts `[0, 65525]` converted to float `[0, 1]`. vec2 in shaders */
    UNORM16x2(2, 4),

    /** four unsigned shorts `[0, 65525]` converted to float `[0, 1]`. vec4 in shaders */
    UNORM16x4(4, 8),

    /** two signed shorts `[-32767, 32767]` converted to float `[-1, 1]`. vec2 in shaders */
    SNORM16x2(2, 4),

    /** two signed shorts `[-32767, 32767]` converted to float `[-1, 1]`. vec4 in shaders */
    SNORM16x4(4, 8),

    /** two half precision floats. vec2 in shaders */
    FLOAT16x2(2, 4),

    /** four half precision floats. vec4 in shaders */
    FLOAT16x4(4, 8),

    /** one float. float in shaders */
    FLOAT32(1, 4),

    /** two floats. vec2 in shaders */
    FLOAT32x2(2, 8),

    /** three floats. vec3 in shaders */
    FLOAT32x3(3, 12),

    /** four floats. vec4 in shaders */
    FLOAT32x4(4, 16),

    /** one unsigned int. uint in shaders */
    UINT32(1, 4, true),

    /** two unsigned ints. uvec2 in shaders */
    UINT32x2(2, 8, true),

    /** three unsigned ints. uvec3 in shaders */
    UINT32x3(3, 12, true),

    /** four unsigned ints. uvec4 in shaders */
    UINT32x4(4, 16, true),

    /** one signed int. int in shaders */
    SINT32(1, 4, true),

    /** two signed ints. ivec2 in shaders */
    SINT32x2(2, 8, true),

    /** three signed ints. ivec2 in shaders */
    SINT32x3(3, 12, true),

    /** four signed ints. ivec2 in shaders */
    SINT32x4(4, 16, true),
}

/** Whether a vertex buffer is indexed by vertex or by instance. */
enum class VertexStepMode {
    /** Vertex data is advanced every vertex. */
    VERTEX,

    /** Vertex data is advanced every instance. */
    INSTANCE,
}

/** Operation to perform to the output attachment at the start of a render pass. */
enum class LoadOp {
    /**
     * Loads the specified value for this attachment into the render pass.
     *
     * On some GPU hardware (primarily mobile), “clear” is significantly cheaper because it avoids
     * loading data from main memory into tile-local memory.
     *
     * On other GPU hardware, there isn’t a significant difference.
     *
     * As a result, it is recommended to use “clear” rather than “load” in cases where the initial
     * value doesn’t matter (e.g. the render target will be cleared using a skybox).
     */
    CLEAR,

    /** Loads the existing value for this attachment into the render pass. */
    LOAD,
}

/** Operation to perform to the output attachment at the end of a render pass. */
enum class StoreOp {
    /**
     * Discards the resulting value of the render pass for this attachment.
     *
     * The attachment will be treated as uninitialized afterwards. (If only either Depth or Stencil
     * texture-aspects is set to Discard, the respective other texture-aspect will be preserved.)
     *
     * This can be significantly faster on tile-based render hardware.
     *
     * Prefer this if the attachment is not read by subsequent passes.
     */
    DISCARD,

    /** Stores the resulting value of the render pass for this attachment. */
    STORE,
}

/** Specific type of a buffer binding. */
enum class BufferBindingType {
    /**
     * A buffer for uniform values.
     *
     * ```wgsl
     * struct Globals {
     *     a_uniform: vec2<f32>,
     *     another_uniform: vec2<f32>,
     * }
     * @group(0) @binding(0)
     * var<uniform> globals: Globals;
     * ```
     */
    UNIFORM,

    /**
     * A storage buffer.
     *
     * ```wgsl
     * @group(0) @binding(0)
     * var<storage, read_write> my_element: array<vec4<f32>>;
     * ```
     */
    STORAGE,

    /**
     * A read only storage buffer. The buffer can only be read in the shader.
     *
     * ```wgsl
     * @group(0) @binding(0)
     * var<storage, read> my_element: array<vec4<f32>>;
     * ```
     */
    READ_ONLY_STORAGE,
}

enum class AddressMode {
    CLAMP_TO_EDGE,
    REPEAT,
    MIRROR_REPEAT,
}

enum class FilterMode {
    NEAREST,
    LINEAR,
}

/** Comparison function used for depth and stencil operations. */
enum class CompareFunction {
    /** Function never passes. */
    NEVER,

    /** Function passes if new value less than existing value. */
    LESS,

    /**
     * Function passes if new value is equal to existing value. When using this compare function,
     * make sure to mark your Vertex shader's `@builtin(position)` output as `@invariant` to prevent
     * artifacting.
     */
    EQUAL,

    /** Function passes if new value is less than or equal to existing value. */
    LESS_EQUAL,

    /** Function passes if new value is greater than existing value. */
    GREATER,
    /**
     * Function passes if new value is not equal to existing value. When using this compare
     * function, make sure to mark your Vertex shader's `@builtin(position)` output as `@invariant`
     * to prevent artifacting.
     */
    NOT_EQUAL,

    /** Function passes if new value is greater than or equal existing value. */
    GREATER_EQUAL,

    /** Function always passes. */
    ALWAYS,
}

/** Specific type of a sample in a texture binding. */
enum class TextureSampleType {
    /**
     * Sampling returns floats.
     *
     * ```wgsl
     * @group(0) @binding(0)
     * var t: texture_2d<f32>;
     * ```
     */
    FLOAT,

    /**
     * Sampling does the depth reference comparison. This is also compatible with a non-filtering
     * sampler.
     *
     * ```wgsl
     * @group(0) @binding(0)
     * var t: texture_depth_2d;
     * ```
     */
    DEPTH,

    /**
     * Sampling returns signed integers.
     *
     * ```wgsl
     * @group(0) @binding(0)
     * var t: texture_2d<i32>;
     * ```
     */
    SINT,

    /**
     * Sampleing returns unsigned integers.
     *
     * ```wgsl
     * @group(0) @binding(0)
     * var t: texture_2d<u32>;
     * ```
     */
    UINT,
}

/** Specific type of a sampler binding. */
enum class SamplerBindingType {
    /**
     * The sampling result is produced based on more than a single color sample from a texture, e.g.
     * when bilinear interpolation is enabled.
     */
    FILTERING,

    /** The sampling result is produced based on a single color sample from a texture. */
    NON_FILTERING,

    /**
     * Use as a comparison sampler instead of a normal sampler. For more info take a look at the
     * analogous functionality in
     * [OpenGL](https://www.khronos.org/opengl/wiki/Sampler_Object#Comparison_mode).
     */
    COMPARISON,
}

/** A color write mask. Disabled color channels will not be written to. */
@JvmInline
value class ColorWriteMask(val usageFlag: Int) {

    infix fun or(other: ColorWriteMask): ColorWriteMask =
        ColorWriteMask(usageFlag or other.usageFlag)

    infix fun and(other: ColorWriteMask): ColorWriteMask =
        ColorWriteMask(usageFlag and other.usageFlag)

    companion object {
        /** Disable writes to all channels. */
        val NONE: ColorWriteMask = ColorWriteMask(0x0)

        /** Enable red channel writes. */
        val RED: ColorWriteMask = ColorWriteMask(0x1)

        /** Enable green channel writes. */
        val GREEN: ColorWriteMask = ColorWriteMask(0x2)

        /** Enable blue channel writes. */
        val BLUE: ColorWriteMask = ColorWriteMask(0x4)

        /** Enable alpha channel writes. */
        val ALPHA: ColorWriteMask = ColorWriteMask(0x8)

        /** Enable writes to all channels. */
        val ALL: ColorWriteMask = ColorWriteMask(0xF)
    }
}

/** Specifies how the alpha channel of the texture should be handled during compositing. */
enum class AlphaMode {
    /**
     * Chooses either [OPAQUE] or [INHERIT] automatically，depending on the `alphaMode` that the
     * current surface can support.
     */
    AUTO,

    /**
     * The alpha channel, if it exists, of the textures is ignored in the compositing process.
     * Instead, the textures is treated as if it has a constant alpha of 1.0.
     */
    OPAQUE,

    /**
     * The alpha channel, if it exists, of the textures is respected in the compositing process. The
     * non-alpha channels of the textures are expected to already be multiplied by the alpha channel
     * by the application.
     */
    PREMULTIPLIED,

    /**
     * The alpha channel, if it exists, of the textures is respected in the compositing process. The
     * non-alpha channels of the textures are not expected to already be multiplied by the alpha
     * channel by the application; instead, the compositor will multiply the non-alpha channels of
     * the texture by the alpha channel during compositing.
     */
    UNPREMULTIPLIED,

    /**
     * The alpha channel, if it exists, of the textures is unknown for processing during
     * compositing. Instead, the application is responsible for setting the composite alpha blending
     * mode using native WSI command. If not set, then a platform-specific default will be used.
     */
    INHERIT;

    companion object
}

/** Behavior of the presentation engine based on frame rate. */
enum class PresentMode {
    /**
     * Presentation frames are kept in a First-In-First-Out queue approximately 3 frames long. Every
     * vertical blanking period, the presentation engine will pop a frame off the queue to display.
     * If there is no frame to display, it will present the same frame again until the next vblank.
     *
     * When a present command is executed on the gpu, the presented image is added on the queue.
     *
     * No tearing will be observed.
     *
     * Calls to `getCurrentTexture` will block until there is a spot in the queue.
     *
     * Supported on all platforms.
     *
     * If you don’t know what mode to choose, choose this mode. This is traditionally called “Vsync
     * On”.
     */
    FIFO,

    /**
     * Presentation frames are kept in a First-In-First-Out queue approximately 3 frames long. Every
     * vertical blanking period, the presentation engine will pop a frame off the queue to display.
     * If there is no frame to display, it will present the same frame until there is a frame in the
     * queue. The moment there is a frame in the queue, it will immediately pop the frame off the
     * queue.
     *
     * When a present command is executed on the gpu, the presented image is added on the queue.
     *
     * Tearing will be observed if frames last more than one vblank as the front buffer.
     *
     * Calls to `getCurrentTexture` will block until there is a spot in the queue.
     *
     * Supported on AMD on Vulkan.
     *
     * This is traditionally called “Adaptive Vsync”
     */
    FIFO_RELAXED,

    /**
     * Presentation frames are not queued at all. The moment a present command is executed on the
     * GPU, the presented image is swapped onto the front buffer immediately.
     *
     * Tearing can be observed.
     *
     * Supported on most platforms except older DX12 and Wayland.
     *
     * This is traditionally called “Vsync Off”.
     */
    IMMEDIATE,

    /**
     * Presentation frames are kept in a single-frame queue. Every vertical blanking period, the
     * presentation engine will pop a frame from the queue. If there is no frame to display, it will
     * present the same frame again until the next vblank.
     *
     * When a present command is executed on the gpu, the frame will be put into the queue. If there
     * was already a frame in the queue, the new frame will replace the old frame on the queue.
     *
     * No tearing will be observed.
     *
     * Supported on DX12 on Windows 10, NVidia on Vulkan and Wayland on Vulkan.
     *
     * This is traditionally called “Fast Vsync”
     */
    MAILBOX,
}

/** Status of the received surface texture. */
enum class TextureStatus {
    /** No issues. */
    SUCCESS,

    /** Unable to get the next frame, timed out. */
    TIMEOUT,

    /** The surface under the swap chain has changed. */
    OUTDATED,

    /** The surface under the swap chain is lost */
    LOST,

    /** The surface under the swap chain has ran out of memory. */
    OUT_OF_MEMORY,

    /** The surface under the swap chain lost the device. */
    DEVICE_LOST;

    companion object
}

/**
 * The usages determine what kind of memory the buffer is allocated from and what actions the buffer
 * can partake in.
 */
@JvmInline
value class BufferUsage(val usageFlag: Int) {

    infix fun or(other: BufferUsage): BufferUsage = BufferUsage(usageFlag or other.usageFlag)

    infix fun and(other: BufferUsage): BufferUsage = BufferUsage(usageFlag and other.usageFlag)

    companion object {

        /**
         * Allow a buffer to be mapped for reading. Does not need to be enabled for
         * mapped_at_creation.
         */
        val MAP_READ: BufferUsage = BufferUsage(0x0001)

        /**
         * Allow a buffer to be mapped for writing. Does not need to be enabled for
         * mapped_at_creation.
         */
        val MAP_WRITE: BufferUsage = BufferUsage(0x0002)

        /**
         * Allow a buffer to be the source buffer for [CommandEncoder.copyBufferToBuffer] or
         * [CommandEncoder.copyBufferToTexture]
         */
        val COPY_SRC: BufferUsage = BufferUsage(0x0004)

        /** Allow a buffer to be the destination buffer for [CommandEncoder.copyBufferToBuffer] */
        val COPY_DST: BufferUsage = BufferUsage(0x0008)

        /** Allow a buffer to be used as index buffer for draw calls */
        val INDEX: BufferUsage = BufferUsage(0x0010)

        /** Allow a buffer to be used as vertex buffer for draw calls */
        val VERTEX: BufferUsage = BufferUsage(0x0020)

        /** Allow a buffer to be used as uniform buffer */
        val UNIFORM: BufferUsage = BufferUsage(0x0040)

        /** Allows a buffer to be used as a storage buffer */
        val STORAGE: BufferUsage = BufferUsage(0x0080)

        /** Allow a buffer to be the indirect buffer in an indirect draw call. */
        val INDIRECT: BufferUsage = BufferUsage(0x0100)

        /**
         * Allow a buffer to be the destination buffer for a [CommandEncoder.resolveQuerySet]
         * operation.
         */
        val QUERY_RESOLVE: BufferUsage = BufferUsage(0x0200)
    }
}

/** Type of buffer mapping. */
@JvmInline
value class MapMode(val usageFlag: Int) {

    infix fun or(other: MapMode): MapMode = MapMode(usageFlag or other.usageFlag)

    infix fun and(other: MapMode): MapMode = MapMode(usageFlag and other.usageFlag)

    companion object {
        /** Map only for reading. */
        val READ: MapMode = MapMode(0x0001)

        /** Map only for writing. */
        val WRITE: MapMode = MapMode(0x0002)
    }
}

/**
 * Describes the shader stages that a binding will be visible from. These can be combined so that
 * something is visible from both vertex and fragment shaders:
 *
 * `ShaderStage.VERTEX or ShaderStage.FRAGMENT`
 */
@JvmInline
value class ShaderStage(val usageFlag: Int) {
    infix fun or(other: ShaderStage): ShaderStage = ShaderStage(usageFlag or other.usageFlag)

    infix fun and(other: ShaderStage): ShaderStage = ShaderStage(usageFlag and other.usageFlag)

    companion object {
        /** Binding visible from the vertex shader of a render pipeline. */
        val VERTEX: ShaderStage = ShaderStage(0x1)

        /** Binding visible from the fragment shader of a render pipeline. */
        val FRAGMENT: ShaderStage = ShaderStage(0x2)

        /** Binding visible from the compute shader of a compute pipeline. */
        val COMPUTE: ShaderStage = ShaderStage(0x4)
    }
}

/**
 * Each Feature identifies a set of functionality which, if available, allows additional usages of
 * WebGPU that would have otherwise been invalid.
 */
enum class Feature {
    /** Allows depth clipping to be disabled. */
    DEPTH_CLIP_CONTROL,

    /** Allows for explicit creation of textures of format "depth32float-stencil8". */
    DEPTH32FLOAT_STENCIL18,

    /**
     * Allows for explicit creation of textures of BC compressed formats. Supports both 2D and 3D
     * textures.
     */
    TEXTURE_COMPRESSION_BC,

    /**
     * Allows for explicit creation of textures of ETC2 compressed formats. Only supports 2D
     * textures.
     */
    TEXTURE_COMPRESSION_ETC2,

    /**
     * Allows for explicit creation of textures of ASTC compressed formats. Only supports 2D
     * textures.
     */
    TEXTURE_COMPRESSION_ASTC,

    /** Adds the ability to query timestamps from GPU command buffers. */
    TIMESTAMP_QUERY,

    /**
     * Allows the use of non-zero firstInstance values in indirect draw parameters and indirect
     * drawIndexed parameters.
     */
    INDIRECT_FIRST_INSTANCE,

    /** Allows the use of the half-precision floating-point type f16 in WGSL. */
    SHADER_F16,

    /**
     * Allows the RENDER_ATTACHMENT usage on textures with format "rg11b10ufloat", and also allows
     * textures of that format to be blended and multisampled.
     */
    RG11B10UFLOAT_RENDERABLE,

    /** Allows the STORAGE_BINDING usage on textures with format "bgra8unorm". */
    BGRA8UNORM_STORAGE,

    /** Makes textures with formats "r32float", "rg32float", and "rgba32float" filterable. */
    FLOAT32_FILTERABLE,

    /** Allows the use of clip_distances in WGSL. */
    CLIP_DISTANCES,

    /**
     * Allows the use of blend_src in WGSL and simultaneously using both pixel shader outputs
     * (@blend_src(0) and @blend_src(1)) as inputs to a blending operation with the single color
     * attachment at location 0.
     */
    DUAL_SOURCE_BLENDING,
}
