package com.littlekt.graphics.webgpu

import kotlin.js.Promise
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView
import org.w3c.dom.RenderingContext
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsNumber
import kotlin.js.definedExternally

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

external class GPUCanvasContext : RenderingContext {
    fun configure(configuration: GPUCanvasConfiguration)

    fun getCurrentTexture(): GPUTexture
}

open external class GPUAdapter : JsAny {
    val name: String

    val features: JsGPUSupportedFeatures

    val limits: GPUSupportedLimits

    fun requestDevice(gpuDeviceDescriptor: GPUDeviceDescriptor?): Promise<GPUDevice>
}

external interface JsGPUSupportedFeatures : JsAny {
    fun has(name: String): Boolean
}

external class GPUDevice : JsAny {
    val features: JsGPUSupportedFeatures

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

    fun submit(cmdBuffers: JsArray<GPUCommandBuffer>)

    fun writeBuffer(
        buffer: GPUBufferJs,
        offset: Double,
        data: ArrayBufferView,
        dataOffset: Double,
        size: Double,
    )

    fun writeTexture(
        destination: GPUImageCopyTexture,
        data: ArrayBufferView,
        dataLayout: GPUImageDataLayout,
        size: GPUExtent3D,
    )

    fun copyExternalImageToTexture(
        source: GPUImageCopyExternalImage,
        destination: GPUImageCopyTexture,
        copySize: GPUExtent3D,
    )
}

external object GPUMapMode {
    val READ: Double
    val WRITE: Double
}

external class GPUBufferJs {

    fun mapAsync(mode: Double): Promise<JsAny>

    fun getMappedRange(offset: Double, size: Double): ArrayBuffer

    fun unmap()

    fun destroy()
}

external interface GPUBindingResource

external class GPUTextureView : GPUBindingResource

external class GPUSampler : GPUBindingResource

expect fun GPUBufferBinding(): GPUBufferBinding
external interface GPUBufferBinding : GPUBindingResource {
    var buffer: GPUBufferJs
    var offset: Double
    var size: Double
}

external class GPUBindGroup

external class GPUBindGroupLayout : JsAny

external class GPUPipelineLayout

external class GPURenderPipeline

external class GPUComputePipeline

external class GPUCommandBuffer : JsAny

external class GPUCommandEncoder {

    fun beginRenderPass(desc: GPURenderPassDescriptor): GPURenderPassEncoder

    fun finish(): GPUCommandBuffer

    fun copyBufferToTexture(
        source: GPUImageCopyBuffer,
        destination: GPUImageCopyTexture,
        copySize: GPUExtent3D,
    )

    fun beginComputePass(descriptor: GPUObjectBase): GPUComputePassEncoder

    fun copyBufferToBuffer(
        source: GPUBufferJs,
        sourceOffset: Int,
        destination: GPUBufferJs,
        destinationOffset: Int,
        size: Double,
    )

    fun copyTextureToBuffer(
        source: GPUImageCopyTexture,
        destination: GPUImageCopyBuffer,
        copySize: GPUExtent3D,
    )
}

external class GPURenderPassEncoder {
    fun setPipeline(pipeline: GPURenderPipeline)

    fun draw(vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int)

    fun end()

    fun setVertexBuffer(slot: Double, buffer: GPUBufferJs, offset: Double, size: Double)

    fun drawIndexed(
        indexCount: Int,
        instanceCount: Int,
        firstVertex: Int,
        baseVertex: Int,
        firstInstance: Int,
    )

    fun setIndexBuffer(buffer: GPUBufferJs, format: String?, offset: Double, size: Double)

    fun setBindGroup(
        index: Int,
        bindGroup: GPUBindGroup,
        dynamicOffsets: JsArray<JsNumber> = definedExternally,
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