package com.littlekt.graphics.webgpu

import com.littlekt.EngineStats
import com.littlekt.Releasable
import ffi.MemoryAllocator
import io.ygdrasil.wgpu.WGPUCommandBuffer
import io.ygdrasil.wgpu.WGPUCommandEncoder
import io.ygdrasil.wgpu.WGPUComputePassEncoder
import io.ygdrasil.wgpu.WGPUComputePipeline
import io.ygdrasil.wgpu.WGPUOrigin3D
import io.ygdrasil.wgpu.WGPURenderPassEncoder
import io.ygdrasil.wgpu.WGPURenderPipeline
import io.ygdrasil.wgpu.WGPUTexelCopyTextureInfo
import io.ygdrasil.wgpu.wgpuCommandBufferRelease
import io.ygdrasil.wgpu.wgpuCommandEncoderCopyBufferToTexture
import io.ygdrasil.wgpu.wgpuCommandEncoderFinish
import io.ygdrasil.wgpu.wgpuCommandEncoderRelease
import io.ygdrasil.wgpu.wgpuComputePassEncoderDispatchWorkgroups
import io.ygdrasil.wgpu.wgpuComputePassEncoderEnd
import io.ygdrasil.wgpu.wgpuComputePassEncoderRelease
import io.ygdrasil.wgpu.wgpuComputePassEncoderSetBindGroup
import io.ygdrasil.wgpu.wgpuComputePassEncoderSetPipeline
import io.ygdrasil.wgpu.wgpuComputePipelineRelease
import io.ygdrasil.wgpu.wgpuRenderPassEncoderDraw
import io.ygdrasil.wgpu.wgpuRenderPassEncoderDrawIndexed
import io.ygdrasil.wgpu.wgpuRenderPassEncoderEnd
import io.ygdrasil.wgpu.wgpuRenderPassEncoderRelease
import io.ygdrasil.wgpu.wgpuRenderPassEncoderSetBindGroup
import io.ygdrasil.wgpu.wgpuRenderPassEncoderSetIndexBuffer
import io.ygdrasil.wgpu.wgpuRenderPassEncoderSetPipeline
import io.ygdrasil.wgpu.wgpuRenderPassEncoderSetScissorRect
import io.ygdrasil.wgpu.wgpuRenderPassEncoderSetVertexBuffer
import io.ygdrasil.wgpu.wgpuRenderPassEncoderSetViewport
import io.ygdrasil.wgpu.wgpuRenderPipelineRelease
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.ValueLayout

actual class RenderPipeline(val segment: WGPURenderPipeline) : Releasable {

    actual override fun release() {
        wgpuRenderPipelineRelease(segment)
    }

    override fun toString(): String {
        return "RenderPipeline"
    }
}

actual class CommandBuffer(val segment: WGPUCommandBuffer) : Releasable {
    actual override fun release() {
        wgpuCommandBufferRelease(segment)
    }
}

fun TextureCopyView.toNative(scope: MemoryAllocator): WGPUTexelCopyTextureInfo {
    return scope.map(this)
}

fun TextureDataLayout.toNative(scope: SegmentAllocator): MemorySegment {
    val native = WGPUTextureDataLayout.allocate(scope)

    WGPUTextureDataLayout.bytesPerRow(native, bytesPerRow)
    WGPUTextureDataLayout.rowsPerImage(native, rowsPerImage)
    WGPUTextureDataLayout.offset(native, offset)

    return native
}

fun BufferCopyView.toNative(scope: SegmentAllocator): MemorySegment {
    val native = WGPUImageCopyBuffer.allocate(scope)
    val layoutNative = WGPUImageCopyBuffer.layout(native)

    WGPUImageCopyBuffer.buffer(native, buffer.segment)
    WGPUTextureDataLayout.bytesPerRow(layoutNative, layout.bytesPerRow)
    WGPUTextureDataLayout.rowsPerImage(layoutNative, layout.rowsPerImage)
    WGPUTextureDataLayout.offset(layoutNative, layout.offset)

    return native
}

