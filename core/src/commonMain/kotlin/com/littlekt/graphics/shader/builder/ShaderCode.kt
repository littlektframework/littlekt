package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.BindGroupLayoutDescriptor

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
    private data class BindGroupInfo(
        val bindingUsage: BindingUsage,
        val group: Int,
        val descriptor: BindGroupLayoutDescriptor,
    )

    private val mainBlocks = blocks.filterIsInstance<MainShaderBlock>()
    private val shaderBindGroups = emptyList<BindGroupInfo>()
    //        (includes.filterIsInstance<ShaderBlockBindGroup>() +
    //                blocks.filterIsInstance<ShaderBlockBindGroup>())
    //            .sortedBy { it.group }
    //            .groupBy { it.group }
    //            .map { (group, groupItems) ->
    //                val combinedUsage =
    //                    groupItems.fold(setOf<String>()) { acc, item -> acc +
    // item.bindingUsage.usage }
    //                val combinedEntries =
    //                    groupItems
    //                        .flatMap { it.descriptor.entries }
    //                        .distinctBy { it.binding }
    //                        .sortedBy { it.binding }
    //                val newDescriptor = BindGroupLayoutDescriptor(combinedEntries)
    //
    //                BindGroupInfo(BindingUsage(combinedUsage), group, newDescriptor)
    //            }
    val bindGroupLayoutUsageLayout: List<BindingUsage> =
        emptyList() // shaderBindGroups.map { it.bindingUsage }
    val bindGroupUsageToGroupIndex: Map<BindingUsage, Int> = emptyMap()
    // shaderBindGroups.associate { it.bindingUsage to it.group }
    val layout: Map<BindingUsage, BindGroupLayoutDescriptor> = emptyMap()
    //  shaderBindGroups.associate { it.bindingUsage to it.descriptor }
    val vertexEntryPoint = vertex?.entryPoint
    val fragmentEntryPoint = fragment?.entryPoint
    val computeEntryPoint = compute?.entryPoint

    override val src: String by lazy {
        val markerRegex = "%\\w+%".toRegex()
        val lines =
            buildString {
                    structs.forEach { appendLine(it.src) }
                    bindingGroups.forEach { append(it.src) }
                    blocks.forEach { appendLine(it) }
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
        lines.removeAll { markerRegex.matches(it.trim()) }
        lines.joinToString("\n") { it.trim() }.format().trimIndent()
    }

    override fun toString(): String {
        return src
    }
}
