package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.graphics.Color

/**
 * A handle to a command buffer on the GPU.
 *
 * A `CommandBuffer` represents a complete sequence of commands that may be submitted to a command
 * queue with [Queue.submit]. A `CommandBuffer` is obtained by recording a series of commands to a
 * [CommandEncoder] and then calling [CommandEncoder.finish].
 */
expect class CommandBuffer : Releasable {
    override fun release()
}

/**
 * View of a texture which can be used to copy to/from a buffer/texture.
 *
 * @param texture the texture to be copied to/from.
 * @param mipLevel the target mip level of the texture.
 * @param origin the base texel of the texture in the select [mipLevel].
 */
data class TextureCopyView(
    val texture: WebGPUTexture,
    val mipLevel: Int = 0,
    val origin: Origin3D = Origin3D(0, 0, 0),
)

/**
 * Layout of a texture in a buffer's memory.
 *
 * The bytes per row and rows per image can be hard to figure otu so here are some examples:
 *
 * |Resolution|Format|Bytes per block|Pixels per block|Bytes per row                   |Rows per image  |
 * |----------|------|---------------|----------------|--------------------------------|----------------|
 * |256x256   |RGBA8 |4              |1 * 1 * 1       |256 * 4 = 1024                  |None            |
 * |32x16x8   |RGBA8 |4              |1 * 1 * 1       |32 * 4 = 128 padded to 256 = 256|None            |
 * |256x256   |BC3   |16             |4 * 4 * 1       |16 * (256 / 4) = 1024 = 1024    |None            |
 * |64x64x8   |BC3   |16             |4 * 4 * 1       |16 * (64 / 4) = 256 = 256       |64 / 4 = 16 = 16|
 *
 * @param bytesPerRow Bytes per “row” in an image. A row is one row of pixels or of compressed
 *   blocks in the x direction. This value is required if there are multiple rows (i.e. height or
 *   depth is more than one pixel or pixel block for compressed textures). Must be a multiple of 256
 *   for [CommandEncoder.copyBufferToTexture] and [CommandEncoder.copyTextureToBuffer]. You must
 *   manually pad the image such that this is a multiple of 256. It will not affect the image data.
 *   [Queue.writeTexture] does not have this requirement. Must be a multiple of the texture block
 *   size. For non-compressed textures, this is 1.
 * @param rowsPerImage “Rows” that make up a single “image”. A row is one row of pixels or of
 *   compressed blocks in the x direction. An image is one layer in the z direction of a 3D image or
 *   2DArray texture. The amount of rows per image may be larger than the actual amount of rows of
 *   data. Required if there are multiple images (i.e. the depth is more than one).
 * @param offset Offset into the buffer that is the start of the texture. Must be a multiple of
 *   texture block size. For non-compressed textures, this is 1.
 */
data class TextureDataLayout(val bytesPerRow: Int, val rowsPerImage: Int, val offset: Long = 0)

/**
 * View of a buffer which can be used to copy to/from a texture.
 *
 * @param buffer the buffer to be copied to/from.
 * @param layout the layout of the texture data in this buffer.
 */
data class BufferCopyView(val buffer: GPUBuffer, val layout: TextureDataLayout)

/**
 * Encodes a series of GPU operations.
 *
 * A command encoder can record [RenderPassEncoder]es, [ComputePassEncoder]es, and transfer
 * operations between driver-managed resources like [GPUBuffer]s and [WebGPUTexture]s.
 *
 * When finished recording, call [CommandEncoder.finish] to obtain a [CommandBuffer] which may be
 * submitted for execution.
 */
expect class CommandEncoder : Releasable {

    /**
     * Begins recording of a [RenderPassEncoder].
     *
     * @return a [RenderPassEncoder] which records a single render pass.
     */
    fun beginRenderPass(desc: RenderPassDescriptor): RenderPassEncoder

    /**
     * Finishes recording.
     *
     * @return a [CommandBuffer] that can be submitted for execution.
     */
    fun finish(): CommandBuffer

    /** Copy data from a buffer to a texture. */
    fun copyBufferToTexture(
        source: BufferCopyView,
        destination: TextureCopyView,
        copySize: Extent3D,
    )

    /**
     * Begins recording of a compute pass.
     *
     * @param label debug label for a [ComputePassEncoder]
     * @return a [ComputePassEncoder] which records a single compute pass.
     */
    fun beginComputePass(label: String? = null): ComputePassEncoder

    /** Copy data from one buffer to another. */
    fun copyBufferToBuffer(
        source: GPUBuffer,
        destination: GPUBuffer,
        sourceOffset: Int = 0,
        destinationOffset: Int = 0,
        size: Long = destination.size - destinationOffset,
    )

    /** Copy data from a texture to a buffer. */
    fun copyTextureToBuffer(source: TextureCopyView, dest: BufferCopyView, size: Extent3D)

    override fun release()
}

