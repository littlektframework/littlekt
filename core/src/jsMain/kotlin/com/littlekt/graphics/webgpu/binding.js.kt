package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.util.internal.jsObject

actual interface IntoBindingResource {
    fun toNative(): GPUBindingResource
}

actual class BindGroup(val delegate: GPUBindGroup) : Releasable {
    actual override fun release() {}
}

actual class BufferBinding
actual constructor(val buffer: GPUBuffer, val offset: Long, val size: Long) : IntoBindingResource {
    private val delegate =
        jsObject().unsafeCast<GPUBufferBinding>().apply {
            buffer = this@BufferBinding.buffer.delegate
            offset = this@BufferBinding.offset
            size = this@BufferBinding.size
        }

    override fun toNative(): GPUBindingResource {
        return delegate
    }
}

actual abstract class BindingLayout actual constructor()

actual class BufferBindingLayout
actual constructor(
    val type: BufferBindingType,
    val hasDynamicOffset: Boolean,
    val minBindingSize: Long,
) : BindingLayout()

actual class TextureBindingLayout
actual constructor(
    val sampleType: TextureSampleType,
    val viewDimension: TextureViewDimension,
    val multisampled: Boolean,
) : BindingLayout()

actual class SamplerBindingLayout actual constructor(val type: SamplerBindingType) :
    BindingLayout()

actual class BindGroupLayout(val delegate: GPUBindGroupLayout) : Releasable {
    actual override fun release() {}
}

actual class PipelineLayout(val delegate: GPUPipelineLayout) : Releasable {
    actual override fun release() {}
}

actual class PipelineLayoutDescriptor
actual constructor(bindGroupLayouts: List<BindGroupLayout>, val label: String?) {
    actual constructor(
        bindGroupLayout: BindGroupLayout,
        label: String?,
    ) : this(listOf(bindGroupLayout), label)

    val delegates = bindGroupLayouts.map { it.delegate }.toTypedArray()
}
