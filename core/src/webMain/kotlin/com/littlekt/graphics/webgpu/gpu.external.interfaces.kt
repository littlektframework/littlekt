package com.littlekt.graphics.webgpu

import org.w3c.dom.ImageBitmap
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsString

expect fun GPUObjectBase(): GPUObjectBase
external interface GPUObjectBase : JsAny {
    var label: String?
}

external interface GPUSupportedLimits {
    var maxTextureDimension1D: Int
    var maxTextureDimension2D: Int
    var maxTextureDimension3D: Int
    var maxTextureArrayLayers: Int
    var maxBindGroups: Int
    var maxBindGroupsPlusVertexBuffers: Int
    var maxBindingsPerBindGroup: Int
    var maxDynamicUniformBuffersPerPipelineLayout: Int
    var maxDynamicStorageBuffersPerPipelineLayout: Int
    var maxSampledTexturesPerShaderStage: Int
    var maxSamplersPerShaderStage: Int
    var maxStorageBuffersPerShaderStage: Int
    var maxStorageTexturesPerShaderStage: Int
    var maxUniformBuffersPerShaderStage: Int
    var maxUniformBufferBindingSize: Double
    var maxStorageBufferBindingSize: Double
    var minUniformBufferOffsetAlignment: Int
    var minStorageBufferOffsetAlignment: Int
    var maxVertexBuffers: Int
    var maxBufferSize: Double
    var maxVertexAttributes: Int
    var maxVertexBufferArrayStride: Int
    var maxInterStageShaderComponents: Int
    var maxInterStageShaderVariables: Int
    var maxColorAttachments: Int
    var maxColorAttachmentBytesPerSample: Int
    var maxComputeWorkgroupStorageSize: Int
    var maxComputeInvocationsPerWorkgroup: Int
    var maxComputeWorkgroupSizeX: Int
    var maxComputeWorkgroupSizeY: Int
    var maxComputeWorkgroupSizeZ: Int
    var maxComputeWorkgroupsPerDimension: Int
}


expect fun GPURequestAdapterOptions(): GPURequestAdapterOptions

external interface GPURequestAdapterOptions {
    var powerPreference: String
    var forceFallbackAdapter: Boolean
}

expect fun JsRequiredLimits(): JsRequiredLimits
external interface JsRequiredLimits {
    var maxTextureDimension1D : Double
    var maxTextureDimension2D : Double
    var maxTextureDimension3D : Double
    var maxTextureArrayLayers : Double
    var maxBindGroups : Double
    var maxBindGroupsPlusVertexBuffers : Double
    var maxBindingsPerBindGroup : Double
    var maxDynamicUniformBuffersPerPipelineLayout : Double
    var maxDynamicStorageBuffersPerPipelineLayout : Double
    var maxSampledTexturesPerShaderStage : Double
    var maxSamplersPerShaderStage : Double
    var maxStorageBuffersPerShaderStage : Double
    var maxStorageTexturesPerShaderStage : Double
    var maxUniformBuffersPerShaderStage : Double
    var maxUniformBufferBindingSize : Double
    var maxStorageBufferBindingSize : Double
    var minUniformBufferOffsetAlignment : Double
    var minStorageBufferOffsetAlignment : Double
    var maxVertexBuffers : Double
    var maxBufferSize : Double
    var maxVertexAttributes : Double
    var maxVertexBufferArrayStride : Double
}


expect fun GPUDeviceDescriptor(): GPUDeviceDescriptor
external interface GPUDeviceDescriptor : GPUObjectBase {
    var requiredFeatures: JsArray<JsString>
    var requiredLimits: JsRequiredLimits
}

expect fun GPUCanvasConfiguration(): GPUCanvasConfiguration
external interface GPUCanvasConfiguration : GPUObjectBase {
    var device: GPUDevice
    var format: String
    var usage: Int
    var colorSpace: String
    var alphaMode: String
    var viewFormats: JsArray<JsString>
}

expect fun GPUShaderModuleDescriptor(): GPUShaderModuleDescriptor
external interface GPUShaderModuleDescriptor : GPUObjectBase {
    var code: String
}

expect fun GPUPipelineLayoutDescriptor(): GPUPipelineLayoutDescriptor
external interface GPUPipelineLayoutDescriptor : GPUObjectBase {
    var bindGroupLayouts: JsArray<GPUBindGroupLayout>
}

external interface GPUObjectPipelineDescriptorBase : GPUObjectBase {
    var layout: GPUPipelineLayout
}

expect fun GPURenderPipelineDescriptor(): GPURenderPipelineDescriptor
external interface GPURenderPipelineDescriptor : GPUObjectPipelineDescriptorBase {
    var vertex: GPUVertexState
    var primitive: GPUPrimitiveState
    var depthStencil: GPUDepthStencilState?
    var multisample: GPUMultisampleState
    var fragment: GPUFragmentState?
}