/**
 * Describes a programmable pipeline stage.
 *
 * @param module the compiled shader module for this stage
 * @param entryPoint the name of the entry point in the compiled shader. There must be a function
 *   with this name in the shader.
 */
data class ProgrammableStage(val module: ShaderModule, val entryPoint: String)

/**
 * Describes a render (graphics) pipeline.
 *
 * For use with [Device.createRenderPipeline]
 *
 * @param layout The layout of bind groups for this pipeline.
 * @param vertex The compiled vertex stage, its entry point, and the input buffers layout.
 * @param fragment The compiled fragment stage, its entry point, and the color targets.
 * @param primitive The properties of the pipeline at the primitive assembly and rasterization
 *   level.
 * @param depthStencil The effect of draw calls on the depth and stencil aspects of the output
 *   target, if any.
 * @param multisample The multi-sampling properties of the pipeline.
 * @param label debug label of a buffer.
 */
data class RenderPipelineDescriptor(
    val layout: PipelineLayout,
    val vertex: VertexState,
    val fragment: FragmentState?,
    val primitive: PrimitiveState = PrimitiveState(),
    val depthStencil: DepthStencilState? = null,
    val multisample: MultisampleState = MultisampleState(),
    val label: String? = null,
)

/**
 * Describes the multi-sampling state of a render pipeline.
 *
 * @param count the number of samples calculated per pixel (for MSAA). For non-multisampled
 *   textures, this should be `1`.
 * @param mask bitmask that restricts the samples of a pixel modifier by this pipeline. All samples
 *   can be enabled using `0xFFFFFFF`
 * @param alphaToCoverageEnabled when enabled, produces another sample mask per pixel based on the
 *   alpha output value, that is ANDed with the [mask] and the primitive coverage to restrict the
 *   set of samples affected by a primitive.
 *
 * The implicit mask produced for alpha of zero is guaranteed to be zero, and for alpha of one is
 * guaranteed to be all 1-s.
 */
data class MultisampleState(
    val count: Int = 1,
    val mask: Int = 0xFFFFFFF,
    val alphaToCoverageEnabled: Boolean = false,
)

/**
 * Describes the depth/stencil state in a render pipeline.
 *
 * @param format format of the depth/stencil buffer must be special depth format. Must match the
 *   format of the depth/stencil attachment in [CommandEncoder.beginRenderPass].
 * @param depthWriteEnabled if disabled, depth will not be written to.
 * @param depthCompare comparison function used to compare depth values in the depth test.
 * @param stencil the stencil state.
 * @param bias the depth bias state.
 */
