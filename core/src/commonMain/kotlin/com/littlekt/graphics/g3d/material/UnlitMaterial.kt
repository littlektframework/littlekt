package com.littlekt.graphics.g3d.material

import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 11/29/2024
 */
open class UnlitMaterial(
    val baseColorFactor: Color = Color.WHITE,
    val baseColorTexture: Texture? = null,
    val transparent: Boolean = false,
    val doubleSided: Boolean = false,
    val alphaCutoff: Float = 0f,
    val castShadows: Boolean = true,
) : Material() {
    protected lateinit var paramBuffer: GPUBuffer

    lateinit var bindGroupLayout: BindGroupLayout
        protected set

    lateinit var bindGroup: BindGroup
        protected set

    open fun upload(device: Device) {
        val paramBuffer =
            device.createGPUFloatBuffer(
                "param buffer",
                floatArrayOf(
                    baseColorFactor.r,
                    baseColorFactor.g,
                    baseColorFactor.b,
                    baseColorFactor.a,
                    alphaCutoff,
                    // padding
                    0f,
                    0f,
                    0f,
                ),
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
            )

        val bgLayoutEntries =
            mutableListOf(BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, BufferBindingLayout()))
        val bgEntries = mutableListOf(BindGroupEntry(0, BufferBinding(paramBuffer)))

        baseColorTexture?.let { baseColorTexture ->
            bgLayoutEntries += BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout())
            bgLayoutEntries += BindGroupLayoutEntry(2, ShaderStage.FRAGMENT, TextureBindingLayout())
            bgEntries += BindGroupEntry(1, baseColorTexture.sampler)
            bgEntries += BindGroupEntry(2, baseColorTexture.view)
        }

        val bindGroupLayout =
            device.createBindGroupLayout(BindGroupLayoutDescriptor(bgLayoutEntries))
        val bindGroup = device.createBindGroup(BindGroupDescriptor(bindGroupLayout, bgEntries))

        this.paramBuffer = paramBuffer
        this.bindGroupLayout = bindGroupLayout
        this.bindGroup = bindGroup
    }

    override fun release() {
        paramBuffer.release()
        bindGroupLayout.release()
        bindGroup.release()
    }
}
