package com.littlekt.graphics.webgpu

import com.littlekt.Releasable

actual interface IntoBindingResource

actual class BindGroup : Releasable {
    actual override fun release() {}
}

actual class BufferBinding actual constructor(buffer: GPUBuffer, offset: Long, size: Long) :
    IntoBindingResource

actual abstract class BindingLayout actual constructor()

actual class BufferBindingLayout
actual constructor(type: BufferBindingType, hasDynamicOffset: Boolean, minBindingSize: Long) :
    BindingLayout()

actual class TextureBindingLayout
actual constructor(
    sampleType: TextureSampleType,
    viewDimension: TextureViewDimension,
    multisampled: Boolean
) : BindingLayout()

actual class SamplerBindingLayout actual constructor(type: SamplerBindingType) : BindingLayout()

actual class BindGroupLayoutEntry
actual constructor(binding: Int, visibility: ShaderStage, bindingLayout: BindingLayout)

actual class BindGroupLayout : Releasable {
    actual override fun release() {}
}

actual class PipelineLayout : Releasable {
    actual override fun release() {}
}

actual class PipelineLayoutDescriptor actual constructor(bindGroupLayouts: List<BindGroupLayout>) {
    actual constructor(bindGroupLayout: BindGroupLayout) : this(listOf(bindGroupLayout))
}