data class DepthStencilState(
    val format: TextureFormat,
    val depthWriteEnabled: Boolean,
    val depthCompare: CompareFunction,
    val stencil: StencilState =
        StencilState(StencilFaceState.IGNORE, StencilFaceState.IGNORE, 0xF, 0Xf),
    val bias: DepthBiasState = DepthBiasState(0, 0f, 0f),
) {
    companion object {
        /**
         * Creates a new [DepthStencilState] meant for depth writes.
         *
         * ```
         * DepthStencilState(
         *                 format = format,
         *                 depthWriteEnabled = true,
         *                 depthCompare = CompareFunction.LESS_EQUAL,
         *             )
         * ```
         */
        fun depthWrite(format: TextureFormat): DepthStencilState =
            DepthStencilState(
                format = format,
                depthWriteEnabled = true,
                depthCompare = CompareFunction.LESS_EQUAL,
            )

        /**
         * Creates a new [DepthStencilState] meant for depth reads.
         *
         * ```
         * DepthStencilState(
         *                 format = format,
         *                 depthWriteEnabled = false,
         *                 depthCompare = CompareFunction.LESS_EQUAL,
         *             )
         * ```
         */
        fun depthRead(format: TextureFormat): DepthStencilState =
            DepthStencilState(
                format = format,
                depthWriteEnabled = false,
                depthCompare = CompareFunction.LESS_EQUAL,
            )

        /**
         * Creates a new [DepthStencilState] meant for stencil writes.
         *
         * ```
         *     DepthStencilState(
         *                 format = format,
         *                 depthWriteEnabled = true,
         *                 depthCompare = CompareFunction.LESS_EQUAL,
         *                 stencil = StencilState(StencilFaceState.WRITE, StencilFaceState.WRITE, 0xF, 0xF),
         *             )
         * ```
         */
        fun stencilWrite(format: TextureFormat): DepthStencilState =
            DepthStencilState(
                format = format,
                depthWriteEnabled = true,
                depthCompare = CompareFunction.LESS_EQUAL,
                stencil = StencilState(StencilFaceState.WRITE, StencilFaceState.WRITE, 0xF, 0xF),
            )

        /**
         * Creates a new [DepthStencilState] meant for stencil reads.
         *
         * ```
         *  DepthStencilState(
         *                 format = format,
         *                 depthWriteEnabled = true,
         *                 depthCompare = CompareFunction.LESS_EQUAL,
         *                 stencil = StencilState(StencilFaceState.READ, StencilFaceState.READ, 0xF, 0xF),
         *             )
         * ```
         */
        fun stencilRead(format: TextureFormat): DepthStencilState =
            DepthStencilState(
                format = format,
                depthWriteEnabled = true,
                depthCompare = CompareFunction.LESS_EQUAL,
                stencil = StencilState(StencilFaceState.READ, StencilFaceState.READ, 0xF, 0xF),
            )
    }
}

/**
 * State of the stencil operation (fixed-pipeline stage).
 *
 * For use in [DepthStencilState].
 *
 * @param front front face mode.
 * @param back back face mode.
 * @param readMask stencil values are AND'd with this mask when reading and writing from the stencil
 *   buffer. Only lower 8 bits are used.
 * @param writeMask stencil values are AND'd with this mask when writing to the stencil buffer. Only
 *   lower 8 bits are used.
 */
data class StencilState(
    val front: StencilFaceState,
    val back: StencilFaceState,
    val readMask: Int,
    val writeMask: Int,
)

/**
 * Describes the stencil state in a render pipeline.
 *
 * If you are not using a stencil state, set this to [StencilFaceState.IGNORE].
 */
data class StencilFaceState(
    /**
     * Comparison function that determines if the [failOp] or [passOp] is used on the stencil
     * buffer.
     */
    val compare: CompareFunction,
    /** Operation that is performed when stencil test fails. */
    val failOp: StencilOperation,
    /** Operation that is performed when depth test fails but stencil test succeeds. */
    val depthFailOp: StencilOperation,
    /** Operation that is performed when stencil test succeeds. */
    val passOp: StencilOperation,
) {
    companion object {
        /** Ignore the stencil state for the face. */
        val IGNORE: StencilFaceState =
            StencilFaceState(
                compare = CompareFunction.ALWAYS,
                failOp = StencilOperation.KEEP,
                depthFailOp = StencilOperation.KEEP,
                passOp = StencilOperation.KEEP,
            )

        /** Write stencil state for the face. */
        val WRITE: StencilFaceState =
            StencilFaceState(
                compare = CompareFunction.ALWAYS,
                failOp = StencilOperation.KEEP,
                depthFailOp = StencilOperation.KEEP,
                passOp = StencilOperation.REPLACE,
            )

        /** Read stencil state for the face. */
        val READ: StencilFaceState =
            StencilFaceState(
                compare = CompareFunction.EQUAL,
                failOp = StencilOperation.KEEP,
                depthFailOp = StencilOperation.KEEP,
                passOp = StencilOperation.KEEP,
            )
    }
}

/**
 * Describes the biasing setting for the depth target.
 *
 * For use in [DepthStencilState].
 *
 * @param constant constant depth biasing factor, in basic units of the depth format
 * @param slopeScale slope depth biasing factor.
 * @param clamp depth bias clamp value (absolute).
 */
data class DepthBiasState(val constant: Int, val slopeScale: Float, val clamp: Float)

/**
 * Describes the vertex processing in a render pipeline.
 *
 * For use in [RenderPipelineDescriptor].
 *
 * @param module the compiled shader module for this stage
 * @param entryPoint The name of the entry point in the compiled shader. There must be a function
 *   with this name in the shader.
 * @param buffers The format of any vertex buffers used with this pipeline.
 */
