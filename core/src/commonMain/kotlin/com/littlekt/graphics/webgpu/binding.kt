package com.littlekt.graphics.webgpu

import com.littlekt.Releasable

/** A resource that can be bound to a pipeline. */
expect interface IntoBindingResource

/**
 * An element of a [BindGroupDescriptor], consisting of bind-able resource and the slot to bind it
 * to.
 *
 * @param binding slot for which binding provides resource. Corresponds to an entry of the same
 *   binding index in the [BindGroupLayoutDescriptor].
 * @param resource resource to attach to the binding.
 */
data class BindGroupEntry(val binding: Int, val resource: IntoBindingResource)

/**
 * Describes a group of bindings and the resources to be bound. For use with
 * [Device.createBindGroup].
 *
 * @param layout the [BindGroupLayout] that corresponds to this bind group.
 * @param entries a list of resources to bind to this bind group.
 * @param label debug label of a [BindGroup].
 */
data class BindGroupDescriptor(
    val layout: BindGroupLayout,
    val entries: List<BindGroupEntry>,
    val label: String? = null
) {
    constructor(
        layout: BindGroupLayout,
        entry: BindGroupEntry,
        label: String? = null
    ) : this(layout, listOf(entry), label)
}

expect class BindGroup : Releasable {
    override fun release()
}

expect class BufferBinding(buffer: GPUBuffer, offset: Long = 0, size: Long = buffer.size - offset) :
    IntoBindingResource

expect abstract class BindingLayout()

expect class BufferBindingLayout(
    type: BufferBindingType = BufferBindingType.UNIFORM,
    hasDynamicOffset: Boolean = false,
    minBindingSize: Long = 0,
) : BindingLayout

expect class TextureBindingLayout(
    sampleType: TextureSampleType = TextureSampleType.FLOAT,
    viewDimension: TextureViewDimension = TextureViewDimension.D2,
    multisampled: Boolean = false,
) : BindingLayout


expect class SamplerBindingLayout(type: SamplerBindingType = SamplerBindingType.FILTERING) :
    BindingLayout

expect class BindGroupLayoutEntry(
    binding: Int,
    visibility: ShaderStage,
    bindingLayout: BindingLayout
)

data class BindGroupLayoutDescriptor(
    val entries: List<BindGroupLayoutEntry>,
    val label: String? = null
) {
    constructor(entry: BindGroupLayoutEntry) : this(listOf(entry))
}


expect class BindGroupLayout : Releasable {
    override fun release()
}

/**
 * A handle to a pipeline layout.
 *
 * A `PipelineLayout` describes the available binding groups of a pipeline. It can be created with
 * [Device.createPipelineLayout].
 */
expect class PipelineLayout : Releasable {
    override fun release()
}

expect class PipelineLayoutDescriptor(
    bindGroupLayouts: List<BindGroupLayout> = emptyList(),
    label: String? = null
) {
    constructor(bindGroupLayout: BindGroupLayout, label: String? = null)
}
