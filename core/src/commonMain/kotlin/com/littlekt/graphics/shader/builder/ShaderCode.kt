package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.BindGroupLayoutDescriptor
import com.littlekt.graphics.webgpu.ShaderStage

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
class ShaderCode(
    val structs: Set<ShaderStruct>,
    val bindingGroups: Set<ShaderBindGroup>,
    val blocks: List<String>,
    val rules: List<ShaderBlockInsertRule>,
    val vertex: VertexShaderBlock?,
    val fragment: FragmentShaderBlock?,
    val compute: ComputeShaderBlock?,
) : ShaderSrc() {

    val visibility: ShaderStage = run {
        var visibility: ShaderStage? = null
        if (vertex != null) visibility = ShaderStage.VERTEX
        if (fragment != null)
            visibility = visibility?.let { it or ShaderStage.FRAGMENT } ?: ShaderStage.FRAGMENT
        if (compute != null)
            visibility = visibility?.let { it or ShaderStage.COMPUTE } ?: ShaderStage.COMPUTE
        visibility ?: error("Shader must have at least one shader stage!")
    }
    private val bindGroupsToDescriptors: Map<ShaderBindGroup, BindGroupLayoutDescriptor> =
        bindingGroups.associateWith { it.generateBindGroupLayoutDescriptor(visibility) }

    val bindGroupLayoutUsageLayout: List<BindingUsage> = bindingGroups.map { it.usage }
    val bindGroupUsageToGroupIndex: Map<BindingUsage, Int> =
        bindingGroups.associate { it.usage to it.group }
    val layout: Map<BindingUsage, BindGroupLayoutDescriptor> =
        bindGroupsToDescriptors.map { it.key.usage to it.value }.toMap()

    val vertexEntryPoint = vertex?.entryPoint
    val fragmentEntryPoint = fragment?.entryPoint
    val computeEntryPoint = compute?.entryPoint

    override val src: String by lazy {
        var lines =
            buildString {
                    structs.forEach { appendLine(it.src) }
                    bindingGroups.forEach { appendLine(it.src) }
                    blocks.forEach { appendLine(it) }
                    appendLine() // padding
                    vertex?.let { appendLine(it.body) }
                    fragment?.let { appendLine(it.body) }
                    compute?.let { appendLine(it.body) }
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

    override fun toString(): String {
        return src
    }
}