data class VertexState(
    val module: ShaderModule,
    val entryPoint: String,
    val buffers: List<WebGPUVertexBufferLayout> = emptyList(),
) {

    /**
     * Describes the vertex processing in a render pipeline.
     *
     * For use in [RenderPipelineDescriptor].
     *
     * @param module the compiled shader module for this stage
     * @param entryPoint The name of the entry point in the compiled shader. There must be a
     *   function with this name in the shader.
     * @param buffer The format of any vertex buffers used with this pipeline.
     */
    constructor(
        module: ShaderModule,
        entryPoint: String,
        buffer: WebGPUVertexBufferLayout,
    ) : this(module, entryPoint, listOf(buffer))
}

/**
 * Describes the state of primitive assembly and rasterization in a render pipeline.
 *
 * @param topology the primitive topology used to interpret vertices.
 * @param stripIndexFormat when drawing strip topologies with indices, this is the required format
 *   for the index buffer. This has no effect on non-indexed or non-strip draws.
 * @param frontFace the face to consider the front for the purpose of culling and stencil
 *   operations.
 * @param cullMode the face culling mode.
 */
data class PrimitiveState(
    val topology: PrimitiveTopology = PrimitiveTopology.TRIANGLE_LIST,
    val stripIndexFormat: IndexFormat? = null,
    val frontFace: FrontFace = FrontFace.CCW,
    val cullMode: CullMode = CullMode.NONE,
)

/**
 * Describes the fragment processing in a render pipeline.
 *
 * For use in [RenderPipelineDescriptor].
 *
 * @param module the compiled shader module for this stage.
 * @param entryPoint the name of the entry point in the compiled shader. There must be a function
 *   with this name in the shader.
 * @param targets the color state of the render targets.
 * @param targets the color state of the render targets.
 */
data class FragmentState(
    val module: ShaderModule,
    val entryPoint: String,
    val targets: List<ColorTargetState>,
) {

    /**
     * Describes the fragment processing in a render pipeline.
     *
     * For use in [RenderPipelineDescriptor].
     *
     * @param module the compiled shader module for this stage.
     * @param entryPoint the name of the entry point in the compiled shader. There must be a
     *   function with this name in the shader.
     * @param target the color state of the render targets.
     */
    constructor(
        module: ShaderModule,
        entryPoint: String,
        target: ColorTargetState,
    ) : this(module, entryPoint, listOf(target))
}

/**
 * Describes the color state of a render pipeline.
 *
 * @param format the [TextureFormat] of the image that this pipeline will render to. Must match the
 *   format of the corresponding color attachment in [CommandEncoder.beginRenderPass].
 * @param blendState the blending that is used for this pipeline.
 * @param writeMask mask which enables/disables writes to different color/alpha channels.
 */
data class ColorTargetState(
    val format: TextureFormat,
    val blendState: BlendState?,
    val writeMask: ColorWriteMask,
)

/**
 * Describe the blend state of a render pipeline, with [ColorTargetState].
 *
 * @param color the color equation
 * @param alpha the alpha equation
 */