expect fun GPUProgrammableStage(): GPUProgrammableStage
external interface GPUProgrammableStage {
    var module: GPUShaderModule
    var entryPoint: String
//    var constants: Map<String, Number>
}

expect fun GPUVertexState(): GPUVertexState
external interface GPUVertexState : GPUProgrammableStage {
    var buffers: JsArray<GPUVertexBufferLayout>
}

expect fun GPUVertexBufferLayout(): GPUVertexBufferLayout
external interface GPUVertexBufferLayout : JsAny {
    var arrayStride: Double
    var stepMode: String
    var attributes: JsArray<GPUVertexAttribute>
}

expect fun GPUVertexAttribute(): GPUVertexAttribute
external interface GPUVertexAttribute : JsAny {
    var format: String
    var offset: Double
    var shaderLocation: Int
}

expect fun GPUPrimitiveState(): GPUPrimitiveState
external interface GPUPrimitiveState {
    var topology: String
    var stripIndexFormat: String?
    var frontFace: String
    var cullMode: String
    var unclippedDepth: Boolean
}

expect fun GPUDepthStencilState(): GPUDepthStencilState
external interface GPUDepthStencilState {
    var format: String
    var depthWriteEnabled: Boolean
    var depthCompare: String
    var stencilFront: GPUStencilFaceState
    var stencilBack: GPUStencilFaceState
    var stencilReadMask: Int
    var stencilWriteMask: Int
    var depthBias: Int
    var depthBiasSlopeScale: Float
    var depthBiasClamp: Float
}

expect fun GPUStencilFaceState(): GPUStencilFaceState
external interface GPUStencilFaceState {
    var compare: String
    var failOp: String
    var depthFailOp: String
    var passOp: String
}

expect fun GPUMultisampleState(): GPUMultisampleState
external interface GPUMultisampleState {
    var count: Int
    var mask: Int
    var alphaToCoverageEnabled: Boolean
}

expect fun GPUFragmentState(): GPUFragmentState
external interface GPUFragmentState : GPUProgrammableStage {
    var targets: JsArray<GPUColorTargetState>
}

expect fun GPUColorTargetState(): GPUColorTargetState
external interface GPUColorTargetState : JsAny {
    var format: String
    var blend: GPUBlendState?
    var writeMask: Int
}

expect fun GPUBlendState(): GPUBlendState
external interface GPUBlendState {
    var color: GPUBlendComponent
    var alpha: GPUBlendComponent
}

expect fun GPUBlendComponent(): GPUBlendComponent
external interface GPUBlendComponent {
    var operation: String
    var srcFactor: String
    var dstFactor: String
}

expect fun GPUBufferDescriptor(): GPUBufferDescriptor
external interface GPUBufferDescriptor : GPUObjectBase {
    var size: Double
    var usage: Int
    var mappedAtCreation: Boolean
}

expect fun GPUBindGroupLayoutDescriptor(): GPUBindGroupLayoutDescriptor
external interface GPUBindGroupLayoutDescriptor : GPUObjectBase {
    var entries: JsArray<GPUBindGroupLayoutEntry>
}

expect fun GPUBindGroupLayoutEntry(): GPUBindGroupLayoutEntry
external interface GPUBindGroupLayoutEntry : GPUObjectBase {
    var binding: Int
    var visibility: Int
    var buffer: GPUBufferBindingLayout
    var sampler: GPUBufferSamplerBindingLayout
    var texture: GPUTextureBindingLayout
    var storageTexture: GPUStorageTextureBindingLayout
    var externalTexture: GPUExternalTextureBindingLayout
}

expect fun GPUBufferBindingLayout(): GPUBufferBindingLayout
external interface GPUBufferBindingLayout : GPUObjectBase {
    var type: String
    var hasDynamicOffset: Boolean
    var minBindingSize: Double
}

expect fun GPUBufferSamplerBindingLayout(): GPUBufferSamplerBindingLayout
external interface GPUBufferSamplerBindingLayout : GPUObjectBase {
    var type: String
}

expect fun GPUTextureBindingLayout(): GPUTextureBindingLayout
external interface GPUTextureBindingLayout : GPUObjectBase {
    var sampleType: String
    var viewDimension: String
    var multisampled: Boolean
}

external interface GPUStorageTextureBindingLayout : GPUObjectBase {
    var access: String
    var format: String
    var viewDimension: String
}

external interface GPUExternalTextureBindingLayout : GPUObjectBase

expect fun GPUBindGroupDescriptor(): GPUBindGroupDescriptor
external interface GPUBindGroupDescriptor : GPUObjectBase {
    var layout: GPUBindGroupLayout
    var entries: JsArray<GPUBindGroupEntry>
}

