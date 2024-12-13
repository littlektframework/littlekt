package com.littlekt.graphics.g3d.material

import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.CompareFunction

/**
 * @author Colton Daily
 * @date 11/29/2024
 */
open class UnlitMaterial(
    val baseColorTexture: Texture,
    val baseColorFactor: Color = Color.WHITE,
    val transparent: Boolean = false,
    val doubleSided: Boolean = false,
    val alphaCutoff: Float = 0f,
    val castShadows: Boolean = true,
    val depthWrite: Boolean = true,
    val depthCompareFunction: CompareFunction = CompareFunction.LESS,
) : Material() {

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
