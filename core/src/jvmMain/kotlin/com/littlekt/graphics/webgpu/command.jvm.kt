package com.littlekt.graphics.webgpu

import com.littlekt.EngineStats
import com.littlekt.Releasable
import com.littlekt.wgpu.*
import com.littlekt.wgpu.WGPU.*
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.ValueLayout

actual class RenderPipeline(val segment: MemorySegment) : Releasable {

    actual override fun release() {
        wgpuRenderPipelineRelease(segment)
    }

    override fun toString(): String {
        return "RenderPipeline"
    }
}

actual class CommandBuffer(val segment: MemorySegment) : Releasable {
    actual override fun release() {
        wgpuCommandBufferRelease(segment)
    }
}

fun TextureCopyView.toNative(scope: SegmentAllocator): MemorySegment {
    val native = WGPUImageCopyTexture.allocate(scope)
    val nativeOrigin = WGPUImageCopyTexture.origin(native)

    WGPUImageCopyTexture.texture(native, texture.segment)
    WGPUImageCopyTexture.mipLevel(native, mipLevel)
    WGPUOrigin3D.x(nativeOrigin, origin.x)
    WGPUOrigin3D.y(nativeOrigin, origin.y)
    WGPUOrigin3D.z(nativeOrigin, origin.z)

    return native
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

actual class CommandEncoder(val segment: MemorySegment) : Releasable {

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
        return CommandBuffer(wgpuCommandEncoderFinish(segment, NULL()))
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

actual class RenderPassEncoder(val segment: MemorySegment, actual val label: String? = null) :
    Releasable {

    actual fun setPipeline(pipeline: RenderPipeline) {
        wgpuRenderPassEncoderSetPipeline(segment, pipeline.segment)
        EngineStats.setPipelineCalls++
    }

    actual fun draw(vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int) {
        EngineStats.drawCalls++
        EngineStats.triangles += (vertexCount / 3) * instanceCount
        wgpuRenderPassEncoderDraw(segment, vertexCount, instanceCount, firstVertex, firstInstance)
    }

    actual fun end() {
        wgpuRenderPassEncoderEnd(segment)
    }

    actual fun setVertexBuffer(slot: Int, buffer: GPUBuffer, offset: Long, size: Long) {
        wgpuRenderPassEncoderSetVertexBuffer(segment, slot, buffer.segment, offset, size)
        EngineStats.setBufferCalls++
    }

    actual fun drawIndexed(
        indexCount: Int,
        instanceCount: Int,
        firstIndex: Int,
        baseVertex: Int,
        firstInstance: Int,
    ) {
        EngineStats.drawCalls++
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
        EngineStats.setBufferCalls++
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
        EngineStats.setBindGroupCalls++
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

actual class ComputePipeline(val segment: MemorySegment) : Releasable {
    actual override fun release() {
        wgpuComputePipelineRelease(segment)
    }

    override fun toString(): String {
        return "ComputePipeline"
    }
}

actual class ComputePassEncoder(val segment: MemorySegment) : Releasable {
    actual fun setPipeline(pipeline: ComputePipeline) {
        wgpuComputePassEncoderSetPipeline(segment, pipeline.segment)
        EngineStats.setPipelineCalls++
    }

    actual fun setBindGroup(index: Int, bindGroup: BindGroup) {
        wgpuComputePassEncoderSetBindGroup(segment, index, bindGroup.segment, 0, WGPU_NULL)
        EngineStats.setBindGroupCalls++
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
