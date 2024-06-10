package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.wgpu.WGPU.*
import com.littlekt.wgpu.WGPUBindGroupEntry
import com.littlekt.wgpu.WGPUBufferBindingLayout
import com.littlekt.wgpu.WGPUSamplerBindingLayout
import com.littlekt.wgpu.WGPUTextureBindingLayout
import java.lang.foreign.MemorySegment

actual interface IntoBindingResource {
    fun intoBindingResource(entry: MemorySegment)
}

actual class BindGroup(val segment: MemorySegment) : Releasable {

    actual override fun release() {
        wgpuBindGroupRelease(segment)
    }

    override fun toString(): String {
        return "BindGroup"
    }
}

actual abstract class BindingLayout actual constructor() {
    abstract fun intoNative(
        bufferBinding: MemorySegment,
        samplerBinding: MemorySegment,
        textureBinding: MemorySegment,
        storageTextureBinding: MemorySegment
    )
}

actual class BufferBinding
actual constructor(val buffer: GPUBuffer, val offset: Long, val size: Long) : IntoBindingResource {

    override fun intoBindingResource(entry: MemorySegment) {
        WGPUBindGroupEntry.buffer(entry, buffer.segment)
        WGPUBindGroupEntry.offset(entry, offset)
        WGPUBindGroupEntry.size(entry, size)
    }
}

actual class BufferBindingLayout
actual constructor(
    val type: BufferBindingType,
    val hasDynamicOffset: Boolean,
    val minBindingSize: Long
) : BindingLayout() {

    override fun intoNative(
        bufferBinding: MemorySegment,
        samplerBinding: MemorySegment,
        textureBinding: MemorySegment,
        storageTextureBinding: MemorySegment
    ) {
        WGPUBufferBindingLayout.type(bufferBinding, type.nativeVal)
        WGPUBufferBindingLayout.hasDynamicOffset(bufferBinding, hasDynamicOffset.toInt())
        WGPUBufferBindingLayout.minBindingSize(bufferBinding, minBindingSize)
    }
}

actual class TextureBindingLayout
actual constructor(
    val sampleType: TextureSampleType,
    val viewDimension: TextureViewDimension,
    val multisampled: Boolean
) : BindingLayout() {

    override fun intoNative(
        bufferBinding: MemorySegment,
        samplerBinding: MemorySegment,
        textureBinding: MemorySegment,
        storageTextureBinding: MemorySegment
    ) {
        WGPUTextureBindingLayout.sampleType(textureBinding, sampleType.nativeVal)
        WGPUTextureBindingLayout.viewDimension(textureBinding, viewDimension.nativeVal)
        WGPUTextureBindingLayout.multisampled(textureBinding, multisampled.toInt())
    }
}

actual class SamplerBindingLayout actual constructor(val type: SamplerBindingType) :
    BindingLayout() {
    override fun intoNative(
        bufferBinding: MemorySegment,
        samplerBinding: MemorySegment,
        textureBinding: MemorySegment,
        storageTextureBinding: MemorySegment
    ) {
        WGPUSamplerBindingLayout.type(samplerBinding, type.nativeVal)
    }
}

actual class BindGroupLayoutEntry
actual constructor(val binding: Int, val visibility: ShaderStage, val bindingLayout: BindingLayout)

actual class BindGroupLayout internal constructor(val segment: MemorySegment) : Releasable {

    actual override fun release() {
        wgpuBindGroupLayoutRelease(segment)
    }

    override fun toString(): String {
        return "BindGroupLayout"
    }
}

actual class PipelineLayout(val segment: MemorySegment) : Releasable {

    actual override fun release() {
        wgpuPipelineLayoutRelease(segment)
    }

    override fun toString(): String {
        return "PipelineLayout"
    }
}

class MemorySegmentList(segments: List<MemorySegment>) : List<MemorySegment> by segments

actual class PipelineLayoutDescriptor
internal constructor(val segments: MemorySegmentList, val label: String?) {
    actual constructor(
        bindGroupLayouts: List<BindGroupLayout>,
        label: String?
    ) : this(MemorySegmentList(bindGroupLayouts.map { it.segment }), label)

    actual constructor(
        bindGroupLayout: BindGroupLayout,
        label: String?
    ) : this(listOf(bindGroupLayout))
}
