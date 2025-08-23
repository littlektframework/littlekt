package com.littlekt.graphics.webgpu

import kotlin.js.toJsArray
import kotlin.js.toJsString

fun DeviceDescriptor.toNative(): GPUDeviceDescriptor {
    val gpuDeviceDescriptor = GPUDeviceDescriptor()
    gpuDeviceDescriptor.requiredFeatures = requiredFeatures.map { it.nativeVal.toJsString() }.toJsArray()
    gpuDeviceDescriptor.requiredLimits = JsRequiredLimits().apply {
        requiredLimits?.maxTextureDimension1D?.let { maxTextureDimension1D = it.toDouble() }
        requiredLimits?.maxTextureDimension2D?.let { maxTextureDimension2D = it.toDouble() }
        requiredLimits?.maxTextureDimension3D?.let { maxTextureDimension3D = it.toDouble() }
        requiredLimits?.maxTextureArrayLayers?.let { maxTextureArrayLayers = it.toDouble() }
        requiredLimits?.maxBindGroups?.let { maxBindGroups = it.toDouble() }
        requiredLimits?.maxBindGroupsPlusVertexBuffers?.let {
            maxBindGroupsPlusVertexBuffers = it.toDouble()
        }
        requiredLimits?.maxBindingsPerBindGroup?.let { maxBindingsPerBindGroup = it.toDouble() }
        requiredLimits?.maxDynamicUniformBuffersPerPipelineLayout?.let {
            maxDynamicUniformBuffersPerPipelineLayout = it.toDouble()
        }
        requiredLimits?.maxDynamicStorageBuffersPerPipelineLayout?.let {
            maxDynamicStorageBuffersPerPipelineLayout = it.toDouble()
        }
        requiredLimits?.maxSampledTexturesPerShaderStage?.let {
            maxSampledTexturesPerShaderStage = it.toDouble()
        }
        requiredLimits?.maxSamplersPerShaderStage?.let {
            maxSamplersPerShaderStage = it.toDouble()
        }
        requiredLimits?.maxStorageBuffersPerShaderStage?.let {
            maxStorageBuffersPerShaderStage = it.toDouble()
        }
        requiredLimits?.maxStorageTexturesPerShaderStage?.let {
            maxStorageTexturesPerShaderStage = it.toDouble()
        }
        requiredLimits?.maxUniformBuffersPerShaderStage?.let {
            maxUniformBuffersPerShaderStage = it.toDouble()
        }
        requiredLimits?.maxUniformBufferBindingSize?.let { maxUniformBufferBindingSize = it.toDouble() }
        requiredLimits?.maxStorageBufferBindingSize?.let { maxStorageBufferBindingSize = it.toDouble() }
        requiredLimits?.minUniformBufferOffsetAlignment?.let {
            minUniformBufferOffsetAlignment = it.toDouble()
        }
        requiredLimits?.minStorageBufferOffsetAlignment?.let {
            minStorageBufferOffsetAlignment = it.toDouble()
        }
        requiredLimits?.maxVertexBuffers?.let { maxVertexBuffers = it.toDouble() }
        requiredLimits?.maxBufferSize?.let { maxBufferSize = it.toDouble() }
        requiredLimits?.maxVertexAttributes?.let { maxVertexAttributes = it.toDouble() }
        requiredLimits?.maxVertexBufferArrayStride?.let {
            maxVertexBufferArrayStride = it.toDouble()
        }
    }
    return gpuDeviceDescriptor
}

fun SurfaceConfiguration.toNative(): GPUCanvasConfiguration = GPUCanvasConfiguration().apply {
    val it = this@toNative
    device = it.device.delegate
    format = it.format.nativeVal
    usage = it.usage.usageFlag
    alphaMode = it.alphaMode.nativeVal
}

fun PipelineLayoutDescriptor.toNative(): GPUPipelineLayoutDescriptor = GPUPipelineLayoutDescriptor().apply {
    val it = this@toNative
    bindGroupLayouts = it.delegates
    label = it.label
}

fun RenderPipelineDescriptor.toNative(): GPURenderPipelineDescriptor = GPURenderPipelineDescriptor().apply {
    val it = this@toNative
    layout = it.layout.delegate
    vertex = it.vertex.toNative()
    primitive = it.primitive.toNative()
    it.depthStencil?.toNative()?.let {
        depthStencil = it
    }
    multisample = it.multisample.toNative()
    it.fragment?.toNative()?.let {
        fragment = it
    }
    label = it.label
}
fun VertexState.toNative(): GPUVertexState = GPUVertexState().apply {
    val it = this@toNative
    module = it.module.delegate
    entryPoint = it.entryPoint
    buffers = it.buffers.map { it.toNative() }.toJsArray()
}

fun WebGPUVertexBufferLayout.toNative(): GPUVertexBufferLayout = GPUVertexBufferLayout().apply {
    val it = this@toNative
    arrayStride = it.arrayStride.toDouble()
    stepMode = it.stepMode.nativeVal
    attributes = it.attributes.map { it.toNative() }.toJsArray()
}
fun WebGPUVertexAttribute.toNative(): GPUVertexAttribute = GPUVertexAttribute().apply {
    val it = this@toNative
    format = it.format.nativeVal
    offset = it.offset.toDouble()
    shaderLocation = it.shaderLocation
}

