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
            .lines()
            .filterNot { markerRegex.matches(it.trim()) }
            .joinToString("\n") { it.trim() }
            .trim()
            .format()
    }

    private fun String.format(): String {
        val indentSize = 4
        var indentLevel = 0
        val formattedCode = StringBuilder()
        var lastLineWasBlank = false
        val code = this

        code.lines().forEach { line ->
            val trimmedLine = line.trim()

            if (trimmedLine.isEmpty()) {
                if (!lastLineWasBlank) {
                    formattedCode.appendLine()
                    lastLineWasBlank = true
                }
                return@forEach
            }

            lastLineWasBlank = false

            if (trimmedLine.startsWith("}")) {
                indentLevel = maxOf(0, indentLevel - 1)
            }

            formattedCode.append(" ".repeat(indentLevel * indentSize))
            formattedCode.appendLine(trimmedLine)

            if (trimmedLine.endsWith("{")) {
                indentLevel++
            }
        }

        return formattedCode.toString()
    }

    override fun toString(): String {
        return src
    }
}
