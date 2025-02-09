package com.littlekt.graphics.shader.builder

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class ShaderBlock(
    val structs: Set<ShaderStruct>,
    val bindingGroups: Set<ShaderBindGroup>,
    val blocks: List<String>,
    val rules: List<ShaderBlockInsertRule>,
    val body: String,
) : ShaderSrc() {
    override val src: String by lazy {
        val markerRegex = "%\\w+%".toRegex()
        val lines =
            buildString {
                    structs.forEach { appendLine(it.src) }
                    bindingGroups.forEach { append(it.src) }
                    blocks.forEach { appendLine(it) }
                    appendLine(body)
                }
                .lines()
                .map { it.trim() }
                .toMutableList()

        rules.forEach { rule ->
            val marker = "%${rule.marker}%"
            when (rule.type) {
                ShaderBlockInsertType.BEFORE -> {
                    for (i in lines.indices) {
                        if (lines[i].trim() == marker) {
                            lines[i] = lines[i].replace(marker, "${rule.block.src}\n${marker}")
                            break
                        }
                    }
                }
                ShaderBlockInsertType.AFTER -> {
                    for (i in lines.indices) {
                        if (lines[i].trim() == marker) {
                            var nextMarkerIndex = -1
                            var nextMarker: String? = null
                            for (j in i until lines.size) {
                                if (markerRegex.matches(lines[j].trim())) {
                                    nextMarkerIndex = j
                                    nextMarker = lines[j].trim()
                                    break
                                }
                            }

                            if (nextMarkerIndex != -1 && nextMarker != null) {
                                // if there's another marker, insert the body before it
                                lines[nextMarkerIndex] =
                                    lines[nextMarkerIndex].replace(
                                        nextMarker,
                                        "${rule.block.src}\n${nextMarker}",
                                    )
                            } else {
                                // if no next marker, append the body to the end of the body
                                lines[lines.indices.last] =
                                    "${lines[lines.indices.last]}\n${rule.block.src}"
                            }
                            break
                        }
                    }
                }
            }
        }
        lines.joinToString("\n") { it.trim() }.format().trimIndent()
    }
}
