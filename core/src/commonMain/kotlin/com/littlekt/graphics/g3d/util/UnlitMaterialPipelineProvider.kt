package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.g3d.material.UnlitMaterial
import com.littlekt.graphics.g3d.shader.UnlitShader
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 12/12/2024
 */
class UnlitMaterialPipelineProvider : BaseMaterialPipelineProvider<UnlitMaterial>() {

    override fun createMaterialPipeline(
        device: Device,
        cameraBuffers: CameraBuffers,
        layout: VertexBufferLayout,
        topology: PrimitiveTopology,
        material: UnlitMaterial,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
    ): MaterialPipeline {
        val shader =
            UnlitShader(
                device = device,
                cameraBuffers = cameraBuffers,
                layout = layout.attributes,
                baseColorTexture = material.baseColorTexture,
                baseColorFactor = material.baseColorFactor,
                transparent = material.transparent,
                doubleSided = material.doubleSided,
                alphaCutoff = material.alphaCutoff,
                castShadows = material.castShadows,
                depthWrite = material.depthWrite,
                depthCompareFunction = material.depthCompareFunction,
            )
        val bindGroups = shader.createBindGroups()

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
            renderOrder =
                if (material.transparent) RenderOrder.TRANSPARENT else RenderOrder.DEFAULT,
            layout = layout,
            renderPipeline = renderPipeline,
            bindGroups = bindGroups,
        )
    }
}