actual class CommandEncoder(val segment: WGPUCommandEncoder) : Releasable {

    actual fun beginRenderPass(desc: RenderPassDescriptor): RenderPassEncoder {
        return Arena.ofConfined().use { scope ->
            WGPURenderPassDescriptor.allocate(scope).let { renderPassDesc ->
                WGPURenderPassDescriptor.label(
                    renderPassDesc,
                    desc.label?.toNativeString(scope) ?: WGPU_NULL,
                )
                WGPURenderPassDescriptor.colorAttachmentCount(
                    renderPassDesc,
                    desc.colorAttachments.size.toLong(),
                )
                WGPURenderPassDescriptor.colorAttachments(
                    renderPassDesc,
                    desc.colorAttachments.mapToNativeEntries(
                        scope,
                        WGPURenderPassColorAttachment.sizeof(),
                        WGPURenderPassColorAttachment::allocateArray,
                    ) { colorAttachment, nativeColorAttachment ->
                        WGPURenderPassColorAttachment.view(
                            nativeColorAttachment,
                            colorAttachment.view.segment,
                        )
                        WGPURenderPassColorAttachment.loadOp(
                            nativeColorAttachment,
                            colorAttachment.loadOp.nativeVal,
                        )
                        WGPURenderPassColorAttachment.storeOp(
                            nativeColorAttachment,
                            colorAttachment.storeOp.nativeVal,
                        )
                        colorAttachment.clearColor?.let { clearColor ->
                            WGPUColor.allocate(scope)
                                .also {
                                    WGPUColor.r(it, clearColor.r.toDouble())
                                    WGPUColor.g(it, clearColor.g.toDouble())
                                    WGPUColor.b(it, clearColor.b.toDouble())
                                    WGPUColor.a(it, clearColor.a.toDouble())
                                }
                                .let {
                                    WGPURenderPassColorAttachment.clearValue(
                                        nativeColorAttachment,
                                        it,
                                    )
                                }
                        }
                    },
                )

                desc.depthStencilAttachment?.let { depthStencilAttachment ->
                    val nativeDepthStencilAttachment =
                        WGPURenderPassDepthStencilAttachment.allocate(scope)

                    WGPURenderPassDepthStencilAttachment.view(
                        nativeDepthStencilAttachment,
                        depthStencilAttachment.view.segment,
                    )
                    WGPURenderPassDepthStencilAttachment.depthClearValue(
                        nativeDepthStencilAttachment,
                        depthStencilAttachment.depthClearValue,
                    )
                    depthStencilAttachment.depthLoadOp?.nativeVal?.let {
                        WGPURenderPassDepthStencilAttachment.depthLoadOp(
                            nativeDepthStencilAttachment,
                            it,
                        )
                    }

                    depthStencilAttachment.depthStoreOp?.nativeVal?.let {
                        WGPURenderPassDepthStencilAttachment.depthStoreOp(
                            nativeDepthStencilAttachment,
                            it,
                        )
                    }
                    WGPURenderPassDepthStencilAttachment.depthReadOnly(
                        nativeDepthStencilAttachment,
                        depthStencilAttachment.depthReadOnly.toInt(),
                    )
                    WGPURenderPassDepthStencilAttachment.stencilClearValue(
                        nativeDepthStencilAttachment,
                        depthStencilAttachment.stencilClearValue,
                    )

                    depthStencilAttachment.stencilLoadOp?.nativeVal?.let {
                        WGPURenderPassDepthStencilAttachment.stencilLoadOp(
                            nativeDepthStencilAttachment,
                            it,
                        )
                    }

                    depthStencilAttachment.stencilStoreOp?.nativeVal?.let {
                        WGPURenderPassDepthStencilAttachment.stencilStoreOp(
                            nativeDepthStencilAttachment,
                            it,
                        )
                    }
                    WGPURenderPassDepthStencilAttachment.depthReadOnly(
                        nativeDepthStencilAttachment,
                        depthStencilAttachment.stencilReadOnly.toInt(),
                    )

                    WGPURenderPassDescriptor.depthStencilAttachment(
                        renderPassDesc,
                        nativeDepthStencilAttachment,
                    )
                }
                RenderPassEncoder(
                    wgpuCommandEncoderBeginRenderPass(segment, renderPassDesc),
                    desc.label,
                )
            }
        }
    }

    actual fun finish(): CommandBuffer {
        return CommandBuffer(wgpuCommandEncoderFinish(segment, null) ?: error("Failed to finish command encoder."))
    }

    actual fun copyBufferToTexture(
        source: BufferCopyView,
        destination: TextureCopyView,
        copySize: Extent3D,
    ) {
        Arena.ofConfined().use { scope ->
            wgpuCommandEncoderCopyBufferToTexture(
                segment,
                source.toNative(scope),
                destination.toNative(scope),
                copySize.toNative(scope),
            )
        }
    }

    actual fun beginComputePass(label: String?): ComputePassEncoder {
        return Arena.ofConfined().use { scope ->
            val desc = WGPUComputePipelineDescriptor.allocate(scope)
            WGPUComputePipelineDescriptor.label(desc, label?.toNativeString(scope) ?: WGPU_NULL)
            ComputePassEncoder(wgpuCommandEncoderBeginComputePass(segment, desc))
        }
    }

    actual fun copyBufferToBuffer(
        source: GPUBuffer,
        destination: GPUBuffer,
        sourceOffset: Int,
        destinationOffset: Int,
        size: Long,
    ) {
        wgpuCommandEncoderCopyBufferToBuffer(
            segment,
            source.segment,
            sourceOffset.toLong(),
            destination.segment,
            destinationOffset.toLong(),
            size,
        )
    }

    actual fun copyTextureToBuffer(source: TextureCopyView, dest: BufferCopyView, size: Extent3D) {
        TODO("IMPL")
    }

    actual override fun release() {
        wgpuCommandEncoderRelease(segment)
    }
}

