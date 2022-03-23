package com.lehaine.littlekt.graph.node.render

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.shader.ShaderProgram

/**
 * @author Colton Daily
 * @date 3/23/2022
 */
open class Material(val shader: ShaderProgram<*, *>? = null) : Disposable {
    var blendMode: BlendMode = BlendMode.NonPreMultiplied
    var depthStencilMode: DepthStencilMode = DepthStencilMode.None

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