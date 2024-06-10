package com.littlekt.graphics.webgpu

import com.littlekt.Releasable

actual class CommandBuffer : Releasable {
    actual override fun release() {}
}

actual class CommandEncoder : Releasable {
    actual fun beginRenderPass(desc: RenderPassDescriptor): RenderPassEncoder {
        TODO("Not yet implemented")
    }

    actual fun finish(): CommandBuffer {
        TODO("Not yet implemented")
    }

    actual fun copyBufferToTexture(
        source: BufferCopyView,
        destination: TextureCopyView,
        copySize: Extent3D
    ) {}

    actual fun beginComputePass(): ComputePassEncoder {
        TODO("Not yet implemented")
    }

    actual fun copyBufferToBuffer(
        source: GPUBuffer,
        destination: GPUBuffer,
        sourceOffset: Int,
        destinationOffset: Int,
        size: Long
    ) {}

    actual fun copyTextureToBuffer(source: TextureCopyView, dest: BufferCopyView, size: Extent3D) {}

    actual override fun release() {}
}

actual class RenderPipeline : Releasable {
    actual override fun release() {}
}

actual class RenderPassEncoder : Releasable {
    actual fun setPipeline(pipeline: RenderPipeline) {}

    actual fun draw(vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int) {}

    actual fun end() {}

    actual fun setVertexBuffer(slot: Int, buffer: GPUBuffer, offset: Long, size: Long) {}

    actual fun drawIndexed(
        indexCount: Int,
        instanceCount: Int,
        firstIndex: Int,
        baseVertex: Int,
        firstInstance: Int
    ) {}

    actual fun setIndexBuffer(
        buffer: GPUBuffer,
        indexFormat: IndexFormat,
        offset: Long,
        size: Long
    ) {}

    actual fun setBindGroup(index: Int, bindGroup: BindGroup) {}

    actual fun setViewport(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        minDepth: Float,
        maxDepth: Float
    ) {}

    actual fun setScissorRect(x: Int, y: Int, width: Int, height: Int) {}

    actual override fun release() {}
}

actual class ComputePipeline : Releasable {
    actual override fun release() {}
}

actual class ComputePassEncoder : Releasable {
    actual fun setPipeline(pipeline: ComputePipeline) {}

    actual fun setBindGroup(index: Int, bindGroup: BindGroup) {}

    actual fun dispatchWorkgroups(
        workgroupCountX: Int,
        workgroupCountY: Int,
        workgroupCountZ: Int
    ) {}

    actual fun end() {}

    actual override fun release() {}
}
