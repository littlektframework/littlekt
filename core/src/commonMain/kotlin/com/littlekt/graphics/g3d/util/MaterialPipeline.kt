package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.shader.Shader
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
    override fun compareTo(other: MaterialPipeline): Int {
        return renderOrder.order - other.renderOrder.order
    }
}
