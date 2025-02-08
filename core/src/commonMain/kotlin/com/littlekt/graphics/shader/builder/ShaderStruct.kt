package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
class ShaderStruct(val name: String, parameters: Map<String, ShaderStructParameterType>) {
    val layout: Map<String, ShaderStructEntry> =
        parameters
            .map { (name, type) ->
                val entry =
                    when (type) {
                        is ShaderStructParameterType.Array -> TODO()
                        is ShaderStructParameterType.Struct -> TODO()
                        is ShaderStructParameterType.WgslType ->
                            ShaderStructEntry(0, type.size, type.alignment, type)
                    }
                name to entry
            }
            .toMap()
}
