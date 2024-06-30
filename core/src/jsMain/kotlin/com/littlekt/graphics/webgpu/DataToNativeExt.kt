package com.littlekt.graphics.webgpu

import com.littlekt.util.internal.jsObject

fun GPUObjectBase(init: GPUObjectBase.() -> Unit = {}): GPUObjectBase =
    jsObject(init).unsafeCast<GPUObjectBase>()

fun GPURequestAdapterOptions(
    init: GPURequestAdapterOptions.() -> Unit = {}
): GPURequestAdapterOptions = jsObject(init).unsafeCast<GPURequestAdapterOptions>()

fun GPUCanvasConfiguration(init: GPUCanvasConfiguration.() -> Unit = {}): GPUCanvasConfiguration =
    jsObject(init).unsafeCast<GPUCanvasConfiguration>()

fun SurfaceConfiguration.toNative(): GPUCanvasConfiguration = GPUCanvasConfiguration {
    val it = this@toNative
    device = it.device.delegate
    format = it.format.nativeVal
    usage = it.usage.usageFlag
    alphaMode = it.alphaMode.nativeVal
}

fun GPUShaderModuleDescriptor(
    init: GPUShaderModuleDescriptor.() -> Unit = {}
): GPUShaderModuleDescriptor = jsObject(init).unsafeCast<GPUShaderModuleDescriptor>()

fun GPUPipelineLayoutDescriptor(
    init: GPUPipelineLayoutDescriptor.() -> Unit = {}
): GPUPipelineLayoutDescriptor = jsObject(init).unsafeCast<GPUPipelineLayoutDescriptor>()

fun PipelineLayoutDescriptor.toNative(): GPUPipelineLayoutDescriptor = GPUPipelineLayoutDescriptor {
    val it = this@toNative
    bindGroupLayouts = it.delegates
    label = it.label
}

fun GPURenderPipelineDescriptor(
    init: GPURenderPipelineDescriptor.() -> Unit = {}
): GPURenderPipelineDescriptor = jsObject(init).unsafeCast<GPURenderPipelineDescriptor>()

fun RenderPipelineDescriptor.toNative(): GPURenderPipelineDescriptor = GPURenderPipelineDescriptor {
    val it = this@toNative
    layout = it.layout.delegate
    vertex = it.vertex.toNative()
    primitive = it.primitive.toNative()
    depthStencil = it.depthStencil?.toNative() ?: undefined
    multisample = it.multisample.toNative()
    fragment = it.fragment?.toNative() ?: undefined
    label = it.label
}

fun GPUVertexState(init: GPUVertexState.() -> Unit = {}): GPUVertexState =
    jsObject(init).unsafeCast<GPUVertexState>()

fun VertexState.toNative(): GPUVertexState = GPUVertexState {
    val it = this@toNative
    module = it.module.delegate
    entryPoint = it.entryPoint
    buffers = it.buffers.map { it.toNative() }.toTypedArray()
}

fun GPUVertexBufferLayout(init: GPUVertexBufferLayout.() -> Unit = {}): GPUVertexBufferLayout =
    jsObject(init).unsafeCast<GPUVertexBufferLayout>()

fun WebGPUVertexBufferLayout.toNative(): GPUVertexBufferLayout = GPUVertexBufferLayout {
    val it = this@toNative
    arrayStride = it.arrayStride
    stepMode = it.stepMode.nativeVal
    attributes = it.attributes.map { it.toNative() }.toTypedArray()
}

fun GPUVertexAttribute(init: GPUVertexAttribute.() -> Unit = {}): GPUVertexAttribute =
    jsObject(init).unsafeCast<GPUVertexAttribute>()

fun WebGPUVertexAttribute.toNative(): GPUVertexAttribute = GPUVertexAttribute {
    val it = this@toNative
    format = it.format.nativeVal
    offset = it.offset
    shaderLocation = it.shaderLocation
}

fun GPUPrimitiveState(init: GPUPrimitiveState.() -> Unit = {}): GPUPrimitiveState =
    jsObject(init).unsafeCast<GPUPrimitiveState>()

fun PrimitiveState.toNative(): GPUPrimitiveState = GPUPrimitiveState {
    val it = this@toNative
    topology = it.topology.nativeVal
    stripIndexFormat = it.stripIndexFormat?.nativeVal ?: undefined
    frontFace = it.frontFace.nativeVal
    cullMode = it.cullMode.nativeVal
}

