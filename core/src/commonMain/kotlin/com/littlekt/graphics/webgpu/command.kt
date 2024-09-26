package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.graphics.Color
import io.ygdrasil.wgpu.VertexFormat

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
    val origin: Origin3D = Origin3D(0, 0, 0)
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
        copySize: Extent3D
    )

    fun beginComputePass(label: String? = null): ComputePassEncoder

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
    val label: String? = null
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
    val bias: DepthBiasState = DepthBiasState(0, 0f, 0f)
) {
    companion object {
        fun depthWrite(format: TextureFormat): DepthStencilState =
            DepthStencilState(
                format = format,
                depthWriteEnabled = true,
                depthCompare = CompareFunction.LESS_EQUAL
            )

        fun depthRead(format: TextureFormat): DepthStencilState =
            DepthStencilState(
                format = format,
                depthWriteEnabled = false,
                depthCompare = CompareFunction.LESS_EQUAL
            )

        fun stencilWrite(format: TextureFormat): DepthStencilState =
            DepthStencilState(
                format = format,
                depthWriteEnabled = true,
                depthCompare = CompareFunction.LESS_EQUAL,
                stencil = StencilState(StencilFaceState.WRITE, StencilFaceState.WRITE, 0xF, 0xF)
            )

        fun stencilRead(format: TextureFormat): DepthStencilState =
            DepthStencilState(
                format = format,
                depthWriteEnabled = true,
                depthCompare = CompareFunction.LESS_EQUAL,
                stencil = StencilState(StencilFaceState.READ, StencilFaceState.READ, 0xF, 0xF)
            )
    }
}

data class StencilState(
    val front: StencilFaceState,
    val back: StencilFaceState,
    val readMask: Int,
    val writeMask: Int
)

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
    val passOp: StencilOperation
) {
    companion object {
        val IGNORE: StencilFaceState =
            StencilFaceState(
                compare = CompareFunction.ALWAYS,
                failOp = StencilOperation.KEEP,
                depthFailOp = StencilOperation.KEEP,
                passOp = StencilOperation.KEEP
            )

        val WRITE: StencilFaceState =
            StencilFaceState(
                compare = CompareFunction.ALWAYS,
                failOp = StencilOperation.KEEP,
                depthFailOp = StencilOperation.KEEP,
                passOp = StencilOperation.REPLACE
            )

        val READ: StencilFaceState =
            StencilFaceState(
                compare = CompareFunction.EQUAL,
                failOp = StencilOperation.KEEP,
                depthFailOp = StencilOperation.KEEP,
                passOp = StencilOperation.KEEP
            )
    }
}

data class DepthBiasState(val constant: Int, val slopeScale: Float, val clamp: Float)

data class VertexState(
    val module: ShaderModule,
    val entryPoint: String,
    val buffers: List<WebGPUVertexBufferLayout> = emptyList()
) {
    constructor(
        module: ShaderModule,
        entryPoint: String,
        buffer: WebGPUVertexBufferLayout
    ) : this(module, entryPoint, listOf(buffer))
}

data class PrimitiveState(
    val topology: PrimitiveTopology = PrimitiveTopology.TRIANGLE_LIST,
    val stripIndexFormat: IndexFormat? = null,
    val frontFace: FrontFace = FrontFace.CCW,
    val cullMode: CullMode = CullMode.NONE,
)

data class FragmentState(
    val module: ShaderModule,
    val entryPoint: String,
    val targets: List<ColorTargetState>
) {
    constructor(
        module: ShaderModule,
        entryPoint: String,
        target: ColorTargetState
    ) : this(module, entryPoint, listOf(target))
}

data class ColorTargetState(
    val format: TextureFormat,
    val blendState: BlendState?,
    val writeMask: ColorWriteMask
)

