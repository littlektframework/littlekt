package com.lehaine.littlekt.graph.node.render

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.util.BlendMode
import com.lehaine.littlekt.graphics.util.DepthStencilMode

/**
 * @author Colton Daily
 * @date 3/23/2022
 */
open class Material(
    /**
     * The [ShaderProgram] that this material will use for rendering
     */
    val shader: ShaderProgram<*, *>? = null,
) : Disposable {
    /**
     * The [BlendMode] this material uses. Defaults to [BlendMode.NonPreMultiplied].
     */
    var blendMode: BlendMode = BlendMode.NonPreMultiplied

    /**
     * The [DepthStencilMode] this material uses. Defaults to [DepthStencilMode.None].
     */
    var depthStencilMode: DepthStencilMode = DepthStencilMode.None

    /**
     * Can be used to set shader uniforms and such right before rendering.
     */
    open fun onPreRender() = Unit

    override fun dispose() {
        shader?.dispose()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Material

        if (shader != other.shader) return false
        if (blendMode != other.blendMode) return false
        if (depthStencilMode != other.depthStencilMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shader?.hashCode() ?: 0
        result = 31 * result + blendMode.hashCode()
        result = 31 * result + depthStencilMode.hashCode()
        return result
    }
}