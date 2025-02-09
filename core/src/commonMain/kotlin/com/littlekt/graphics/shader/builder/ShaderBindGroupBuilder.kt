package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.util.BindingUsage

open class ShaderBindGroupBuilder(val group: Int, val usage: BindingUsage) {
    protected val bindings = mutableListOf<ShaderBinding>()

    fun bind(shaderBinding: ShaderBinding, bindingIdx: Int = shaderBinding.binding) {
        bindings.add(shaderBinding.copy(group = group, binding = bindingIdx))
    }

    fun bind(
        bindingIdx: Int,
        varName: String,
        paramType: ShaderBindingParameterType,
        bindingType: ShaderBindingType,
    ) {
        bindings += ShaderBinding(group, bindingIdx, varName, paramType, bindingType)
    }

    fun bind(
        bindingIdx: Int,
        varName: String,
        struct: ShaderStruct,
        bindingType: ShaderBindingType,
    ) = bind(bindingIdx, varName, ShaderBindingParameterType.Struct(struct), bindingType)

    fun bindTexture2d(bindingIdx: Int, varName: String) =
        bind(
            bindingIdx,
            varName,
            ShaderBindingParameterType.WgslType.texture_f32,
            ShaderBindingType.Plain,
        )

    fun bindSampler(bindingIdx: Int, varName: String) =
        bind(
            bindingIdx,
            varName,
            ShaderBindingParameterType.WgslType.sampler,
            ShaderBindingType.Plain,
        )

    fun bindArray(
        bindingIdx: Int,
        varName: String,
        struct: ShaderStruct,
        length: Int,
        bindingType: ShaderBindingType,
    ) =
        bind(
            bindingIdx,
            varName,
            ShaderBindingParameterType.Array(ShaderBindingParameterType.Struct(struct), length),
            bindingType,
        )

    fun build(): ShaderBindGroup {
        return ShaderBindGroup(group, usage, bindings)
    }
}