data class BlendState(
    val color: BlendComponent = BlendComponent(),
    val alpha: BlendComponent = BlendComponent()
) {
    companion object {
        /** Standard alpha blending. */
        val Alpha: BlendState =
            BlendState(
                color = BlendComponent(dstFactor = BlendFactor.ONE_MINUS_SRC_ALPHA),
                alpha = BlendComponent(dstFactor = BlendFactor.ONE_MINUS_SRC_ALPHA)
            )

        /** Fully oqaque, no alpha, blending. */
        val Opaque: BlendState = BlendState()

        /** Non-premultiplied, alpha blending. */
        val NonPreMultiplied: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.SRC_ALPHA,
                        dstFactor = BlendFactor.ONE_MINUS_SRC_ALPHA
                    ),
                alpha =
                    BlendComponent(
                        srcFactor = BlendFactor.SRC_ALPHA,
                        dstFactor = BlendFactor.ONE_MINUS_SRC_ALPHA
                    )
            )

        val Add: BlendState =
            BlendState(
                color =
                    BlendComponent(srcFactor = BlendFactor.SRC_ALPHA, dstFactor = BlendFactor.ONE),
                alpha =
                    BlendComponent(srcFactor = BlendFactor.SRC_ALPHA, dstFactor = BlendFactor.ONE)
            )

        val Subtract: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.SRC_ALPHA,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.REVERSE_SUBTRACT
                    ),
                alpha =
                    BlendComponent(
                        srcFactor = BlendFactor.SRC_ALPHA,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.REVERSE_SUBTRACT
                    ),
            )

        val Difference: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE_MINUS_DST_COLOR,
                        dstFactor = BlendFactor.ONE_MINUS_SRC_COLOR,
                        operation = BlendOperation.ADD
                    ),
            )

        val Multiply: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.DST_COLOR,
                        dstFactor = BlendFactor.ZERO,
                        operation = BlendOperation.ADD
                    ),
                alpha =
                    BlendComponent(
                        srcFactor = BlendFactor.DST_ALPHA,
                        dstFactor = BlendFactor.ZERO,
                        operation = BlendOperation.ADD
                    )
            )

        val Lighten: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.MAX
                    ),
                alpha =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.MAX
                    ),
            )

        val Darken: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.MIN
                    ),
                alpha =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.MIN
                    ),
            )

        val Screen: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE_MINUS_DST_COLOR,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.ADD
                    )
            )

        val LinearDodge: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.ADD
                    )
            )

        val LinearBurn: BlendState =
            BlendState(
                color =
                    BlendComponent(
                        srcFactor = BlendFactor.ONE,
                        dstFactor = BlendFactor.ONE,
                        operation = BlendOperation.REVERSE_SUBTRACT
                    )
            )
    }
}

data class WebGPUVertexAttribute(
    val format: VertexFormat,
    val offset: Long,
    val shaderLocation: Int
)

data class WebGPUVertexBufferLayout(
    val arrayStride: Long,
    val stepMode: VertexStepMode,
    val attributes: List<WebGPUVertexAttribute>
)

data class BlendComponent(
    var srcFactor: BlendFactor = BlendFactor.ONE,
    var dstFactor: BlendFactor = BlendFactor.ZERO,
    var operation: BlendOperation = BlendOperation.ADD
)

expect class RenderPassEncoder : Releasable {

    val label: String?


    fun draw(vertexCount: Int, instanceCount: Int, firstVertex: Int = 0, firstInstance: Int = 0)

    fun end()


    fun setVertexBuffer(
        slot: Int,
        buffer: GPUBuffer,
        offset: Long = 0,
        size: Long = buffer.size - offset
    )

    fun drawIndexed(
        indexCount: Int,
        instanceCount: Int,
        firstIndex: Int = 0,
        baseVertex: Int = 0,
        firstInstance: Int = 0
    )

    fun setIndexBuffer(
        buffer: GPUBuffer,
        indexFormat: IndexFormat,
        offset: Long = 0,
        size: Long = buffer.size - offset
    )

    fun setScissorRect(x: Int, y: Int, width: Int, height: Int)

    override fun release()
}

data class RenderPassColorAttachmentDescriptor(
    val view: TextureView,
    val loadOp: LoadOp,
    val storeOp: StoreOp,
    val clearColor: Color? = null,
    val resolveTarget: TextureView? = null
)

data class RenderPassDepthStencilAttachmentDescriptor(
    val view: TextureView,
    val depthClearValue: Float,
    val depthLoadOp: LoadOp?,
    val depthStoreOp: StoreOp?,
    val depthReadOnly: Boolean = false,
    val stencilClearValue: Int = 0,
    val stencilLoadOp: LoadOp? = null,
    val stencilStoreOp: StoreOp? = null,
    val stencilReadOnly: Boolean = false
)

data class RenderPassDescriptor(
    val colorAttachments: List<RenderPassColorAttachmentDescriptor>,
    val depthStencilAttachment: RenderPassDepthStencilAttachmentDescriptor? = null,
    val label: String? = null
)

expect class ComputePipeline : Releasable {
    override fun release()
}

expect class ComputePassEncoder : Releasable {

    fun dispatchWorkgroups(workgroupCountX: Int, workgroupCountY: Int = 1, workgroupCountZ: Int = 1)

    override fun release()
}
