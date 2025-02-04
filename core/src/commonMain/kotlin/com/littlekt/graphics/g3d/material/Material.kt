package com.littlekt.graphics.g3d.material

import com.littlekt.Releasable
import com.littlekt.graphics.Color
import com.littlekt.graphics.Texture
import com.littlekt.graphics.TextureState
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.BindGroup
import com.littlekt.graphics.webgpu.CompareFunction
import com.littlekt.util.UniqueId

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
abstract class Material : Releasable {
    val id: Int = UniqueId.next<Material>()

    abstract val baseColorTexture: Texture
    abstract val baseColorFactor: Color
    abstract val transparent: Boolean
    abstract val doubleSided: Boolean
    abstract val alphaCutoff: Float
    abstract val castShadows: Boolean
    abstract val depthWrite: Boolean
    abstract val depthCompareFunction: CompareFunction
    abstract val skinned: Boolean

    /**
     * If `true` then the material is ready for rendering. This defaults to the [baseColorTexture]
     * being loaded.
     */
    open val ready: Boolean
        get() = baseColorTexture.state == TextureState.LOADED

    /**
     * They material render state key, used for pipeline caching. This should only add things
     * related to rendering, such as transparency, shadows, alpha cutoff, culling, etc. Do not add
     * textures and such otherwise it won't cache.
     */
    open val key: Int
        get() = run {
            var result = transparent.hashCode()
            result = 31 * result + doubleSided.hashCode()
            result = 31 * result + alphaCutoff.hashCode()
            result = 31 * result + castShadows.hashCode()
            result = 31 * result + depthWrite.hashCode()
            result = 31 * result + depthCompareFunction.hashCode()
            result = 31 * result + skinned.hashCode()
            result
        }

    abstract fun createBindGroup(shader: Shader): BindGroup

    abstract fun update()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Material

        return key == other.key
    }

    override fun hashCode(): Int {
        return key
    }
}
