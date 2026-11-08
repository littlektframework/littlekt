package com.littlekt.graphics.g3d.shadow

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.g3d.shader.blocks.Standard
import com.littlekt.graphics.g3d.util.BaseMaterialPipelineProvider
import com.littlekt.graphics.g3d.util.MaterialPipeline
import com.littlekt.graphics.g3d.util.RenderOrder
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.shader.builder.shader
import com.littlekt.graphics.webgpu.*
import kotlin.reflect.KClass

/**
 * A [BaseMaterialPipelineProvider] that creates depth only pipelines for rendering shadow casters
 * into a shadow map. Materials that don't [Material.castShadows] are skipped. Meant to be used as
 * the [com.littlekt.graphics.g3d.ModelBatch.defaultPipelineProvider] of a shadow
 * [com.littlekt.graphics.g3d.ModelBatch].
 *
 * @param depthBias the constant depth bias added to each fragment.
 * @param slopeScaleBias the depth bias scaled by the fragment's slope.
 * @author Colton Daily
 * @date 11/8/2026
 */
class ShadowMaterialPipelineProvider(
    val depthBias: Int = 2,
    val slopeScaleBias: Float = 2f,
) : BaseMaterialPipelineProvider<Material>() {
    override val type: KClass<Material> = Material::class

    override fun getMaterialPipeline(
        device: Device,
        material: Material,
        environment: Environment,
        layout: VertexBufferLayout,
        topology: PrimitiveTopology,
        stripIndexFormat: IndexFormat?,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
        sampleCount: Int,
    ): MaterialPipeline? {
        if (!material.castShadows) return null
        return super.getMaterialPipeline(
            device,
            material,
            environment,
            layout,
            topology,
            stripIndexFormat,
            colorFormat,
            depthFormat,
            sampleCount,
        )
    }

    override fun createMaterialPipeline(
        device: Device,
        environment: Environment,
        layout: VertexBufferLayout,
        topology: PrimitiveTopology,
        material: Material,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
        sampleCount: Int,
    ): MaterialPipeline {
        val vertexShaderCode =
            if (material.skinned) Standard.SkinnedVertexShader(layout.attributes, skinGroup = 2)
            else Standard.VertexShader(layout.attributes)
        val shader = Shader(device, shader { vertex(vertexShaderCode.vertex) })

        val renderPipeline =
            device.createRenderPipeline(
                RenderPipelineDescriptor(
                    layout = shader.getOrCreatePipelineLayout(),
                    vertex =
                        VertexState(
                            module = shader.shaderModule,
                            entryPoint = shader.vertexEntryPoint,
                            layout.gpuVertexBufferLayout,
                        ),
                    fragment = null,
                    primitive =
                        PrimitiveState(
                            topology = topology,
                            cullMode = if (material.doubleSided) CullMode.NONE else CullMode.BACK,
                        ),
                    depthStencil =
                        DepthStencilState(
                            depthFormat,
                            true,
                            CompareFunction.LESS,
                            bias = DepthBiasState(depthBias, slopeScaleBias, 0f),
                        ),
                    multisample =
                        MultisampleState(
                            count = sampleCount,
                            mask = 0xFFFFFFF,
                            alphaToCoverageEnabled = false,
                        ),
                    label = "Shadow Caster Pipeline",
                )
            )

        return MaterialPipeline(
            shader = shader,
            environment = environment,
            renderOrder = RenderOrder.DEFAULT,
            layout = layout,
            renderPipeline = renderPipeline,
        )
    }
}