data class BlendState(
    val color: BlendComponent = BlendComponent(),
    val alpha: BlendComponent = BlendComponent(),
) {
    companion object {
        /** Standard alpha blending. */
        val Alpha: BlendState =
            BlendState(
                color = BlendComponent(dstFactor = BlendFactor.ONE_MINUS_SRC_ALPHA),
                alpha = BlendComponent(dstFactor = BlendFactor.ONE_MINUS_SRC_ALPHA),
            )

        /** Fully oqaque, no alpha, blending. */
        val Opaque: BlendState = BlendState()

        /** Non-premultiplied, alpha blending. */
        val NonPreMultiplied: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.SRC_ALPHA,
                        dstFactor = BlendFactor.ONE_MINUS_SRC_ALPHA,
                    ),
                alpha =
                    BlendComponent(
                        srcFactor = BlendFactor.SRC_ALPHA,
                        dstFactor = BlendFactor.ONE_MINUS_SRC_ALPHA,
                    ),
            )

        val Add: BlendState =
            BlendState(
                color =
                    BlendComponent(srcFactor = BlendFactor.SRC_ALPHA, dstFactor = BlendFactor.ONE),
                alpha =
                    BlendComponent(srcFactor = BlendFactor.SRC_ALPHA, dstFactor = BlendFactor.ONE),
            )

        val Subtract: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.SRC_ALPHA,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.REVERSE_SUBTRACT,
                    ),
                alpha =
                    BlendComponent(
                        srcFactor = BlendFactor.SRC_ALPHA,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.REVERSE_SUBTRACT,
                    ),
            )

        val Difference: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE_MINUS_DST_COLOR,
                        dstFactor = BlendFactor.ONE_MINUS_SRC_COLOR,
                        operation = BlendOperation.ADD,
                    )
            )

        val Multiply: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.DST_COLOR,
                        dstFactor = BlendFactor.ZERO,
                        operation = BlendOperation.ADD,
                    ),
                alpha =
                    BlendComponent(
                        srcFactor = BlendFactor.DST_ALPHA,
                        dstFactor = BlendFactor.ZERO,
                        operation = BlendOperation.ADD,
                    ),
            )

        val Lighten: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.MAX,
                    ),
                alpha =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.MAX,
                    ),
            )

        val Darken: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.MIN,
                    ),
                alpha =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.MIN,
                    ),
            )

        val Screen: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE_MINUS_DST_COLOR,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.ADD,
                    )
            )

        val LinearDodge: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.ADD,
                    )
            )

        val LinearBurn: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.REVERSE_SUBTRACT,
                    )
            )
    }
}

/**
 * Vertex inputs (attributes) to shaders.
 *
 * @param format the format of the input
 * @param offset byte offset of the start of the input
 * @param shaderLocation location for this input. Must match the location in the shader.
 */
data class WebGPUVertexAttribute(
    val format: VertexFormat,
    val offset: Long,
    val shaderLocation: Int,
)

/**
 * Describes how the vertex buffer is interpreted.
 *
 * For use in [VertexState].
 *
 * @param arrayStride the stride, in bytes, between elements of this buffer.
 * @param stepMode how often this vertex buffer is "stepped" forward.
 * @param attributes the list of attributes which comprise a single vertex.
 */
data class WebGPUVertexBufferLayout(
    val arrayStride: Long,
    val stepMode: VertexStepMode,
    val attributes: List<WebGPUVertexAttribute>,
) {
    constructor(
        arrayStride: Long,
        stepMode: VertexStepMode,
        attribute: WebGPUVertexAttribute,
    ) : this(arrayStride, stepMode, listOf(attribute))
}

/**
 * Describes a blend component of a [BlendState].
 *
 * @param srcFactor multiplier for the source, which is produced by the fragment shader.
 * @param dstFactor multiplier for the destination, which is stored in the target.
 * @param operation the binary operation applied to the source and destination, multiplied by their
 *   respective factors.
 */
data class BlendComponent(
    var srcFactor: BlendFactor = BlendFactor.ONE,
    var dstFactor: BlendFactor = BlendFactor.ZERO,
    var operation: BlendOperation = BlendOperation.ADD,
)

/**
 * Handle to a rendering (graphics) pipeline,
 *
 * A `RenderPipeline` object represents a graphics pipeline and its stages, bindings, vertex buffers
 * and targets. It can be created with [Device.createRenderPipeline].
 */
expect class RenderPipeline : Releasable {
    override fun release()
}

/**
 * An in-progress recording of a render pass: a list of render commands in a [CommandEncoder].
 *
 * It can be created with [CommandEncoder.beginRenderPass], whose [RenderPassDescriptor] specifies
 * the attachments (textures) that will be rendered to.
 *
 * Most of the methods on `RenderPassEncoder` serve one of two purposes, identifiable by their
 * names:
 * * `draw*()`: drawing (that is, encoding a render command, which, when executed by the GPU, will
 *   rasterize something and execute shaders).
 * * `set*()`: setting part of the render state for future drawing commands.
 *
 * A render pass may contain any number of drawing commands, and before/between each command the
 * render state may be updated however you wish; each drawing command will be executed using the
 * render state that has been set when the `draw*()` function is called.
 */
