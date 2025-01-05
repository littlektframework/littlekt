package com.littlekt.graphics.g3d.util.shader

/**
 * @author Colton Daily
 * @date 12/13/2024
 */
class ShaderBuilder {
    private var vertexShader: String? = null
    private var fragmentShader: String? = null
    private var computeShader: String? = null

    fun vertex(block: VertexShaderBuilder.() -> Unit) {
        val builder = VertexShaderBuilder()
        builder.block()
        vertexShader = builder.build()
    }

    fun fragment(block: FragmentShaderBuilder.() -> Unit) {
        val builder = FragmentShaderBuilder()
        builder.block()
        fragmentShader = builder.build()
    }

    fun compute(block: ComputeShaderBuilder.() -> Unit) {
        val builder = ComputeShaderBuilder()
        builder.block()
        computeShader = builder.build()
    }

    fun build(): String {
        return buildString {
            vertexShader?.let {
                append("// Vertex Shader\n")
                append(it)
                append("\n")
            }
            fragmentShader?.let {
                append("// Fragment Shader\n")
                append(it)
                append("\n")
            }
            computeShader?.let {
                append("// Compute Shader\n")
                append(it)
                append("\n")
            }
        }
    }
}

fun buildCommonShader(block: ShaderBuilder.() -> Unit): String {
    val builder = ShaderBuilder()
    builder.block()
    return builder.build()
}