fun PrimitiveState.toNative(): GPUPrimitiveState = GPUPrimitiveState().apply {
    val it = this@toNative
    topology = it.topology.nativeVal
    it.stripIndexFormat?.nativeVal?.let {
        stripIndexFormat = it
    }
    frontFace = it.frontFace.nativeVal
    cullMode = it.cullMode.nativeVal
}
fun DepthStencilState.toNative(): GPUDepthStencilState = GPUDepthStencilState().apply {
    val it = this@toNative
    format = it.format.nativeVal
    depthWriteEnabled = it.depthWriteEnabled
    depthCompare = it.depthCompare.nativeVal
    stencilFront = it.stencil.front.toNative()
    stencilBack = it.stencil.back.toNative()
    stencilReadMask = it.stencil.readMask
    stencilWriteMask = it.stencil.writeMask
    depthBias = it.bias.constant
    depthBiasSlopeScale = it.bias.slopeScale
    depthBiasClamp = it.bias.clamp
}
fun StencilFaceState.toNative(): GPUStencilFaceState = GPUStencilFaceState().apply {
    val it = this@toNative
    compare = it.compare.nativeVal
    failOp = it.failOp.nativeVal
    depthFailOp = it.depthFailOp.nativeVal
    passOp = it.passOp.nativeVal
}

fun MultisampleState.toNative(): GPUMultisampleState = GPUMultisampleState().apply {
    val it = this@toNative
    count = it.count
    mask = it.mask
    alphaToCoverageEnabled = it.alphaToCoverageEnabled
}

fun FragmentState.toNative(): GPUFragmentState = GPUFragmentState().apply {
    val it = this@toNative
    module = it.module.delegate
    entryPoint = it.entryPoint
    targets = it.targets.map { it.toNative() }.toJsArray()
}

fun ColorTargetState.toNative(): GPUColorTargetState = GPUColorTargetState().apply {
    val it = this@toNative
    format = it.format.nativeVal
    it.blendState?.toNative()?.let {
        blend = it
    }
    writeMask = it.writeMask.usageFlag
}

fun BlendState.toNative(): GPUBlendState = GPUBlendState().apply {
    val it = this@toNative
    color = it.color.toNative()
    alpha = it.alpha.toNative()
}
fun BlendComponent.toNative(): GPUBlendComponent = GPUBlendComponent().apply {
    val it = this@toNative
    operation = it.operation.nativeVal
    srcFactor = it.srcFactor.nativeVal
    dstFactor = it.dstFactor.nativeVal
}

fun ComputePipelineDescriptor.toNative(): GPUComputePipelineDescriptor =
    GPUComputePipelineDescriptor().apply {
        val it = this@toNative
        layout = it.layout.delegate
        compute = it.compute.toNative()
        label = it.label
    }
fun ProgrammableStage.toNative(): GPUProgrammableStage = GPUProgrammableStage().apply {
    val it = this@toNative
    module = it.module.delegate
    entryPoint = it.entryPoint
    // TODO constants?
}

fun BufferDescriptor.toNative(): GPUBufferDescriptor = GPUBufferDescriptor().apply {
    val it = this@toNative
    size = it.size.toDouble()
    usage = it.usage.usageFlag
    mappedAtCreation = it.mappedAtCreation
    label = it.label
}


fun BindGroupLayoutDescriptor.toNative(): GPUBindGroupLayoutDescriptor =
    GPUBindGroupLayoutDescriptor().apply {
        val it = this@toNative
        entries = it.entries.map { it.toNative() }.toJsArray()
        label = it.label
    }

fun BindGroupLayoutEntry.toNative(): GPUBindGroupLayoutEntry = GPUBindGroupLayoutEntry().apply {
    val it = this@toNative
    binding = it.binding
    visibility = it.visibility.usageFlag
    when (val layout = it.bindingLayout) {
        is BufferBindingLayout -> buffer = layout.toNative()
        is SamplerBindingLayout -> sampler = layout.toNative()
        is TextureBindingLayout -> texture = layout.toNative()
        else -> {
            TODO("Implement storageTexture and externalTexture binding layouts!")
        }
    }
}

fun BufferBindingLayout.toNative(): GPUBufferBindingLayout = GPUBufferBindingLayout().apply {
    val it = this@toNative
    type = it.type.nativeVal
    hasDynamicOffset = it.hasDynamicOffset
    minBindingSize = it.minBindingSize.toDouble()
}

fun SamplerBindingLayout.toNative(): GPUBufferSamplerBindingLayout = GPUBufferSamplerBindingLayout().apply {
    val it = this@toNative
    type = it.type.nativeVal
}

fun TextureBindingLayout.toNative(): GPUTextureBindingLayout = GPUTextureBindingLayout().apply {
    val it = this@toNative
    sampleType = it.sampleType.nativeVal
    viewDimension = it.viewDimension.nativeVal
    multisampled = it.multisampled
}

