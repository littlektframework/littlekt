package com.lehaine.littlekt.graph.node.render

import com.lehaine.littlekt.graphics.gl.CompareFunction
import com.lehaine.littlekt.graphics.gl.StencilAction

/**
 * @author Colton Daily
 * @date 3/23/2022
 */
open class DepthStencilMode(
    val depthBufferEnable: Boolean = true,
    val depthBufferWriteEnable: Boolean = true,
    val depthBufferFunction: CompareFunction = CompareFunction.LEQUAL,
    val stencilEnable: Boolean = false,
    val stencilFunction: CompareFunction = CompareFunction.ALWAYS,
    val stencilPass: StencilAction = StencilAction.KEEP,
    val stencilFail: StencilAction = StencilAction.KEEP,
    val stencilDepthBufferFail: StencilAction = StencilAction.KEEP,
    val twoSidedStencilMode: Boolean = false,
    val counterClockwiseStencilFunction: CompareFunction = CompareFunction.ALWAYS,
    val counterClockwiseStencilFail: StencilAction = StencilAction.KEEP,
    val counterClockwiseStencilPass: StencilAction = StencilAction.KEEP,
    val counterClockwiseStencilDepthBufferFail: StencilAction = StencilAction.KEEP,
    val stencilMask: Int = Int.MAX_VALUE,
    val stencilWriteMask: Int = Int.MAX_VALUE,
    val referenceStencil: Int = 0,
) {

    object Default : DepthStencilMode(
        depthBufferEnable = true,
        depthBufferWriteEnable = true)

    object DepthRead : DepthStencilMode(
        depthBufferEnable = true,
        depthBufferWriteEnable = false)

    object None : DepthStencilMode(
        depthBufferEnable = false,
        depthBufferWriteEnable = false
    )

    object StencilWrite : DepthStencilMode(
        stencilEnable = true,
        stencilFunction = CompareFunction.ALWAYS,
        stencilPass = StencilAction.REPLACE,
        referenceStencil = 1,
        depthBufferEnable = false)


    object StencilRead : DepthStencilMode(
        stencilEnable = true,
        stencilFunction = CompareFunction.EQUAL,
        stencilPass = StencilAction.KEEP,
        referenceStencil = 1,
        depthBufferEnable = false)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DepthStencilMode

        if (depthBufferEnable != other.depthBufferEnable) return false
        if (depthBufferWriteEnable != other.depthBufferWriteEnable) return false
        if (depthBufferFunction != other.depthBufferFunction) return false
        if (stencilEnable != other.stencilEnable) return false
        if (stencilFunction != other.stencilFunction) return false
        if (stencilPass != other.stencilPass) return false
        if (stencilFail != other.stencilFail) return false
        if (stencilDepthBufferFail != other.stencilDepthBufferFail) return false
        if (twoSidedStencilMode != other.twoSidedStencilMode) return false
        if (counterClockwiseStencilFunction != other.counterClockwiseStencilFunction) return false
        if (counterClockwiseStencilFail != other.counterClockwiseStencilFail) return false
        if (counterClockwiseStencilPass != other.counterClockwiseStencilPass) return false
        if (counterClockwiseStencilDepthBufferFail != other.counterClockwiseStencilDepthBufferFail) return false
        if (stencilMask != other.stencilMask) return false
        if (stencilWriteMask != other.stencilWriteMask) return false
        if (referenceStencil != other.referenceStencil) return false

        return true
    }

    override fun hashCode(): Int {
        var result = depthBufferEnable.hashCode()
        result = 31 * result + depthBufferWriteEnable.hashCode()
        result = 31 * result + depthBufferFunction.hashCode()
        result = 31 * result + stencilEnable.hashCode()
        result = 31 * result + stencilFunction.hashCode()
        result = 31 * result + stencilPass.hashCode()
        result = 31 * result + stencilFail.hashCode()
        result = 31 * result + stencilDepthBufferFail.hashCode()
        result = 31 * result + twoSidedStencilMode.hashCode()
        result = 31 * result + counterClockwiseStencilFunction.hashCode()
        result = 31 * result + counterClockwiseStencilFail.hashCode()
        result = 31 * result + counterClockwiseStencilPass.hashCode()
        result = 31 * result + counterClockwiseStencilDepthBufferFail.hashCode()
        result = 31 * result + stencilMask
        result = 31 * result + stencilWriteMask
        result = 31 * result + referenceStencil
        return result
    }


}