actual class RenderPassEncoder(val segment: WGPURenderPassEncoder, actual val label: String? = null) :
    Releasable {

    actual fun setPipeline(pipeline: RenderPipeline) {
        wgpuRenderPassEncoderSetPipeline(segment, pipeline.segment)
        EngineStats.setPipelineCalls += 1
    }

    actual fun draw(vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int) {
        EngineStats.drawCalls += 1
        EngineStats.triangles += (vertexCount / 3) * instanceCount
        wgpuRenderPassEncoderDraw(segment, vertexCount.toUInt(), instanceCount.toUInt(), firstVertex.toUInt(), firstInstance.toUInt())
    }

    actual fun end() {
        wgpuRenderPassEncoderEnd(segment)
    }

    actual fun setVertexBuffer(slot: Int, buffer: GPUBuffer, offset: Long, size: Long) {
        wgpuRenderPassEncoderSetVertexBuffer(segment, slot.toUInt(), buffer.segment, offset.toULong(), size.toULong())
        EngineStats.setBufferCalls += 1
    }

    actual fun drawIndexed(
        indexCount: Int,
        instanceCount: Int,
        firstIndex: Int,
        baseVertex: Int,
        firstInstance: Int,
    ) {
        EngineStats.drawCalls += 1
        EngineStats.triangles += (indexCount / 3) * instanceCount
        wgpuRenderPassEncoderDrawIndexed(
            segment,
            indexCount,
            instanceCount,
            firstIndex,
            baseVertex,
            firstInstance,
        )
    }

    actual fun setIndexBuffer(
        buffer: GPUBuffer,
        indexFormat: IndexFormat,
        offset: Long,
        size: Long,
    ) {
        wgpuRenderPassEncoderSetIndexBuffer(
            segment,
            buffer.segment,
            indexFormat.nativeVal,
            offset,
            size,
        )
        EngineStats.setBufferCalls += 1
    }

    actual fun setBindGroup(index: Int, bindGroup: BindGroup, dynamicOffsets: List<Long>) {
        if (dynamicOffsets.isNotEmpty()) {
            Arena.ofConfined().use { scope ->
                val offsets =
                    scope.allocateFrom(ValueLayout.JAVA_LONG, *dynamicOffsets.toLongArray())
                wgpuRenderPassEncoderSetBindGroup(
                    segment,
                    index,
                    bindGroup.segment,
                    dynamicOffsets.size.toLong(),
                    offsets,
                )
            }
        } else {
            wgpuRenderPassEncoderSetBindGroup(segment, index, bindGroup.segment, 0, WGPU_NULL)
        }
        EngineStats.setBindGroupCalls += 1
    }

    actual override fun release() {
        wgpuRenderPassEncoderRelease(segment)
    }

    actual fun setViewport(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        minDepth: Float,
        maxDepth: Float,
    ) {
        wgpuRenderPassEncoderSetViewport(
            segment,
            x.toFloat(),
            y.toFloat(),
            width.toFloat(),
            height.toFloat(),
            minDepth,
            maxDepth,
        )
    }

    actual fun setScissorRect(x: Int, y: Int, width: Int, height: Int) {
        wgpuRenderPassEncoderSetScissorRect(segment, x, y, width, height)
    }

    override fun toString(): String {
        return "RenderPassEncoder(label=$label)"
    }
}

actual class ComputePipeline(val segment: WGPUComputePipeline) : Releasable {
    actual override fun release() {
        wgpuComputePipelineRelease(segment)
    }

    override fun toString(): String {
        return "ComputePipeline"
    }
}

actual class ComputePassEncoder(val segment: WGPUComputePassEncoder) : Releasable {
    actual fun setPipeline(pipeline: ComputePipeline) {
        wgpuComputePassEncoderSetPipeline(segment, pipeline.segment)
        EngineStats.setPipelineCalls += 1
    }

    actual fun setBindGroup(index: Int, bindGroup: BindGroup) {
        wgpuComputePassEncoderSetBindGroup(segment, index, bindGroup.segment, 0, WGPU_NULL)
        EngineStats.setBindGroupCalls += 1
    }

    actual fun dispatchWorkgroups(
        workgroupCountX: Int,
        workgroupCountY: Int,
        workgroupCountZ: Int,
    ) {
        wgpuComputePassEncoderDispatchWorkgroups(
            segment,
            workgroupCountX,
            workgroupCountY,
            workgroupCountZ,
        )
    }

    actual fun end() {
        wgpuComputePassEncoderEnd(segment)
    }

    actual override fun release() {
        wgpuComputePassEncoderRelease(segment)
    }
}

internal fun MemoryAllocator.map(input: TextureCopyView) =
    WGPUTexelCopyTextureInfo.allocate(this).also { output ->
        output.texture = input.texture.segment
        output.mipLevel = input.mipLevel.toUInt()
        map(input.origin, output.origin)
    }


internal fun map(input: Origin3D, output: WGPUOrigin3D) {
    output.x = input.x.toUInt()
    output.y = input.y.toUInt()
    output.z = input.z.toUInt()
}