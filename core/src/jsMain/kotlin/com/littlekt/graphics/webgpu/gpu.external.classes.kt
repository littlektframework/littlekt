package com.littlekt.graphics.webgpu

import kotlin.js.Promise
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView

external val navigator: Navigator

external class Navigator {
    val gpu: GPU
}

external class GPU {
    /**
     * @return A string indicating a canvas texture format. The value can be `rgba8unorm` or
     *   `bgra8unorm`.
     */
    fun getPreferredCanvasFormat(): String

    fun requestAdapter(options: GPURequestAdapterOptions = definedExternally): Promise<GPUAdapter>
}

external class GPUCanvasContext {
    fun configure(configuration: GPUCanvasConfiguration)

    fun getCurrentTexture(): GPUTexture
}

open external class GPUAdapter {
    val name: String

    val features: dynamic

    val limits: GPUSupportedLimits

    fun requestDevice(device: GPUDeviceDescriptor?): Promise<GPUDevice>
}

external class GPUDevice {
    val features: dynamic

    val limits: GPUSupportedLimits

    val queue: GPUQueue

    fun createShaderModule(desc: GPUShaderModuleDescriptor): GPUShaderModule

    fun createPipelineLayout(desc: GPUPipelineLayoutDescriptor): GPUPipelineLayout

    fun createRenderPipeline(desc: GPURenderPipelineDescriptor): GPURenderPipeline

    fun createTexture(desc: GPUTextureDescriptor): GPUTexture

    fun createCommandEncoder(desc: GPUObjectBase): GPUCommandEncoder

    fun createBuffer(desc: GPUBufferDescriptor): GPUBufferJs

    fun createBindGroupLayout(desc: GPUBindGroupLayoutDescriptor): GPUBindGroupLayout

    fun createBindGroup(desc: GPUBindGroupDescriptor): GPUBindGroup

    fun createSampler(desc: GPUSamplerDescriptor): GPUSampler

    fun createComputePipeline(desc: GPUComputePipelineDescriptor): GPUComputePipeline
}

external class GPUQueue {

    fun submit(cmdBuffers: Array<GPUCommandBuffer>)

    fun writeBuffer(
        buffer: GPUBufferJs,
        offset: Long,
        data: ArrayBufferView,
        dataOffset: Long,
        size: Long
    )

    fun writeTexture(
        destination: GPUImageCopyTexture,
        data: ArrayBufferView,
        dataLayout: GPUImageDataLayout,
        size: GPUExtent3D
    )
}

external object GPUMapMode {
    val READ: Long
    val WRITE: Long
}

external class GPUBufferJs {

    fun mapAsync(mode: Long): Promise<dynamic>

    fun getMappedRange(offset: Long, size: Long): ArrayBuffer

    fun unmap()

    fun destroy()
}

external interface GPUBindingResource

external class GPUTextureView : GPUBindingResource

external class GPUSampler : GPUBindingResource

external interface GPUBufferBinding : GPUBindingResource {
    var buffer: GPUBufferJs
    var offset: Long
    var size: Long
}

external class GPUBindGroup

external class GPUBindGroupLayout

external class GPUPipelineLayout

external class GPURenderPipeline

external class GPUComputePipeline

external class GPUCommandBuffer

external class GPUCommandEncoder {

    fun beginRenderPass(desc: GPURenderPassDescriptor): GPURenderPassEncoder

    fun finish(): GPUCommandBuffer

    fun copyBufferToTexture(
        source: GPUImageCopyBuffer,
        destination: GPUImageCopyTexture,
        copySize: GPUExtent3D
    )

    fun beginComputePass(descriptor: GPUObjectBase): GPUComputePassEncoder

    fun copyBufferToBuffer(
        source: GPUBufferJs,
        sourceOffset: Int,
        destination: GPUBufferJs,
        destinationOffset: Int,
        size: Long
    )

    fun copyTextureToBuffer(
        source: GPUImageCopyTexture,
        destination: GPUImageCopyBuffer,
        copySize: GPUExtent3D
    )
}

external class GPURenderPassEncoder {
    fun setPipeline(pipeline: GPURenderPipeline)

    fun draw(vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int)

    fun end()

    fun setVertexBuffer(slot: Long, buffer: GPUBufferJs, offset: Long, size: Long)

    fun drawIndexed(
        indexCount: Int,
        instanceCount: Int,
        firstVertex: Int,
        baseVertex: Int,
        firstInstance: Int
    )

    fun setIndexBuffer(buffer: GPUBufferJs, format: String?, offset: Long, size: Long)

    fun setBindGroup(
        index: Int,
        bindGroup: GPUBindGroup,
        dynamicOffsets: LongArray = definedExternally
    )

    fun setViewport(x: Int, y: Int, width: Int, height: Int, minDepth: Float, maxDepth: Float)

    fun setScissorRect(x: Int, y: Int, width: Int, height: Int)
}

external class GPUComputePassEncoder {
    fun setPipeline(pipeline: GPUComputePipeline)

    fun setBindGroup(index: Int, bindGroup: GPUBindGroup)

    fun dispatchWorkgroups(workgroupCountX: Int, workgroupCountY: Int, workgroupCountZ: Int)

    fun end()
}
