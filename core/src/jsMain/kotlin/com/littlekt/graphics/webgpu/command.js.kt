package com.littlekt.graphics.webgpu

import com.littlekt.EngineStats
import com.littlekt.Releasable

actual class CommandBuffer(val delegate: GPUCommandBuffer) : Releasable {
    actual override fun release() {}
}

actual class CommandEncoder(val delegate: GPUCommandEncoder) : Releasable {

    actual fun beginRenderPass(desc: RenderPassDescriptor): RenderPassEncoder {
        return RenderPassEncoder(delegate.beginRenderPass(desc.toNative()), desc.label)
    }

    actual fun finish(): CommandBuffer {
        return CommandBuffer(delegate.finish())
    }

    actual fun copyBufferToTexture(
        source: BufferCopyView,
        destination: TextureCopyView,
        copySize: Extent3D,
    ) {
        delegate.copyBufferToTexture(source.toNative(), destination.toNative(), copySize.toNative())
    }

    actual fun beginComputePass(label: String?): ComputePassEncoder {
        return ComputePassEncoder(delegate.beginComputePass(GPUObjectBase { this.label = label }))
    }

    actual fun copyTextureToBuffer(source: TextureCopyView, dest: BufferCopyView, size: Extent3D) {
        delegate.copyTextureToBuffer(source.toNative(), dest.toNative(), size.toNative())
    }

    actual override fun release() {}
}

actual class RenderPassEncoder(
    val delegate: GPURenderPassEncoder,
    actual val label: String? = null,
) : Releasable {

    actual fun draw(vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int) {
        EngineStats.drawCalls++
        EngineStats.triangles += (vertexCount / 3) * instanceCount
        delegate.draw(vertexCount, instanceCount, firstVertex, firstInstance)
    }

    actual fun end() {
        delegate.end()
    }

    actual fun setVertexBuffer(slot: Int, buffer: GPUBuffer, offset: Long, size: Long) {
        delegate.setVertexBuffer(slot.toLong(), buffer.delegate, offset, size)
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
        delegate.drawIndexed(indexCount, instanceCount, firstIndex, baseVertex, firstInstance)
    }

    actual fun setIndexBuffer(
        buffer: GPUBuffer,
        indexFormat: IndexFormat,
        offset: Long,
        size: Long,
    ) {
        delegate.setIndexBuffer(buffer.delegate, indexFormat.nativeVal, offset, size)
        EngineStats.setBufferCalls++
    }

    actual fun setScissorRect(x: Int, y: Int, width: Int, height: Int) {
        delegate.setScissorRect(x, y, width, height)
    }

    actual override fun release() = Unit

    override fun toString(): String {
        return "RenderPassEncoder(label=$label)"
    }
}

actual class ComputePipeline : Releasable {
    actual override fun release() = Unit
}

actual class ComputePassEncoder(val delegate: GPUComputePassEncoder) : Releasable {

    actual fun dispatchWorkgroups(
        workgroupCountX: Int,
        workgroupCountY: Int,
        workgroupCountZ: Int,
    ) {
        delegate.dispatchWorkgroups(workgroupCountX, workgroupCountY, workgroupCountZ)
    }

    actual override fun release() = Unit
}
