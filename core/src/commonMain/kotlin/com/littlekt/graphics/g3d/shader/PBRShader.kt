package com.littlekt.graphics.g3d.shader

import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.g3d.util.shader.*
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Vec3f
import com.littlekt.resources.Textures

/**
 * @author Colton Daily
 * @date 12/31/2024
 */
open class PBRShader(
    device: Device,
    layout: List<VertexAttribute>,
    baseColorTexture: Texture = Textures.textureWhite,
    baseColorFactor: Color = Color.WHITE,
    val metallicFactor: Float = 1f,
    val roughnessFactor: Float = 1f,
    metallicRoughnessTexture: Texture? = null,
    normalTexture: Texture? = null,
    val emissiveFactor: Vec3f = Vec3f(0f),
    emissiveTexture: Texture? = null,
    occlusionTexture: Texture? = null,
    val occlusionStrength: Float = 1f,
    transparent: Boolean = false,
    doubleSided: Boolean = false,
    alphaCutoff: Float = 0f,
    castShadows: Boolean = true,
    depthWrite: Boolean = true,
    depthCompareFunction: CompareFunction = CompareFunction.LESS,
    vertexEntryPoint: String = "vs_main",
    fragmentEntryPoint: String = "fs_main",
    vertexSrc: String = buildCommonShader {
        vertex {
            vertexInput(layout)
            vertexOutput(layout)
            camera(0, 0)
            model(1, 0)
            main(layout, vertexEntryPoint)
        }
    },
    fragmentSrc: String = buildCommonShader {
        fragment {
            unlit {
                material(2)
                main()
            }
        }
    },
    bindGroupLayout: List<BindGroupLayoutDescriptor> =
        listOf(
            BindGroupLayoutDescriptor(
                listOf(
                    // camera
                    BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout())
                )
            ),
            BindGroupLayoutDescriptor(
                listOf(
                    // model
                    BindGroupLayoutEntry(0, ShaderStage.VERTEX, BufferBindingLayout())
                )
            ),
            BindGroupLayoutDescriptor(
                listOf(
                    // material uniform
                    BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, BufferBindingLayout()),
                    // baseColorTexture
                    BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    // baseColorSampler
                    BindGroupLayoutEntry(2, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    // normal texture
                    BindGroupLayoutEntry(3, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    // normal sampler
                    BindGroupLayoutEntry(4, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    // metallic roughness texture
                    BindGroupLayoutEntry(5, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    // metallic roughness sampler
                    BindGroupLayoutEntry(6, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    // occlusion texture
                    BindGroupLayoutEntry(7, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    // occlusion sampler
                    BindGroupLayoutEntry(8, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    // emissive texture
                    BindGroupLayoutEntry(9, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    // emissive sampler
                    BindGroupLayoutEntry(10, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                )
            ),
        ),
) :
    UnlitShader(
        device,
        layout,
        baseColorTexture,
        baseColorFactor,
        transparent,
        doubleSided,
        alphaCutoff,
        castShadows,
        depthWrite,
        depthCompareFunction,
        vertexEntryPoint,
        fragmentEntryPoint,
        vertexSrc,
        fragmentSrc,
        bindGroupLayout,
    ) {
    private val isFullyRough: Boolean = roughnessFactor == 1f && metallicRoughnessTexture == null
    val metallicRoughnessTexture: Texture = metallicRoughnessTexture ?: Textures.textureWhite
    val normalTexture: Texture = normalTexture ?: Textures.textureWhite
    val emissiveTexture: Texture = emissiveTexture ?: Textures.textureWhite
    val occlusionTexture: Texture = occlusionTexture ?: Textures.textureWhite

    override val key: Int = 31 * super.key + isFullyRough.hashCode()

    override val materialUniformBuffer: GPUBuffer =
        device.createGPUFloatBuffer(
            "material buffer",
            floatArrayOf(
                baseColorFactor.r,
                baseColorFactor.g,
                baseColorFactor.b,
                baseColorFactor.a,
                metallicFactor,
                roughnessFactor,
                occlusionStrength,
                emissiveFactor.x,
                emissiveFactor.y,
                emissiveFactor.z,
                alphaCutoff,
                // padding
                0f,
            ),
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )

    override fun MutableList<BindGroup>.createBindGroupsInternal(data: Map<String, Any>) {
        add(
            device.createBindGroup(
                BindGroupDescriptor(
                    layouts[0],
                    listOf(BindGroupEntry(0, cameraUniformBufferBinding)),
                )
            )
        )
        add(
            device.createBindGroup(
                BindGroupDescriptor(
                    layouts[1],
                    listOf(BindGroupEntry(0, modelUniformBufferBinding)),
                )
            )
        )
        add(
            device.createBindGroup(
                BindGroupDescriptor(
                    layouts[2],
                    listOf(
                        BindGroupEntry(0, materialUniformBufferBinding),
                        BindGroupEntry(1, baseColorTexture.view),
                        BindGroupEntry(2, baseColorTexture.sampler),
                        BindGroupEntry(3, normalTexture.view),
                        BindGroupEntry(4, normalTexture.sampler),
                        BindGroupEntry(5, metallicRoughnessTexture.view),
                        BindGroupEntry(6, metallicRoughnessTexture.sampler),
                        BindGroupEntry(7, occlusionTexture.view),
                        BindGroupEntry(8, occlusionTexture.sampler),
                        BindGroupEntry(9, emissiveTexture.view),
                        BindGroupEntry(10, emissiveTexture.sampler),
                    ),
                )
            )
        )
    }

    companion object {
        const val VIEW_PROJECTION = "viewProjection"
        const val MODEL = "model"
    }
}
