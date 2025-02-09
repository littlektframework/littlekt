package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 2/8/2025
 */
data class ShaderBinding(
    val group: Int,
    val binding: Int,
    val varName: String,
    val paramType: ShaderBindingParameterType,
    val bindingType: ShaderBindingType,
) : ShaderSrc() {

    override val src: String by lazy {
        "@group($group) @binding($binding) var${bindingType.name} $varName: ${paramType.name};"
    }

    fun generateBindingLayoutEntry(visibility: ShaderStage): BindGroupLayoutEntry {
        return BindGroupLayoutEntry(binding, visibility, bindingType.toBindingLayout(paramType))
    }

    private fun ShaderBindingType.toBindingLayout(
        paramType: ShaderBindingParameterType
    ): BindingLayout {
        return when (this) {
            ShaderBindingType.Plain ->
                when (paramType) {
                    is ShaderBindingParameterType.WgslType.texture_f32 -> TextureBindingLayout()
                    is ShaderBindingParameterType.WgslType.sampler -> SamplerBindingLayout()
                    else -> error("Unable to determine binding layout for ${paramType.name}")
                }
            is ShaderBindingType.Storage -> {
                when (access) {
                    MemoryAccessMode.READ ->
                        BufferBindingLayout(BufferBindingType.READ_ONLY_STORAGE)
                    MemoryAccessMode.WRITE ->
                        error("Binding cannot use write access! Use READ_WRITE instead.")
                    MemoryAccessMode.READ_WRITE -> BufferBindingLayout(BufferBindingType.STORAGE)
                }
            }
            ShaderBindingType.Uniform -> BufferBindingLayout(BufferBindingType.UNIFORM)
        }
    }
}
