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

/**
 * Handle to a binding group.
 *
 * A `BindGroup` represents the set of resources bound to the bindings described by a
 * [BindGroupLayout]. It can be created with [Device.createBindGroup]. A `BindGroup` can be bound to
 * a particular [RenderPassEncoder] with [RenderPassEncoder.setBindGroup], or to a
 * [ComputePassEncoder] with [ComputePassEncoder.setBindGroup].
 */
expect class BindGroup : Releasable {
    override fun release()
}

/**
 * Describes the segment of a buffer to bind.
 *
 * @param buffer the buffer to bind
 * @param offset base offset of the buffer, in bytes.
 * @param size size of the binding in bytes
 */
expect class BufferBinding(buffer: GPUBuffer, offset: Long = 0, size: Long = buffer.size - offset) :
    IntoBindingResource

/** Describes a generic binding layout. */
expect abstract class BindingLayout()

/**
 * Describes the layout of a [BufferBinding].
 *
 * @param type sub-type of the buffer binding.
 * @param hasDynamicOffset indicates that the binding has a dynamic offset. One offset must be
 *   passed to [RenderPassEncoder.setBindGroup] for each dynamic binding in increasing order of
 *   binding number.
 * @param minBindingSize the minimum size for a [BufferBinding] matching this entry, in bytes.
 */
expect class BufferBindingLayout(
    type: BufferBindingType = BufferBindingType.UNIFORM,
    hasDynamicOffset: Boolean = false,
    minBindingSize: Long = 0,
) : BindingLayout

/**
 * Describes the layout of binding a texture.
 *
 * ```wgsl
 * @group(0) @binding(0)
 * var t: texture_2d<f32>;
 * ```
 *
 * @param sampleType sample type of the texture binding.
 * @param viewDimension dimension of the texture view that is going to be sampled.
 * @param multisampled `true` if the texture has a sample count greater than 1. If this is `true`,
 *   the texture must be read from shaders with `texture1DMS`, `texture2DMS`, or `texture3DMS`,
 *   depending on dimension.
 */
expect class TextureBindingLayout(
    sampleType: TextureSampleType = TextureSampleType.FLOAT,
    viewDimension: TextureViewDimension = TextureViewDimension.D2,
    multisampled: Boolean = false,
) : BindingLayout

/**
 * A sampler binding layout that can be used to sample a texture.
 *
 * ```wgsl
 * @group(0) @binding(0)
 * var s: sampler;
 * ```
 *
 * @param type the sampler type of the sampler binding.
 */
expect class SamplerBindingLayout(type: SamplerBindingType = SamplerBindingType.FILTERING) :
    BindingLayout

/**
 * Describes a single binding inside a bind group.
 *
 * @param binding binding index. Must match shader index and be unique inside a [BindGroupLayout]. A
 *   binding of index 1, would be described as layout(set = 0, binding = 1) uniform in shaders.
 * @param visibility which shader stages can see this binding.
 * @param bindingLayout the binding layout to be used in the [BindGroupLayout].
 */
expect class BindGroupLayoutEntry(
    binding: Int,
    visibility: ShaderStage,
    bindingLayout: BindingLayout
)

/**
 * Describes a [BindGroupLayout]. For use with [Device.createBindGroupLayout].
 *
 * @param entries a list of entries in this [BindGroupLayout].
 * @param label debug label of a [BindGroupLayout].
 */
data class BindGroupLayoutDescriptor(
    val entries: List<BindGroupLayoutEntry>,
    val label: String? = null
) {
    constructor(entry: BindGroupLayoutEntry) : this(listOf(entry))
}

/**
 * Handle to a binding group layout.
 *
 * A `BindGroupLayout` is a handle to the GPU-side layout of a binding group. It can be used to
 * create a [BindGroupDescriptor] object, which in turn can be used to create a [BindGroup] object
 * with [Device.createBindGroup]. A series of `BindGroupLayout`s can also be sued to create a
 * [PipelineLayoutDescriptor], which can be used to create a [PipelineLayout].
 *
 * It can be created with [Device.createBindGroupLayout].
 */
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

/**
 * Describes a [PipelineLayout].
 *
 * For use with [Device.createPipelineLayout].
 *
 * @param bindGroupLayouts bind groups that this pipeline uses. The first entry will provide all the
 *   bindings for "set = 0", second entry will provide all the binding for "set = 1" etc.
 * @param label debug label of a [PipelineLayout].
 */
expect class PipelineLayoutDescriptor(
    bindGroupLayouts: List<BindGroupLayout> = emptyList(),
    label: String? = null
) {
    constructor(bindGroupLayout: BindGroupLayout, label: String? = null)
}