fun BindGroupDescriptor.toNative(): GPUBindGroupDescriptor = GPUBindGroupDescriptor().apply {
    val it = this@toNative
    layout = it.layout.delegate
    entries = it.entries.map { it.toNative() }.toJsArray()
    label = it.label
}

fun BindGroupEntry.toNative(): GPUBindGroupEntry = GPUBindGroupEntry().apply {
    val it = this@toNative
    binding = it.binding
    resource = it.resource.toNative()
}

fun SamplerDescriptor.toNative(): GPUSamplerDescriptor = GPUSamplerDescriptor().apply {
    val it = this@toNative
    addressModeU = it.addressModeU.nativeVal
    addressModeV = it.addressModeV.nativeVal
    addressModeW = it.addressModeW.nativeVal
    magFilter = it.magFilter.nativeVal
    minFilter = it.minFilter.nativeVal
    mipmapFilter = it.mipmapFilter.nativeVal
    lodMinClamp = it.lodMinClamp
    lodMaxClamp = it.lodMaxClamp
    it.compare?.nativeVal?.let {
        compare = it
    }
    maxAnisotropy = it.maxAnisotropy
    label = it.label
}

fun TextureDescriptor.toNative(): GPUTextureDescriptor = GPUTextureDescriptor().apply {
    val it = this@toNative
    size = it.size.toNative()
    mipLevelCount = it.mipLevelCount
    sampleCount = it.sampleCount
    dimension = it.dimension.nativeVal
    format = it.format.nativeVal
    usage = it.usage.usageFlag
    label = it.label
    // TODO viewFormats!
}

fun Extent3D.toNative(): GPUExtent3D = GPUExtent3D().apply {
    val it = this@toNative
    width = it.width
    height = it.height
    depthOrArrayLayer = it.depth
}

fun TextureDataLayout.toNative(): GPUImageDataLayout = GPUImageDataLayout().apply {
    val it = this@toNative
    offset = it.offset.toDouble()
    bytesPerRow = it.bytesPerRow
    rowsPerImage = it.rowsPerImage
}

fun TextureViewDescriptor.toNative(): GPUTextureViewDescriptor = GPUTextureViewDescriptor().apply {
    val it = this@toNative
    arrayLayerCount = it.arrayLayerCount
    aspect = it.aspect.nativeVal
    baseArrayLayer = it.baseArrayLayer
    baseMipLevel = it.baseMipLevel
    dimension = it.dimension.nativeVal
    format = it.format.nativeVal
    mipLevelCount = it.mipLevelCount
    label = it.label
}

fun RenderPassDescriptor.toNative(): GPURenderPassDescriptor = GPURenderPassDescriptor().apply {
    val it = this@toNative
    colorAttachments = it.colorAttachments.map { it.toNative() }.toJsArray()
    it.depthStencilAttachment?.toNative()?.let {
        depthStencilAttachment = it
    }
    label = it.label
}

expect fun GPUColorDict(): GPUColorDict
external interface GPUColorDict {
    var r: Double
    var g: Double
    var b: Double
    var a: Double
}
expect fun rgbaToColorDict(values: FloatArray): GPUColorDict

fun RenderPassColorAttachmentDescriptor.toNative(): GPURenderPassColorAttachment =
    GPURenderPassColorAttachment().apply {
        val it = this@toNative
        view = it.view.delegate
        it.resolveTarget?.delegate?.let {
            resolveTarget = it
        }
        it.clearColor?.fields?.let {
            clearValue = rgbaToColorDict(it)
        }
        loadOp = it.loadOp.nativeVal
        storeOp = it.storeOp.nativeVal
    }
fun RenderPassDepthStencilAttachmentDescriptor.toNative(): GPURenderPassDepthStencilAttachment =
    GPURenderPassDepthStencilAttachment().apply {
        val it = this@toNative
        view = it.view.delegate
        depthClearValue = it.depthClearValue
        it.depthLoadOp?.nativeVal?.let {
            depthLoadOp = it
        }
        it.depthStoreOp?.nativeVal?.let {
            depthStoreOp = it
        }
        depthReadOnly = it.depthReadOnly
        stencilClearValue = it.stencilClearValue
        it.stencilLoadOp?.nativeVal?.let {
            stencilLoadOp = it
        }
        it.stencilStoreOp?.nativeVal?.let {
            stencilStoreOp = it
        }
        stencilReadOnly = it.stencilReadOnly
    }

fun TextureCopyView.toNative(): GPUImageCopyTexture = GPUImageCopyTexture().apply {
    val it = this@toNative
    mipLevel = it.mipLevel
    origin = it.origin.toNative()
    texture = it.texture.delegate
}

fun Origin3D.toNative(): GPUOrigin3D = GPUOrigin3D().apply {
    val it = this@toNative
    x = it.x
    y = it.y
    z = it.z
}

fun BufferCopyView.toNative(): GPUImageCopyBuffer = GPUImageCopyBuffer().apply {
    val it = this@toNative
    offset = it.layout.offset.toDouble()
    bytesPerRow = it.layout.bytesPerRow
    rowsPerImage = it.layout.rowsPerImage
    buffer = it.buffer.delegate
}