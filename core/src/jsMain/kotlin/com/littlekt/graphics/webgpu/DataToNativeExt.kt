package com.littlekt.graphics.webgpu

import com.littlekt.util.internal.jsObject

fun GPUObjectBase(init: GPUObjectBase.() -> Unit = {}): GPUObjectBase =
    jsObject(init).unsafeCast<GPUObjectBase>()

fun GPURequestAdapterOptions(
    init: GPURequestAdapterOptions.() -> Unit = {}
): GPURequestAdapterOptions = jsObject(init).unsafeCast<GPURequestAdapterOptions>()


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
