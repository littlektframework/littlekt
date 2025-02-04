package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.shader.PBRShader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 12/12/2024
 */
class PBRMaterialPipelineProvider : BaseMaterialPipelineProvider<PBRMaterial>() {
    override val type: KClass<PBRMaterial> = PBRMaterial::class

    override fun createMaterialPipeline(
        device: Device,
        environment: Environment,
        layout: VertexBufferLayout,
        topology: PrimitiveTopology,
        material: PBRMaterial,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
    ): MaterialPipeline {
        val shader = PBRShader(device, layout.attributes, material.skinned)

        val renderPipeline =
            device.createRenderPipeline(
                RenderPipelineDescriptor(
                    layout =
                        shader.getOrCreatePipelineLayout {
                            if (it == BindingUsage.CAMERA) environment.buffers.bindGroupLayout
                            else error("Unsupported $it in UnlitMaterialPipelineProvider")
                        },
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
        )
    }
}
