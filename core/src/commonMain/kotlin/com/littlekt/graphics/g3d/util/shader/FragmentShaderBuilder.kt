package com.littlekt.graphics.g3d.util.shader

/**
 * @author Colton Daily
 * @date 12/13/2024
 */
open class FragmentShaderBuilder {
    private val parts = mutableListOf<String>()

    fun unlit(block: UnlitMaterialBuilder.() -> Unit) {
        val builder = UnlitMaterialBuilder()
        builder.block()
        parts += builder.build()
    }

    fun pbr(block: PBRMaterialBuilder.() -> Unit) {
        val builder = PBRMaterialBuilder()
        builder.block()
        parts += builder.build()
    }

    fun <T : SubFragmentShaderBuilder> from(builder: T, block: T.() -> Unit) {
        builder.block()
        parts += builder.build()
    }

    fun build(): String {
        return parts.joinToString("\n")
    }
}
