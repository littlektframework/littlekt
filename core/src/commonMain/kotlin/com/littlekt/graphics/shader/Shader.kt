package com.littlekt.graphics.shader

import com.littlekt.Releasable
import com.littlekt.util.datastructure.fastForEach
import io.ygdrasil.wgpu.*

/**
 * A base shader class to handle creating and customizing the required bind groups without the need
 * to follow a strict binding order.
 *
 * @param device the current [Device]
 * @param src the WGSL shader source code
 * @param layout a list of [BindGroupLayoutDescriptor] in order to create [BindGroupLayout]s for the
 *   [PipelineLayout]. The order should match the index of the [BindGroupLayout].
 * @param vertexEntryPoint the entry point for the Vertex shader. Defaults to `vs_main`.
 * @param fragmentEntryPoint the entry point for the Fragment shader. Defaults to `fs_main`
 * @author Colton Daily
 * @date 4/14/2024
 */
open class Shader(
    val device: Device,
    val src: String,
    layout: List<BindGroupLayoutDescriptor>,
    val vertexEntryPoint: String = "vs_main",
    val fragmentEntryPoint: String = "fs_main"
) : Releasable {
    /** The id of this shader. */
    val id: Int = lastId++

    /** The [ShaderModule]/ */
    val shaderModule = device.createShaderModule(ShaderModuleDescriptor(src))

    /** The list of [BindGroupLayout]s described by the layout */
    val layouts = layout.map { device.createBindGroupLayout(it) }

    /** The [PipelineLayout] created by using [layouts]. */
    val pipelineLayout: PipelineLayout =
        device.createPipelineLayout(PipelineLayoutDescriptor(layouts))

    private val bindGroups: MutableList<BindGroup> = mutableListOf()

    /**
     * Create a new list of bind groups that must be tracked and passed into [setBindGroups]. This
     * list must be tracked due to the fact of being able to swap out bind groups for each shader
     * based on dynamic data, such as, textures. It is best to call this once and cache the result
     * based on the data it needs.
     *
     * @param data a data map that can include any data that is needed in order to create bind
     *   groups, using a string as a key.
     * @return a list of [BindGroup]s for the shader that. This [Shader] is responsible for
     *   releasing the newly created [BindGroup] by calling [release].
     */
    fun createBindGroups(data: Map<String, Any> = emptyMap()): List<BindGroup> {
        val newBindGroups = mutableListOf<BindGroup>()
        newBindGroups.createBindGroupsInternal(data)
        bindGroups += newBindGroups
        return newBindGroups.toList()
    }

    /**
     * Do any buffer updates here.
     *
     * @param data a data map that can include any data that is needed in order to update bindings ,
     *   using a string as a key.
     */
    open fun update(data: Map<String, Any>) = Unit

    /**
     * Add the newly created bind groups to the given list.
     *
     * ```
     * fun MutableList<BindGroup>.createBindGroupsInternal(data: Map<String, Any>): List<BindGroup> {
     *     val texture = data["texture"] as Texture
     *     add(device.createBindGroup(createTextureDescriptor(texture))
     *     add(device.createBindGroup(anotherDescriptor)
     * }
     * ```
     *
     * @param data the data needed in order to create the bind groups.
     */
    protected open fun MutableList<BindGroup>.createBindGroupsInternal(data: Map<String, Any>) =
        Unit

    /**
     * Set the [BindGroup]s to the correct binding index on the given [RenderPassEncoder].
     *
     * @param encoder the [RenderPassEncoder] to encode draw commands with
     * @param bindGroups the list of bind groups to bind. This assumes the bind list of bind groups
     *   were created with [createBindGroups] and are in the correct order.
     * @param dynamicOffsets the list of offsets, for any dynamic bind groups, if applicable
     */
    open fun setBindGroups(
        encoder: RenderPassEncoder,
        bindGroups: List<BindGroup>,
        dynamicOffsets: List<Long> = emptyList()
    ) = Unit

    override fun release() {
        shaderModule.close()
        layouts.fastForEach { it.close() }
        pipelineLayout.close()
        bindGroups.forEach { it.close() }
        bindGroups.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Shader

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        private var lastId = 0
    }
}