fun GPUDepthStencilState(init: GPUDepthStencilState.() -> Unit = {}): GPUDepthStencilState =
    jsObject(init).unsafeCast<GPUDepthStencilState>()

fun DepthStencilState.toNative(): GPUDepthStencilState = GPUDepthStencilState {
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

fun GPUStencilFaceState(init: GPUStencilFaceState.() -> Unit = {}): GPUStencilFaceState =
    jsObject(init).unsafeCast<GPUStencilFaceState>()

fun StencilFaceState.toNative(): GPUStencilFaceState = GPUStencilFaceState {
    val it = this@toNative
    compare = it.compare.nativeVal
    failOp = it.failOp.nativeVal
    depthFailOp = it.depthFailOp.nativeVal
    passOp = it.passOp.nativeVal
}

fun GPUMultisampleState(init: GPUMultisampleState.() -> Unit = {}): GPUMultisampleState =
    jsObject(init).unsafeCast<GPUMultisampleState>()

fun MultisampleState.toNative(): GPUMultisampleState = GPUMultisampleState {
    val it = this@toNative
    count = it.count
    mask = it.mask
    alphaToCoverageEnabled = it.alphaToCoverageEnabled
}

fun GPUFragmentState(init: GPUFragmentState.() -> Unit = {}): GPUFragmentState =
    jsObject(init).unsafeCast<GPUFragmentState>()

fun FragmentState.toNative(): GPUFragmentState = GPUFragmentState {
    val it = this@toNative
    module = it.module.delegate
    entryPoint = it.entryPoint
    targets = it.targets.map { it.toNative() }.toTypedArray()
}

fun GPUColorTargetState(init: GPUColorTargetState.() -> Unit = {}): GPUColorTargetState =
    jsObject(init).unsafeCast<GPUColorTargetState>()

fun ColorTargetState.toNative(): GPUColorTargetState = GPUColorTargetState {
    val it = this@toNative
    format = it.format.nativeVal
    blend = it.blendState?.toNative() ?: undefined
    writeMask = it.writeMask.usageFlag
}

fun GPUBlendState(init: GPUBlendState.() -> Unit = {}): GPUBlendState =
    jsObject(init).unsafeCast<GPUBlendState>()

fun BlendState.toNative(): GPUBlendState = GPUBlendState {
    val it = this@toNative
    color = it.color.toNative()
    alpha = it.alpha.toNative()
}

fun GPUBlendComponent(init: GPUBlendComponent.() -> Unit = {}): GPUBlendComponent =
    jsObject(init).unsafeCast<GPUBlendComponent>()

fun BlendComponent.toNative(): GPUBlendComponent = GPUBlendComponent {
    val it = this@toNative
    operation = it.operation.nativeVal
    srcFactor = it.srcFactor.nativeVal
    dstFactor = it.dstFactor.nativeVal
}

fun GPUComputePipelineDescriptor(
    init: GPUComputePipelineDescriptor.() -> Unit = {}
): GPUComputePipelineDescriptor = jsObject(init).unsafeCast<GPUComputePipelineDescriptor>()

fun ComputePipelineDescriptor.toNative(): GPUComputePipelineDescriptor =
    GPUComputePipelineDescriptor {
        val it = this@toNative
        layout = it.layout.delegate
        compute = it.compute.toNative()
        label = it.label
    }

fun GPUProgrammableStage(init: GPUProgrammableStage.() -> Unit = {}): GPUProgrammableStage =
    jsObject(init).unsafeCast<GPUProgrammableStage>()

fun ProgrammableStage.toNative(): GPUProgrammableStage = GPUProgrammableStage {
    val it = this@toNative
    module = it.module.delegate
    entryPoint = it.entryPoint
    // TODO constants?
}

fun GPUBufferDescriptor(init: GPUBufferDescriptor.() -> Unit = {}): GPUBufferDescriptor =
    jsObject(init).unsafeCast<GPUBufferDescriptor>()

fun BufferDescriptor.toNative(): GPUBufferDescriptor = GPUBufferDescriptor {
    val it = this@toNative
    size = it.size
    usage = it.usage.usageFlag
    mappedAtCreation = it.mappedAtCreation
    label = it.label
}

fun GPUBindGroupLayoutDescriptor(
    init: GPUBindGroupLayoutDescriptor.() -> Unit = {}
): GPUBindGroupLayoutDescriptor = jsObject(init).unsafeCast<GPUBindGroupLayoutDescriptor>()

fun BindGroupLayoutDescriptor.toNative(): GPUBindGroupLayoutDescriptor =
    GPUBindGroupLayoutDescriptor {
        val it = this@toNative
        entries = it.entries.map { it.toNative() }.toTypedArray()
        label = it.label
    }

fun GPUBindGroupLayoutEntry(
    init: GPUBindGroupLayoutEntry.() -> Unit = {}
): GPUBindGroupLayoutEntry = jsObject(init).unsafeCast<GPUBindGroupLayoutEntry>()

fun BindGroupLayoutEntry.toNative(): GPUBindGroupLayoutEntry = GPUBindGroupLayoutEntry {
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

fun GPUBufferBindingLayout(init: GPUBufferBindingLayout.() -> Unit = {}): GPUBufferBindingLayout =
    jsObject(init).unsafeCast<GPUBufferBindingLayout>()

fun BufferBindingLayout.toNative(): GPUBufferBindingLayout = GPUBufferBindingLayout {
    val it = this@toNative
    type = it.type.nativeVal
    hasDynamicOffset = it.hasDynamicOffset
    minBindingSize = it.minBindingSize
}

fun GPUBufferSamplerBindingLayout(
    init: GPUBufferSamplerBindingLayout.() -> Unit = {}
): GPUBufferSamplerBindingLayout = jsObject(init).unsafeCast<GPUBufferSamplerBindingLayout>()

fun SamplerBindingLayout.toNative(): GPUBufferSamplerBindingLayout = GPUBufferSamplerBindingLayout {
    val it = this@toNative
    type = it.type.nativeVal
}

fun GPUTextureBindingLayout(
    init: GPUTextureBindingLayout.() -> Unit = {}
): GPUTextureBindingLayout = jsObject(init).unsafeCast<GPUTextureBindingLayout>()

fun TextureBindingLayout.toNative(): GPUTextureBindingLayout = GPUTextureBindingLayout {
    val it = this@toNative
    sampleType = it.sampleType.nativeVal
    viewDimension = it.viewDimension.nativeVal
    multisampled = it.multisampled
}

fun GPUBindGroupDescriptor(init: GPUBindGroupDescriptor.() -> Unit = {}): GPUBindGroupDescriptor =
    jsObject(init).unsafeCast<GPUBindGroupDescriptor>()

fun BindGroupDescriptor.toNative(): GPUBindGroupDescriptor = GPUBindGroupDescriptor {
    val it = this@toNative
    layout = it.layout.delegate
    entries = it.entries.map { it.toNative() }.toTypedArray()
    label = it.label
}

fun GPUBindGroupEntry(init: GPUBindGroupEntry.() -> Unit = {}): GPUBindGroupEntry =
    jsObject(init).unsafeCast<GPUBindGroupEntry>()

fun BindGroupEntry.toNative(): GPUBindGroupEntry = GPUBindGroupEntry {
    val it = this@toNative
    binding = it.binding
    resource = it.resource.toNative()
}

fun GPUSamplerDescriptor(init: GPUSamplerDescriptor.() -> Unit = {}): GPUSamplerDescriptor =
    jsObject(init).unsafeCast<GPUSamplerDescriptor>()

fun SamplerDescriptor.toNative(): GPUSamplerDescriptor = GPUSamplerDescriptor {
    val it = this@toNative
    addressModeU = it.addressModeU.nativeVal
    addressModeV = it.addressModeV.nativeVal
    addressModeW = it.addressModeW.nativeVal
    magFilter = it.magFilter.nativeVal
    minFilter = it.minFilter.nativeVal
    mipmapFilter = it.mipmapFilter.nativeVal
    lodMinClamp = it.lodMinClamp
    lodMaxClamp = it.lodMaxClamp
    compare = it.compare?.nativeVal ?: undefined
    maxAnisotropy = it.maxAnisotropy
    label = it.label
}

fun GPUTextureDescriptor(init: GPUTextureDescriptor.() -> Unit = {}): GPUTextureDescriptor =
    jsObject(init).unsafeCast<GPUTextureDescriptor>()

fun TextureDescriptor.toNative(): GPUTextureDescriptor = GPUTextureDescriptor {
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

fun GPUExtent3D(init: GPUExtent3D.() -> Unit = {}): GPUExtent3D =
    jsObject(init).unsafeCast<GPUExtent3D>()

fun Extent3D.toNative(): GPUExtent3D = GPUExtent3D {
    val it = this@toNative
    width = it.width
    height = it.height
    depthOrArrayLayer = it.depth
}

fun GPUImageDataLayout(init: GPUImageDataLayout.() -> Unit = {}): GPUImageDataLayout =
    jsObject(init).unsafeCast<GPUImageDataLayout>()

fun TextureDataLayout.toNative(): GPUImageDataLayout = GPUImageDataLayout {
    val it = this@toNative
    offset = it.offset
    bytesPerRow = it.bytesPerRow
    rowsPerImage = it.rowsPerImage
}

fun GPUTextureViewDescriptor(
    init: GPUTextureViewDescriptor.() -> Unit = {}
): GPUTextureViewDescriptor = jsObject(init).unsafeCast<GPUTextureViewDescriptor>()

fun TextureViewDescriptor.toNative(): GPUTextureViewDescriptor = GPUTextureViewDescriptor {
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

fun GPURenderPassDescriptor(
    init: GPURenderPassDescriptor.() -> Unit = {}
): GPURenderPassDescriptor = jsObject(init).unsafeCast<GPURenderPassDescriptor>()

fun RenderPassDescriptor.toNative(): GPURenderPassDescriptor = GPURenderPassDescriptor {
    val it = this@toNative
    colorAttachments = it.colorAttachments.map { it.toNative() }.toTypedArray()
    depthStencilAttachment = it.depthStencilAttachment?.toNative() ?: undefined
    label = it.label
}

fun GPURenderPassColorAttachment(
    init: GPURenderPassColorAttachment.() -> Unit = {}
): GPURenderPassColorAttachment = jsObject(init).unsafeCast<GPURenderPassColorAttachment>()

fun RenderPassColorAttachmentDescriptor.toNative(): GPURenderPassColorAttachment =
    GPURenderPassColorAttachment {
        val it = this@toNative
        view = it.view.delegate
        resolveTarget = it.resolveTarget?.delegate ?: undefined
        clearValue = it.clearColor?.fields?.toTypedArray() ?: undefined
        loadOp = it.loadOp.nativeVal
        storeOp = it.storeOp.nativeVal
    }

fun GPURenderPassDepthStencilAttachment(
    init: GPURenderPassDepthStencilAttachment.() -> Unit = {}
): GPURenderPassDepthStencilAttachment =
    jsObject(init).unsafeCast<GPURenderPassDepthStencilAttachment>()

fun RenderPassDepthStencilAttachmentDescriptor.toNative(): GPURenderPassDepthStencilAttachment =
    GPURenderPassDepthStencilAttachment {
        val it = this@toNative
        view = it.view.delegate
        depthClearValue = it.depthClearValue
        depthLoadOp = it.depthLoadOp?.nativeVal ?: undefined
        depthStoreOp = it.depthStoreOp?.nativeVal ?: undefined
        depthReadOnly = it.depthReadOnly
        stencilClearValue = it.stencilClearValue
        stencilLoadOp = it.stencilLoadOp?.nativeVal ?: undefined
        stencilStoreOp = it.stencilStoreOp?.nativeVal ?: undefined
        stencilReadOnly = it.stencilReadOnly
    }

fun GPUImageCopyTexture(init: GPUImageCopyTexture.() -> Unit = {}): GPUImageCopyTexture =
    jsObject(init).unsafeCast<GPUImageCopyTexture>()

fun TextureCopyView.toNative(): GPUImageCopyTexture = GPUImageCopyTexture {
    val it = this@toNative
    mipLevel = it.mipLevel
    origin = it.origin.toNative()
    texture = it.texture.delegate
}

fun GPUOrigin3D(init: GPUOrigin3D.() -> Unit = {}): GPUOrigin3D =
    jsObject(init).unsafeCast<GPUOrigin3D>()

fun Origin3D.toNative(): GPUOrigin3D = GPUOrigin3D {
    val it = this@toNative
    x = it.x
    y = it.y
    z = it.z
}

fun GPUImageCopyBuffer(init: GPUImageCopyBuffer.() -> Unit = {}): GPUImageCopyBuffer =
    jsObject(init).unsafeCast<GPUImageCopyBuffer>()

fun BufferCopyView.toNative(): GPUImageCopyBuffer = GPUImageCopyBuffer {
    val it = this@toNative
    offset = it.layout.offset
    bytesPerRow = it.layout.bytesPerRow
    rowsPerImage = it.layout.rowsPerImage
    buffer = it.buffer.delegate
}
