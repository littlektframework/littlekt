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
 * @param bindGroupLayoutUsageLayout a list of [BindingUsage] that determines the order and each
 *   type of bind group layout. For example, if we had a list of camera and texture binding usgaes,
 *   then we'd expect two bind group layouts, one for camera, and the second for texture. This
 *   allows setting existing bind groups from elsewhere but ensuring the layout of the shader.
 * @param layout a list of [BindGroupLayoutDescriptor] in order to create [BindGroupLayout]s for the
 *   [PipelineLayout]. The order should match the index of the [bindGroupLayoutUsageLayout]. This
 *   can be an empty map if bind groups will be passed in at a later time.
 * @param bindGroupUsageToGroupIndex a mapping of [BindingUsage] to Bind group layout index. By
 *   default, it uses [bindGroupLayoutUsageLayout] and its corresponding index but this may be
 *   overridden, if required.
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
    val bindGroupLayoutUsageLayout: List<BindingUsage>,
    layout: Map<BindingUsage, BindGroupLayoutDescriptor>,
    val bindGroupUsageToGroupIndex: Map<BindingUsage, Int> =
        bindGroupLayoutUsageLayout
            .mapIndexed { index, bindingUsage -> bindingUsage to index }
            .toMap(),
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
    val layouts: Map<Int, BindGroupLayout>
        get() = _layouts

    private var _layouts: MutableMap<Int, BindGroupLayout> = mutableMapOf()

    private val usageToBindGroupLayout: MutableMap<BindingUsage, BindGroupLayout> = mutableMapOf()

    /** The list of [BindGroupLayout]s described by the layout for this specific Shader. */
    private val shaderLayouts: List<BindGroupLayout> =
        bindGroupLayoutUsageLayout.mapIndexedNotNull { index, usage ->
            val descriptor = layout[usage]
            if (descriptor != null) {
                val bindGroupLayout = device.createBindGroupLayout(descriptor)
                usageToBindGroupLayout[usage] = bindGroupLayout
                _layouts[index] = bindGroupLayout
                bindGroupLayout
            } else {
                null
            }
        }

    /**
     * The [PipelineLayout] created by using [layouts]. If additional [BindGroupLayout] needs to be
     * passed in to the layout then use [getOrCreatePipelineLayout] before accessing this field.
     */
    val pipelineLayout: PipelineLayout
        get() = getOrCreatePipelineLayout()

    private var _pipelineLayout: PipelineLayout? = null

    /**
     * @param provideBindGroupLayout an optional builder function to allow passing in additional
     *   [BindGroupLayout] to the [PipelineLayout] if the shader requires it. The
     *   [provideBindGroupLayout] will pass in the [BindingUsage] of the Shader as the parameter and
     *   requires a [BindGroupLayout] to proceed.
     * @return an existing [PipelineLayout] or creates a new one if it doesn't exist.
     */
    fun getOrCreatePipelineLayout(
        provideBindGroupLayout: ((BindingUsage) -> BindGroupLayout)? = null
    ): PipelineLayout {
        return _pipelineLayout
            ?: run {
                bindGroupLayoutUsageLayout.forEachIndexed { index, usage ->
                    if (!usageToBindGroupLayout.contains(usage) && provideBindGroupLayout != null) {
                        usageToBindGroupLayout[usage] = provideBindGroupLayout(usage)
                    }
                    _layouts[index] =
                        usageToBindGroupLayout[usage]
                            ?: error("$this: Unable to get BindGroupLayout for $usage")
                }
                _layouts = _layouts.toList().sortedBy { it.first }.toMap().toMutableMap()
                device
                    .createPipelineLayout(
                        PipelineLayoutDescriptor(
                            _layouts.map { it.value },
                            label = "$this PipeLineLayout",
                        )
                    )
                    .also { _pipelineLayout = it }
            }
    }

    /**
     * External [BindGroup] that is passed in and should be set to the correct binding. By default,
     * sets in the order of [bindGroupUsageToGroupIndex] and ignores [dynamicOffsets].
     */
    open fun setBindGroup(
        renderPassEncoder: RenderPassEncoder,
        bindGroup: BindGroup,
        bindingUsage: BindingUsage,
        dynamicOffsets: List<Long> = emptyList(),
    ) {
        val index = getBindGroupLayoutIndex(bindingUsage)
        renderPassEncoder.setBindGroup(index, bindGroup, dynamicOffsets.ifEmpty { emptyList() })
    }

    /**
     * @return the index for the given [usage] within the bind group layout.
     * @throws IllegalStateException if usage not found.
     * @see [getBindGroupLayoutIndexOrNull]
     */
    fun getBindGroupLayoutIndex(usage: BindingUsage): Int =
        getBindGroupLayoutIndexOrNull(usage) ?: error("Unable to find the group index for $usage.")

    /**
     * @return the index for the given [usage] within the bind group layout; `null` if not found.
     */
    fun getBindGroupLayoutIndexOrNull(usage: BindingUsage): Int? = bindGroupUsageToGroupIndex[usage]

    /**
     * @return get the bind group layout by [usage].
     * @throws IllegalStateException if not found
     * @see [getBindGroupLayoutByUsageOrNull]
     */
    fun getBindGroupLayoutByUsage(usage: BindingUsage) =
        getBindGroupLayoutByUsageOrNull(usage)
            ?: error("BindGroupLayout does exist for usage: $usage.")

    /** @return get the bind group layout by [usage]; `null` if not found. */
    fun getBindGroupLayoutByUsageOrNull(usage: BindingUsage): BindGroupLayout? {
        val index = bindGroupUsageToGroupIndex[usage] ?: return null
        return layouts[index]
    }

    /** Set any internal bind groups here. */
    open fun setBindGroups(renderPassEncoder: RenderPassEncoder) = Unit

    /**
     * Create the applicable [BindGroup] based off of the [BindingUsage], if required.
     *
     * @param args requires arguments needed to create bind group. Ensure the order of arguments is
     *   correct before creating bind group.
     */
    open fun createBindGroup(usage: BindingUsage, vararg args: IntoBindingResource): BindGroup? {
        val index = bindGroupUsageToGroupIndex[usage] ?: return null
        val layout = layouts[index] ?: return null
        return device.createBindGroup(
            BindGroupDescriptor(
                layout,
                args.mapIndexed { argsIndex, resource -> BindGroupEntry(argsIndex, resource) },
            )
        )
    }

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
