package com.littlekt.graphics.shader

import com.littlekt.Releasable
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.datastructure.fastForEach

/**
 * A base shader class to handle creating and customizing the required bind groups without the need
 * to follow a strict binding order.
 *
 * @param device the current [Device]
 * @param src the WGSL shader source code
 * @param layout a list of [BindGroupLayoutDescriptor] in order to create [BindGroupLayout]s for the
 *   [PipelineLayout]. The order should match the index of the [BindGroupLayout].
 * @param vertexEntryPoint the entry point for the Vertex shader. Defaults to `vs_main`. This should
 *   match the main vertex function in [src]. Pass this parameter along to [VertexState.entryPoint],
 *   if a vertex function is supplied; otherwise this value may be safely ignored.
 * @param fragmentEntryPoint the entry point for the Fragment shader. Defaults to `fs_main`. This
 *   should match the main fragment function in [src]. Pass this parameter along to
 *   [FragmentState.entryPoint], if a fragment function is supplied; otherwise this value may be
 *   safely ignored.
 * @param computeEntryPoint the entry point for a compute shader. Defaults to `cmp_main`. This
 *   should match the main compute function in [src], Pass this parameter along to
 *   [ProgrammableStage.entryPoint], if a compute function is supplied; otherwise this value may be
 *   safely ignored.
 * @author Colton Daily
 * @date 4/14/2024
 */
open class Shader(
    val device: Device,
    val src: String,
    layout: List<BindGroupLayoutDescriptor>,
    val vertexEntryPoint: String = "vs_main",
    val fragmentEntryPoint: String = "fs_main",
    val computeEntryPoint: String = "cmp_main",
) : Releasable {
    /** The id of this shader. */
    val id: Int = lastId++

    /** The [ShaderModule]/ */
    val shaderModule = device.createShaderModule(src)

    /** The list of [BindGroupLayout]s described by the layout */
    val layouts = layout.map { device.createBindGroupLayout(it) }

    /**
     * The [PipelineLayout] created by using [layouts]. If additional [BindGroupLayout] needs to be
     * passed in to the layout then use [getOrCreatePipelineLayout] before accessing this field.
     */
    val pipelineLayout: PipelineLayout
        get() = getOrCreatePipelineLayout()

    private var pipelineLayoutOrNull: PipelineLayout? = null

    /**
     * @param build an optional builder function to allow passing in additional [BindGroupLayout] to
     *   the [PipelineLayout] if the shader requires it. The [build] will pass in the [layouts] of
     *   the Shader as the parameter and requires a list of [BindGroupLayout] to proceed.
     * @return an existing [PipelineLayout] or creates a new one if it doesn't exist.
     */
    fun getOrCreatePipelineLayout(
        build: ((List<BindGroupLayout>) -> List<BindGroupLayout>) = { it }
    ): PipelineLayout {
        return pipelineLayoutOrNull
            ?: device.createPipelineLayout(PipelineLayoutDescriptor(build(layouts))).also {
                pipelineLayoutOrNull = it
            }
    }

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

    fun createBindGroups(bindings: List<IntoBindingResource>) {}

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
     * Set the [BindGroup]s to the correct binding index on the given [RenderPassEncoder]. By
     * default, this will set the bind groups in list order omitting the use of [dynamicOffsets].
     * Override this to set own bind group order / functionality.
     *
     * @param encoder the [RenderPassEncoder] to encode draw commands with
     * @param bindGroups the list of bind groups to bind. This assumes the bind list of bind groups
     *   were created with [createBindGroups] and are in the correct order.
     * @param dynamicOffsets the list of offsets, for any dynamic bind groups, if applicable
     */
    open fun setBindGroups(
        encoder: RenderPassEncoder,
        bindGroups: List<BindGroup>,
        dynamicOffsets: List<Long> = emptyList(),
    ) {
        bindGroups.forEachIndexed { index, bindGroup -> encoder.setBindGroup(index, bindGroup) }
    }

    override fun release() {
        shaderModule.release()
        layouts.fastForEach { it.release() }
        pipelineLayout.release()
        bindGroups.forEach { it.release() }
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
