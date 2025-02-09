package com.littlekt.graphics.shader.builder

import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.BindGroupLayoutDescriptor
import com.littlekt.graphics.webgpu.ShaderStage

/**
 * @author Colton Daily
 * @date 2/6/2025
 */
open class ShaderCode(
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ShaderCode

        if (structs != other.structs) return false
        if (bindingGroups != other.bindingGroups) return false
        if (blocks != other.blocks) return false
        if (rules != other.rules) return false
        if (vertex != other.vertex) return false
        if (fragment != other.fragment) return false
        if (compute != other.compute) return false
        if (visibility != other.visibility) return false
        if (bindGroupsToDescriptors != other.bindGroupsToDescriptors) return false
        if (bindGroupLayoutUsageLayout != other.bindGroupLayoutUsageLayout) return false
        if (bindGroupUsageToGroupIndex != other.bindGroupUsageToGroupIndex) return false
        if (layout != other.layout) return false
        if (vertexEntryPoint != other.vertexEntryPoint) return false
        if (fragmentEntryPoint != other.fragmentEntryPoint) return false
        if (computeEntryPoint != other.computeEntryPoint) return false

        return true
    }

    override fun hashCode(): Int {
        var result = structs.hashCode()
        result = 31 * result + bindingGroups.hashCode()
        result = 31 * result + blocks.hashCode()
        result = 31 * result + rules.hashCode()
        result = 31 * result + (vertex?.hashCode() ?: 0)
        result = 31 * result + (fragment?.hashCode() ?: 0)
        result = 31 * result + (compute?.hashCode() ?: 0)
        result = 31 * result + visibility.hashCode()
        result = 31 * result + bindGroupsToDescriptors.hashCode()
        result = 31 * result + bindGroupLayoutUsageLayout.hashCode()
        result = 31 * result + bindGroupUsageToGroupIndex.hashCode()
        result = 31 * result + layout.hashCode()
        result = 31 * result + (vertexEntryPoint?.hashCode() ?: 0)
        result = 31 * result + (fragmentEntryPoint?.hashCode() ?: 0)
        result = 31 * result + (computeEntryPoint?.hashCode() ?: 0)
        return result
    }
}