expect fun GPUBindGroupEntry(): GPUBindGroupEntry
external interface GPUBindGroupEntry : GPUObjectBase {
    var binding: Int

    /** [GPUSampler] or [GPUTextureView] or [GPUBufferBinding] */
    var resource: GPUBindingResource
}

expect fun GPUSamplerDescriptor(): GPUSamplerDescriptor
external interface GPUSamplerDescriptor : GPUObjectBase {
    var addressModeU: String
    var addressModeV: String
    var addressModeW: String
    var magFilter: String
    var minFilter: String
    var mipmapFilter: String
    var lodMinClamp: Float
    var lodMaxClamp: Float
    var compare: String?
    var maxAnisotropy: Short
}

expect fun GPUComputePipelineDescriptor(): GPUComputePipelineDescriptor
external interface GPUComputePipelineDescriptor : GPUObjectPipelineDescriptorBase {
    var compute: GPUProgrammableStage
}

expect fun GPUTextureDescriptor(): GPUTextureDescriptor
external interface GPUTextureDescriptor : GPUObjectBase {
    var size: GPUExtent3D
    var mipLevelCount: Int
    var sampleCount: Int
    var dimension: String
    var format: String
    var usage: Int
    var viewFormats: JsArray<JsString>
}

expect fun GPUExtent3D(): GPUExtent3D
external interface GPUExtent3D {
    var width: Int
    var height: Int
    var depthOrArrayLayer: Int
}

external interface GPUShaderModule : GPUObjectBase {
    val compilationInfo: JsAny
}

external interface GPUTexture : GPUObjectBase {
    var depthOrLayers: Int
    var dimension: String
    var format: String
    var width: Int
    var height: Int
    var mipLevelCount: Int
    var sampleCount: Int
    var usage: Int

    fun createView(desc: GPUTextureViewDescriptor?): GPUTextureView

    fun destroy()
}

expect fun GPUTextureViewDescriptor(): GPUTextureViewDescriptor
external interface GPUTextureViewDescriptor : GPUObjectBase {
    var arrayLayerCount: Int
    var aspect: String
    var baseArrayLayer: Int
    var baseMipLevel: Int
    var dimension: String
    var format: String
    var mipLevelCount: Int
}

expect fun GPURenderPassDescriptor(): GPURenderPassDescriptor
external interface GPURenderPassDescriptor : GPUObjectBase {
    var colorAttachments: JsArray<GPURenderPassColorAttachment>
    var depthStencilAttachment: GPURenderPassDepthStencilAttachment?
    var occlusionQuerySet: GPUQuerySet
    var timestampWrites: GPURenderPassTimestampWrites
    var maxDrawCount: Double
}

expect fun GPURenderPassColorAttachment(): GPURenderPassColorAttachment
external interface GPURenderPassColorAttachment : JsAny {
    var view: GPUTextureView
    var resolveTarget: GPUTextureView?
    var clearValue: GPUColorDict?
    var loadOp: String
    var storeOp: String
}

expect fun GPURenderPassDepthStencilAttachment(): GPURenderPassDepthStencilAttachment
external interface GPURenderPassDepthStencilAttachment {
    var view: GPUTextureView
    var depthClearValue: Float
    var depthLoadOp: String?
    var depthStoreOp: String?
    var depthReadOnly: Boolean
    var stencilClearValue: Int
    var stencilLoadOp: String?
    var stencilStoreOp: String?
    var stencilReadOnly: Boolean
}

external interface GPUQuerySet {
    var type: String
    var count: Int
}

external interface GPURenderPassTimestampWrites {
    var querySet: GPUQuerySet
    var beginningOfPassWriteIndex: Int
    var endOfPassWriteIndex: Int
}

expect fun GPUImageCopyTexture(): GPUImageCopyTexture
external interface GPUImageCopyTexture {
    var aspect: String
    var mipLevel: Int
    var origin: GPUOrigin3D
    var texture: GPUTexture
}

expect fun GPUImageCopyExternalImage(): GPUImageCopyExternalImage
external interface GPUImageCopyExternalImage {
    var source: ImageBitmap
}

expect fun GPUOrigin3D(): GPUOrigin3D
external interface GPUOrigin3D {
    var x: Int
    var y: Int
    var z: Int
}

expect fun GPUImageDataLayout(): GPUImageDataLayout
external interface GPUImageDataLayout {
    var offset: Double
    var bytesPerRow: Int
    var rowsPerImage: Int
}

expect fun GPUImageCopyBuffer(): GPUImageCopyBuffer
external interface GPUImageCopyBuffer : GPUImageDataLayout {
    var buffer: GPUBufferJs
}