package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.g3d.MeshPrimitive
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.util.CameraBuffers
import com.littlekt.graphics.webgpu.BindGroup
import com.littlekt.graphics.webgpu.RenderPipeline

/**
 * Represents a pipeline configuration for rendering with a specific material setup.
 *
 * @property shader Specifies the shader used in the rendering pipeline, which defines how the
 *   material is shaded and rendered.
 * @property environment Defines the environment settings or properties (e.g., lighting, fog, etc.)
 *   that influence the material's appearance during rendering.
 * @property renderOrder Configures the order in which this material pipeline is rendered relative
 *   to others, allowing control over render priority.
 * @property layout Specifies the vertex buffer layout that determines how vertex data is provided
 *   to the shader.
 * @property renderPipeline Refers to the rendering pipeline which establishes the state and
 *   settings for rendering, including shaders and pipeline configurations.
 */
data class MaterialPipeline(
    val shader: Shader,
    var environment: Environment,
    val renderOrder: RenderOrder,
    val layout: VertexBufferLayout,
    val renderPipeline: RenderPipeline,
) : Comparable<MaterialPipeline> {
    internal val modelGroupIndex = shader.getBindGroupLayoutIndexOrNull(BindingUsage.MODEL) ?: -1
    internal val materialGroupIndex =
        shader.getBindGroupLayoutIndexOrNull(BindingUsage.MATERIAL) ?: -1
    internal val skinGroupIndex = shader.getBindGroupLayoutIndexOrNull(BindingUsage.SKIN) ?: -1
    internal val modelLayout = shader.getBindGroupLayoutByUsageOrNull(BindingUsage.MODEL)

    internal val queue = mutableListOf<MeshPrimitive>()
    internal val queueMaterials = mutableListOf<Material>()
    internal var queuedFlush = -1L

    private var environmentGroupEnvironment: Environment? = null
    private var environmentGroupIndexCache = -1
    private var environmentBindGroupBuffers: CameraBuffers? = null
    private var environmentBindGroupCache: BindGroup? = null

    internal fun environmentGroupIndex(): Int {
        val environment = environment
        if (environmentGroupEnvironment !== environment) {
            environmentGroupEnvironment = environment
            environmentGroupIndexCache =
                shader.getBindGroupLayoutIndexOrNull(environment.buffers.bindingUsage) ?: -1
        }
        return environmentGroupIndexCache
    }

    internal fun environmentBindGroup(buffers: CameraBuffers): BindGroup {
        val cached = environmentBindGroupCache
        if (environmentBindGroupBuffers === buffers && cached != null) return cached
        return buffers.getOrCreateBindGroup(shader).also {
            environmentBindGroupBuffers = buffers
            environmentBindGroupCache = it
        }
    }

    override fun compareTo(other: MaterialPipeline): Int {
        return renderOrder.order - other.renderOrder.order
    }
}
