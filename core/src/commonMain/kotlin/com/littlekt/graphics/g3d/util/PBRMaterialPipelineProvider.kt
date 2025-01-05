package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.shader.PBRShader
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 12/12/2024
 */
class PBRMaterialPipelineProvider : BaseMaterialPipelineProvider<PBRMaterial>() {

    override fun createMaterialPipeline(
        device: Device,
        environment: Environment,
        layout: VertexBufferLayout,
        topology: PrimitiveTopology,
        material: PBRMaterial,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
    ): MaterialPipeline {
        val shader =
            PBRShader(
                device = device,
                layout = layout.attributes,
                baseColorTexture = material.baseColorTexture,
                baseColorFactor = material.baseColorFactor,
                metallicFactor = material.metallicFactor,
                roughnessFactor = material.roughnessFactor,
                metallicRoughnessTexture = material.metallicRoughnessTexture,
                normalTexture = material.normalTexture,
                emissiveFactor = material.emissiveFactor,
                emissiveTexture = material.emissiveTexture,
                occlusionTexture = material.occlusionTexture,
                occlusionStrength = material.occlusionStrength,
                transparent = material.transparent,
                doubleSided = material.doubleSided,
                alphaCutoff = material.alphaCutoff,
                castShadows = material.castShadows,
                depthWrite = material.depthWrite,
                depthCompareFunction = material.depthCompareFunction,
            )
        val bindGroups = listOf(environment.buffers.bindGroup) + shader.createBindGroups()

        val renderPipeline =
            device.createRenderPipeline(
                RenderPipelineDescriptor(
                    layout = shader.pipelineLayout,
                    vertex =
                        VertexState(
                            module = shader.shaderModule,
                            entryPoint = shader.vertexEntryPoint,
                            layout.gpuVertexBufferLayout,
                        ),
                    fragment =
                        FragmentState(
                            module = shader.shaderModule,
                            entryPoint = shader.fragmentEntryPoint,
                            target =
                                ColorTargetState(
                                    format = colorFormat,
                                    blendState =
                                        if (material.transparent) BlendState.Alpha
                                        else BlendState.Opaque,
                                    writeMask = ColorWriteMask.ALL,
                                ),
                        ),
                    primitive =
                        PrimitiveState(
                            topology = topology,
                            cullMode = if (material.doubleSided) CullMode.NONE else CullMode.BACK,
                        ),
                    depthStencil =
                        DepthStencilState(
                            depthFormat,
                            material.depthWrite,
                            material.depthCompareFunction,
                        ),
                    multisample =
                        MultisampleState(
                            count = 1,
                            mask = 0xFFFFFFF,
                            alphaToCoverageEnabled = false,
                        ),
                )
            )

        return MaterialPipeline(
            shader = shader,
            environment = environment,
            renderOrder =
                if (material.transparent) RenderOrder.TRANSPARENT else RenderOrder.DEFAULT,
            layout = layout,
            renderPipeline = renderPipeline,
            bindGroups = bindGroups,
        )
    }
}
