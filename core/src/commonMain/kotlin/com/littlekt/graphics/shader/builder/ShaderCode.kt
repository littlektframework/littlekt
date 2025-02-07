package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
class ShaderCode(val includes: List<ShaderBlock>, val blocks: List<ShaderBlock>) {
    private val mainBlocks = blocks.filterIsInstance<MainShaderBlock>()
    val vertexEntryPoint = mainBlocks.firstOrNull { it.type == ShaderBlockType.VERTEX }?.entryPoint
    val fragmentEntryPoint =
        mainBlocks.firstOrNull { it.type == ShaderBlockType.FRAGMENT }?.entryPoint
    val computeEntryPoint =
        mainBlocks.firstOrNull { it.type == ShaderBlockType.COMPUTE }?.entryPoint

    val src by lazy {
        val markerRegex = "%\\w+%".toRegex()
        buildString {
                includes.forEach { appendLine(it.src) }
                blocks.forEach { appendLine(it.src) }
            }
            .split("\n")
            .filterNot { markerRegex.matches(it) }
            .joinToString("\n") { it }
            .trim()
    }
}