expect class RenderPassEncoder : Releasable {
    /** A label that can be used to identify the object in error messages or warnings. */
    val label: String?

    /**
     * Sets the active render pipeline.
     *
     * Subsequent draw calls will exhibit the behavior defined by pipeline.
     */
    fun setPipeline(pipeline: RenderPipeline)

    /**
     * Draws primitives using the active vertex buffers.
     *
     * The active vertex buffers can be set with [setVertexBuffer].
     *
     * Fails if indices range is outside of the range of the vertices range of any set vertex
     * buffer.
     *
     * This drawing command uses the current render state, as set by preceding `set*()` methods. It
     * is not affected by changes to the state that are performed after it is called.
     *
     * @param vertexCount the amount of vertices to draw
     * @param instanceCount Range of Instances to draw. Use 0..1 if instance buffers are not used.
     *   E.g.of how its used internally
     * @param firstVertex the offset into the vertex buffers, in vertices, to begin drawing from
     * @param firstInstance a number defining the first instance to draw.
     */
    fun draw(vertexCount: Int, instanceCount: Int, firstVertex: Int = 0, firstInstance: Int = 0)

    /** Completes recording of the render pass commands sequence. */
    fun end()

    /**
     * Assign a vertex buffer to a slot.
     *
     * Subsequent calls to [draw] and [drawIndexed] on this [RenderPassEncoder] will use [buffer] as
     * one of the source vertex buffers.
     *
     * The [slot] refers to the index of the matching descriptor in [VertexState.buffers].
     */
    fun setVertexBuffer(
        slot: Int,
        buffer: GPUBuffer,
        offset: Long = 0,
        size: Long = buffer.size - offset,
    )

    /**
     * Draws indexed primitives using the active index buffer and the active vertex buffers.
     *
     * The active index buffer can be set with [setIndexBuffer] The active vertex buffers can be set
     * with [setVertexBuffer].
     *
     * Fails if indices range is outside of the range of the indices range of any set index buffer.
     *
     * This drawing command uses the current render state, as set by preceding `set*()` methods. It
     * is not affected by changes to the state that are performed after it is called.
     *
     * @param indexCount the amount of indices to draw
     * @param instanceCount Range of Instances to draw. Use 0..1 if instance buffers are not used.
     *   E.g.of how its used internally
     * @param firstIndex the offset to of the indices to begin drawing from
     * @param baseVertex value added to each index value before indexing into the vertex buffers.
     * @param firstInstance a number defining the first instance to draw.
     */
    fun drawIndexed(
        indexCount: Int,
        instanceCount: Int,
        firstIndex: Int = 0,
        baseVertex: Int = 0,
        firstInstance: Int = 0,
    )

    /**
     * Sets the active index buffer.
     *
     * Subsequent calls to [drawIndexed] on this [RenderPassEncoder] will use buffer as the source
     * index buffer.
     */
    fun setIndexBuffer(
        buffer: GPUBuffer,
        indexFormat: IndexFormat,
        offset: Long = 0,
        size: Long = buffer.size - offset,
    )

    /**
     * Sets the active bind group for a given bind group index. The bind group layout in the active
     * pipeline when any `draw*()` method is called must match the layout of this bind group.
     *
     * If the bind group have dynamic offsets, provide them in binding order.
     *
     * Subsequent draw calls’ shader executions will be able to access data in these bind groups.
     */
    fun setBindGroup(index: Int, bindGroup: BindGroup, dynamicOffsets: List<Long> = emptyList())

    /**
     * Sets the viewport used during the rasterization stage to linear map from normalized device
     * coordinates to viewport coordinates.
     *
     * @param x min x value of the viewport in pixels
     * @param y min y value of the viewport in pixels
     * @param width width of the viewport in pixels
     * @param height height of the viewport in pixels
     * @param minDepth min depth value of the viewport
     * @param maxDepth max depth value of the viewport
     */
    fun setViewport(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        minDepth: Float = 0f,
        maxDepth: Float = 1f,
    )

    /**
     * Sets the scissor rectangle used during the rasterization stage. After transformation into
     * viewport coordinates any fragments which fall outside the scissor rectnagle will be
     * discarded.
     *
     * @param x min x value of the scissor rectangle in pixels
     * @param y min y value of the scissor rectangle in pixels
     * @param width width of the scissor rectangle in pixels
     * @param height height of the scissor rectangle in pixels
     */
    fun setScissorRect(x: Int, y: Int, width: Int, height: Int)

    override fun release()
}

