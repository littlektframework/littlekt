package com.littlekt.graph.node.render

import com.littlekt.Releasable
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.BlendState

/**
 * @author Colton Daily
 * @date 3/23/2022
 */
open class Material(
    /** The [Shader] that this material will use for rendering */
    val shader: Shader? = null,
) : Releasable {
    /** The [BlendState] this material uses. Defaults to [BlendState.NonPreMultiplied]. */
    var blendMode: BlendState = BlendState.NonPreMultiplied

    //    /** The [DepthStencilMode] this material uses. Defaults to [DepthStencilMode.None]. */
    //    var depthStencilMode: DepthStencilState = DepthStencilState.None

    /** Can be used to set shader uniforms and such right before rendering. */
    open fun onPreRender() = Unit

    override fun release() {
        shader?.release()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Material

        if (shader != other.shader) return false
        if (blendMode != other.blendMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shader?.hashCode() ?: 0
        result = 31 * result + blendMode.hashCode()
        return result
    }
}
