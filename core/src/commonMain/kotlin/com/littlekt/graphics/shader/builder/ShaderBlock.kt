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
        var lines =
            buildString {
                    structs.forEach { appendLine(it.src) }
                    bindingGroups.forEach { appendLine(it.src) }
                    blocks.forEach { appendLine(it) }
                    appendLine() // padding
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
                            lines[i] = lines[i].replace(marker, "${rule.block.body}\n${marker}")
                            break
                        }
                    }
                }
                ShaderBlockInsertType.AFTER -> {
                    for (i in lines.indices) {
                        if (lines[i].trim() == marker) {
                            var nextMarkerIndex = -1
                            var nextMarker: String? = null
                            for (j in i + 1 until lines.size) {
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
                                        "${rule.block.body}\n${nextMarker}",
                                    )
                            } else {
                                // if no next marker, append the body to the end of the body
                                lines[lines.indices.last] =
                                    "${lines[lines.indices.last]}\n${rule.block.body}"
                            }
                            break
                        }
                    }
                }
            }
        }
        lines = lines.joinToString("\n") { it.trim() }.lines().toMutableList()
        lines.removeAll { markerRegex.matches(it.trim()) }
        lines.joinToString("\n") { it.trim() }.format().trimIndent()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ShaderBlock

        if (structs != other.structs) return false
        if (bindingGroups != other.bindingGroups) return false
        if (blocks != other.blocks) return false
        if (rules != other.rules) return false
        if (body != other.body) return false
        if (src != other.src) return false

        return true
    }

    override fun hashCode(): Int {
        var result = structs.hashCode()
        result = 31 * result + bindingGroups.hashCode()
        result = 31 * result + blocks.hashCode()
        result = 31 * result + rules.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + src.hashCode()
        return result
    }

    override fun toString(): String {
        return "ShaderBlock(structs=$structs, bindingGroups=$bindingGroups, blocks=$blocks, rules=$rules, body='$body')"
    }
}