/**
 * Describes a color attachment to a [RenderPassEncoder].
 *
 * For use with a [RenderPassDescriptor].
 *
 * @param view the view to use as an attachment
 * @param loadOp the load operation that will be performed on this color attachment.
 * @param storeOp the store operation that will be performed on this color attachment.
 * @param clearColor the color to clear the view
 * @param resolveTarget the view that will receive the resolved output if multisampling is used. If
 *   set, it is always written to, regardless of how [loadOp] or [storeOp] is configured.
 * @param depthSlice index of a 3D view. It must not be provided if the view is not 3D.
 */
data class RenderPassColorAttachmentDescriptor(
    val view: TextureView,
    val loadOp: LoadOp,
    val storeOp: StoreOp,
    val clearColor: Color? = null,
    val resolveTarget: TextureView? = null,
    val depthSlice: Int? = null
)

/**
 * Describes a depth/stencil attachment to a [RenderPassEncoder].
 *
 * For use with a [RenderPassDescriptor].
 *
 * @param view the view to use as an attachment.
 * @param depthClearValue indicates the value to clear the views depth component prior to executing
 *   the render pass. Ignored if [depthLoadOp] is not [LoadOp.CLEAR]. Must be between `0f` and `1f`.
 * @param depthLoadOp the load operation that will be performed on the depth part of this
 *   attachment.
 * @param depthStoreOp the store operation that will be performed on the depth part of this
 *   attachment.
 * @param depthReadOnly Indicates that the depth component of [view] is read only.
 * @param stencilClearValue indicates the value to clear the views stencil component prior to
 *   executing the render pass. Ignored if [stencilLoadOp] is not [LoadOp.CLEAR].
 * @param stencilLoadOp the load operation that will be performed on the stencil part of this
 *   attachment.
 * @param stencilStoreOp the store operation that will be performed on the stencil part of this
 *   attachment.
 * @param stencilReadOnly Indicates that the stencil component of [view] is read only.
 */
data class RenderPassDepthStencilAttachmentDescriptor(
    val view: TextureView,
    val depthClearValue: Float,
    val depthLoadOp: LoadOp?,
    val depthStoreOp: StoreOp?,
    val depthReadOnly: Boolean = false,
    val stencilClearValue: Int = 0,
    val stencilLoadOp: LoadOp? = null,
    val stencilStoreOp: StoreOp? = null,
    val stencilReadOnly: Boolean = false,
)

/**
 * Describes the attachments of a render pass.
 *
 * For use with [CommandEncoder.beginRenderPass].
 *
 * @param colorAttachments the color attachments of the render pass.
 * @param depthStencilAttachment the depth stencil attachment of the render pass, if any.
 * @param label debug label of a [RenderPassEncoder].
 */
data class RenderPassDescriptor(
    val colorAttachments: List<RenderPassColorAttachmentDescriptor>,
    val depthStencilAttachment: RenderPassDepthStencilAttachmentDescriptor? = null,
    val label: String? = null,
)

/**
 * Describes a compute pipeline. For use with [Device.createComputePipeline].
 *
 * @param layout the layout of bind groups for this pipeline.
 * @param compute the programmable stage descriptor for this pipeline
 * @param label debug label of a [ComputePipeline].
 */
data class ComputePipelineDescriptor(
    val layout: PipelineLayout,
    val compute: ProgrammableStage,
    val label: String? = null,
)

/**
 * Handle to a compute pipeline.
 *
 * A `ComputePipeline` object represents a compute pipeline and its single shader stage. It can be
 * created with [Device.createComputePipeline].
 */
expect class ComputePipeline : Releasable {
    override fun release()
}

/**
 * In-progress recording of a compute pass. It can be created with
 * [CommandEncoder.beginComputePass].
 */
expect class ComputePassEncoder : Releasable {

    /** Sets the active compute pipeline. */
    fun setPipeline(pipeline: ComputePipeline)

    /**
     * Sets the active bind group for a given bind group index. This bind group layout in the active
     * pipeline when the `[dispatchWorkgroups]` function is called must match the layout of this
     * bind group.
     */
    fun setBindGroup(index: Int, bindGroup: BindGroup)

    /**
     * Dispatches the compute work operations.
     *
     * @param workgroupCountX denote number of work groups in x-dimension
     * @param workgroupCountY denote number of work groups in y-dimension
     * @param workgroupCountZ denote number of work groups in z-dimension
     */
    fun dispatchWorkgroups(workgroupCountX: Int, workgroupCountY: Int = 1, workgroupCountZ: Int = 1)

    /** End the recording of this compute pass. */
    fun end()

    override fun release()
}
