package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.g3d.material.UnlitMaterial
import com.littlekt.graphics.g3d.shader.blocks.Standard
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.shader.builder.shader
import com.littlekt.graphics.webgpu.*
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 12/12/2024
 */
class UnlitMaterialPipelineProvider : BaseMaterialPipelineProvider<UnlitMaterial>() {
    override val type: KClass<UnlitMaterial> = UnlitMaterial::class

    override fun createMaterialPipeline(
        device: Device,
        environment: Environment,
        layout: VertexBufferLayout,
        topology: PrimitiveTopology,
        material: UnlitMaterial,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
    ): MaterialPipeline {
        val vertexShaderCode =
            if (material.skinned) Standard.SkinnedVertexShader(layout.attributes)
            else Standard.VertexShader(layout.attributes)
        val fragmentShaderCode = Standard.Unlit.FragmentShader(layout.attributes)
        val shaderCode = shader {
            vertex(vertexShaderCode.vertex)
            fragment(fragmentShaderCode.fragment)
        }
        val shader = Shader(device, shaderCode)

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
