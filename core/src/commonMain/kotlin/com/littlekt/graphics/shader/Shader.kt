package com.littlekt.graphics.shader

import com.littlekt.Releasable
import com.littlekt.graphics.util.BindingUsage
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

    /**
     * The list of [BindGroupLayout]s described by the layout plus any optional BindGroupLayouts
     * passed in via [getOrCreatePipelineLayout]
     */
    var layouts: List<BindGroupLayout> = emptyList()
        private set

    /** The list of [BindGroupLayout]s described by the layout for this specific Shader. */
    private val shaderLayouts: List<BindGroupLayout> =
        layout.map { device.createBindGroupLayout(it) }

    /**
     * The [PipelineLayout] created by using [layouts]. If additional [BindGroupLayout] needs to be
     * passed in to the layout then use [getOrCreatePipelineLayout] before accessing this field.
     */
    val pipelineLayout: PipelineLayout
        get() = getOrCreatePipelineLayout()

    private var _pipelineLayout: PipelineLayout? = null

    /**
     * @param build an optional builder function to allow passing in additional [BindGroupLayout] to
     *   the [PipelineLayout] if the shader requires it. The [build] will pass in the [layouts] of
     *   the Shader as the parameter and requires a list of [BindGroupLayout] to proceed.
     * @return an existing [PipelineLayout] or creates a new one if it doesn't exist.
     */
    fun getOrCreatePipelineLayout(
        build: ((List<BindGroupLayout>) -> List<BindGroupLayout>) = { it }
    ): PipelineLayout {
        return _pipelineLayout
            ?: run {
                layouts = build(shaderLayouts)
                device
                    .createPipelineLayout(
                        PipelineLayoutDescriptor(
                            layouts,
                            label = "${this::class.simpleName}($id) PipeLineLayout",
                        )
                    )
                    .also { _pipelineLayout = it }
            }
    }

    /** External [BindGroup] that is passed in and should be set to the correct binding. */
    open fun setBindGroup(
        renderPassEncoder: RenderPassEncoder,
        bindGroup: BindGroup,
        bindingUsage: BindingUsage,
        dynamicOffsets: List<Long> = emptyList(),
    ) = Unit

    /** Set any internal bind groups here. */
    open fun setBindGroups(renderPassEncoder: RenderPassEncoder) = Unit

    /**
     * Create the applicable [BindGroup] based off of the [BindingUsage], if required.
     *
     * @param args requires arguments needed to create bind group. Ensure the order of arguments is
     *   correct before creating bind group.
     */
    open fun createBindGroup(usage: BindingUsage, vararg args: Any): BindGroup? = null

    override fun release() {
        shaderModule.release()
        shaderLayouts.fastForEach { it.release() }
        _pipelineLayout?.release()
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
