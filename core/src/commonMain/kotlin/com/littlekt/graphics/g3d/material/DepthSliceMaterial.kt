package com.littlekt.graphics.g3d.material

import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.*
import com.littlekt.resources.Textures

/**
 * @author Colton Daily
 * @date 2/5/2025
 */
class DepthSliceMaterial(val device: Device) : Material() {
    override val baseColorTexture: Texture = Textures.textureWhite
    override val baseColorFactor: Color = Color.WHITE
    override val transparent: Boolean = false
    override val doubleSided: Boolean = false
    override val alphaCutoff: Float = 0f
    override val castShadows: Boolean = true
    override val depthWrite: Boolean = true
    override val depthCompareFunction: CompareFunction = CompareFunction.LESS
    override val skinned: Boolean = false

    override fun createBindGroup(shader: Shader): BindGroup? = null

    override fun update() = Unit

    override fun release() = Unit

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UnlitMaterial

        if (baseColorTexture != other.baseColorTexture) return false
        if (baseColorFactor != other.baseColorFactor) return false
        if (transparent != other.transparent) return false
        if (doubleSided != other.doubleSided) return false
        if (alphaCutoff != other.alphaCutoff) return false
        if (castShadows != other.castShadows) return false
        if (depthWrite != other.depthWrite) return false
        if (depthCompareFunction != other.depthCompareFunction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = baseColorTexture.hashCode()
        result = 31 * result + baseColorFactor.hashCode()
        result = 31 * result + transparent.hashCode()
        result = 31 * result + doubleSided.hashCode()
        result = 31 * result + alphaCutoff.hashCode()
        result = 31 * result + castShadows.hashCode()
        result = 31 * result + depthWrite.hashCode()
        result = 31 * result + depthCompareFunction.hashCode()
        return result
    }
}
