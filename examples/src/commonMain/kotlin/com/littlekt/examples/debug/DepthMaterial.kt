package com.littlekt.examples.debug

import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.g3d.material.UnlitMaterial
import com.littlekt.graphics.g3d.shader.blocks.Standard
import com.littlekt.graphics.g3d.util.BaseMaterialPipelineProvider
import com.littlekt.graphics.g3d.util.MaterialPipeline
import com.littlekt.graphics.g3d.util.RenderOrder
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.shader.builder.shader
import com.littlekt.graphics.webgpu.*
import com.littlekt.resources.Textures
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 2/11/2025
 */
class DepthMaterialPipelineProvider : BaseMaterialPipelineProvider<DepthMaterial>() {
    override val type: KClass<DepthMaterial> = DepthMaterial::class

    override fun createMaterialPipeline(
        device: Device,
        environment: Environment,
        layout: VertexBufferLayout,
        topology: PrimitiveTopology,
        material: DepthMaterial,
        colorFormat: TextureFormat,
        depthFormat: TextureFormat,
    ): MaterialPipeline {
        val vertexShaderCode =
            if (material.skinned) Standard.SkinnedVertexShader(layout.attributes)
            else Standard.VertexShader(layout.attributes)
        val fragmentShaderCode = shader {
            val inputStruct = Standard.VertexOutputStruct(layout.attributes)
            include(inputStruct)
            include(Standard.Unlit.FragmentOutputStruct)
            fragment {
                main(input = inputStruct, Standard.Unlit.FragmentOutputStruct) {
                    """
                        var out: FragmentOutput;
                        let depth: f32 = input.position.z / input.position.w;

                        let gray: f32 = clamp(depth, 0.0, 1.0);
                        out.color = vec4f(gray, gray, gray, 1.0);
                        return out;
                    """
                        .trimIndent()
                }
            }
        }

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

class DepthMaterial(device: Device) : UnlitMaterial(device, Textures.textureWhite)
