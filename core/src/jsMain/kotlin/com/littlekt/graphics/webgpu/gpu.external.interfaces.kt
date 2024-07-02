package com.littlekt.graphics.webgpu

external interface GPUObjectBase {
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
    var maxUniformBufferBindingSize: Long
    var maxStorageBufferBindingSize: Long
    var minUniformBufferOffsetAlignment: Int
    var minStorageBufferOffsetAlignment: Int
    var maxVertexBuffers: Int
    var maxBufferSize: Long
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

external interface GPURequestAdapterOptions {
    var powerPreference: String
    var forceFallbackAdapter: Boolean
}

external interface GPUCanvasConfiguration : GPUObjectBase {
    var device: GPUDevice
    var format: String
    var usage: Int
    var colorSpace: String
    var alphaMode: String
    var viewFormats: Array<String>
}

external interface GPUShaderModuleDescriptor : GPUObjectBase {
    var code: String
}

external interface GPUPipelineLayoutDescriptor : GPUObjectBase {
    var bindGroupLayouts: Array<GPUBindGroupLayout>
}

external interface GPUObjectPipelineDescriptorBase : GPUObjectBase {
    var layout: GPUPipelineLayout
}

external interface GPURenderPipelineDescriptor : GPUObjectPipelineDescriptorBase {
    var vertex: GPUVertexState
    var primitive: GPUPrimitiveState
    var depthStencil: GPUDepthStencilState?
    var multisample: GPUMultisampleState
    var fragment: GPUFragmentState?
}

external interface GPUProgrammableStage {
    var module: GPUShaderModule
    var entryPoint: String
    var constants: Map<String, Number>
}

external interface GPUVertexState : GPUProgrammableStage {
    var buffers: Array<GPUVertexBufferLayout>
}

external interface GPUVertexBufferLayout {
    var arrayStride: Long
    var stepMode: String
    var attributes: Array<GPUVertexAttribute>
}

external interface GPUVertexAttribute {
    var format: String
    var offset: Long
    var shaderLocation: Int
}

external interface GPUPrimitiveState {
    var topology: String
    var stripIndexFormat: String?
    var frontFace: String
    var cullMode: String
    var unclippedDepth: Boolean
}

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

external interface GPUStencilFaceState {
    var compare: String
    var failOp: String
    var depthFailOp: String
    var passOp: String
}

external interface GPUMultisampleState {
    var count: Int
    var mask: Int
    var alphaToCoverageEnabled: Boolean
}

external interface GPUFragmentState : GPUProgrammableStage {
    var targets: Array<GPUColorTargetState>
}

external interface GPUColorTargetState {
    var format: String
    var blend: GPUBlendState?
    var writeMask: Int
}

external interface GPUBlendState {
    var color: GPUBlendComponent
    var alpha: GPUBlendComponent
}

external interface GPUBlendComponent {
    var operation: String
    var srcFactor: String
    var dstFactor: String
}

external interface GPUBufferDescriptor : GPUObjectBase {
    var size: Long
    var usage: Int
    var mappedAtCreation: Boolean
}

external interface GPUBindGroupLayoutDescriptor : GPUObjectBase {
    var entries: Array<GPUBindGroupLayoutEntry>
}

external interface GPUBindGroupLayoutEntry : GPUObjectBase {
    var binding: Int
    var visibility: Int
    var buffer: GPUBufferBindingLayout
    var sampler: GPUBufferSamplerBindingLayout
    var texture: GPUTextureBindingLayout
    var storageTexture: GPUStorageTextureBindingLayout
    var externalTexture: GPUExternalTextureBindingLayout
}

external interface GPUBufferBindingLayout : GPUObjectBase {
    var type: String
    var hasDynamicOffset: Boolean
    var minBindingSize: Long
}

external interface GPUBufferSamplerBindingLayout : GPUObjectBase {
    var type: String
}

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

external interface GPUBindGroupDescriptor : GPUObjectBase {
    var layout: GPUBindGroupLayout
    var entries: Array<GPUBindGroupEntry>
}

external interface GPUBindGroupEntry : GPUObjectBase {
    var binding: Int

    /** [GPUSampler] or [GPUTextureView] or [GPUBufferBinding] */
    var resource: GPUBindingResource
}

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

external interface GPUComputePipelineDescriptor : GPUObjectPipelineDescriptorBase {
    var compute: GPUProgrammableStage
}

external interface GPUTextureDescriptor : GPUObjectBase {
    var size: GPUExtent3D
    var mipLevelCount: Int
    var sampleCount: Int
    var dimension: String
    var format: String
    var usage: Int
    var viewFormats: Array<String>
}

external interface GPUExtent3D {
    var width: Int
    var height: Int
    var depthOrArrayLayer: Int
}

external interface GPUShaderModule : GPUObjectBase {
    val compilationInfo: Any
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

external interface GPUTextureViewDescriptor : GPUObjectBase {
    var arrayLayerCount: Int
    var aspect: String
    var baseArrayLayer: Int
    var baseMipLevel: Int
    var dimension: String
    var format: String
    var mipLevelCount: Int
}

external interface GPURenderPassDescriptor : GPUObjectBase {
    var colorAttachments: Array<GPURenderPassColorAttachment>
    var depthStencilAttachment: GPURenderPassDepthStencilAttachment?
    var occlusionQuerySet: GPUQuerySet
    var timestampWrites: GPURenderPassTimestampWrites
    var maxDrawCount: Long
}

external interface GPURenderPassColorAttachment {
    var view: GPUTextureView
    var depthSlice: Int
    var resolveTarget: GPUTextureView?
    var clearValue: Array<Float>?
    var loadOp: String
    var storeOp: String
}

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

external interface GPUImageCopyTexture {
    var aspect: String
    var mipLevel: Int
    var origin: GPUOrigin3D
    var texture: GPUTexture
}

external interface GPUOrigin3D {
    var x: Int
    var y: Int
    var z: Int
}

external interface GPUImageDataLayout {
    var offset: Long
    var bytesPerRow: Int
    var rowsPerImage: Int
}

external interface GPUImageCopyBuffer : GPUImageDataLayout {
    var buffer: GPUBufferJs
}
