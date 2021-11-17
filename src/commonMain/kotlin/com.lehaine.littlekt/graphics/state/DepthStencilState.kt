package com.lehaine.littlekt.graphics.state

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
class DepthStencilState(
    var name: String = "",
    var depthBufferEnable: Boolean = true,
    var depthBufferWriteEnable: Boolean = true
) {

    companion object {
        val DEFAULT = DepthStencilState(
            "DepthStencilState.DEFAULT",
            depthBufferEnable = true,
            depthBufferWriteEnable = true
        )

        val DEPTH_READ = DepthStencilState(
            "DepthStencilState.DEPTH_READ",
            depthBufferEnable = true,
            depthBufferWriteEnable = false
        )

        val NONE = DepthStencilState(
            "DepthStencilState.NONE",
            depthBufferEnable = false,
            depthBufferWriteEnable = false
        )
    }

    var depthBufferFunction = CompareFunction.LESS_EQUAL
    var stencilEnable = false
    var stencilFunction = CompareFunction.ALWAYS
    var stencilPass = StencilOperation.KEEP
    var stencilFail = StencilOperation.KEEP
    var stencilDepthBufferFail = StencilOperation.KEEP
    var twoSidedStencilMode = false
    var counterClockwiseStencilFunction = CompareFunction.ALWAYS
    var counterClockwiseStencilFail = StencilOperation.KEEP
    var counterClockwiseStencilPass = StencilOperation.KEEP
    var counterClockwiseStencilDepthBufferFail = StencilOperation.KEEP
    var stencilMask = Int.MAX_VALUE
    var stencilWriteMask = Int.MAX_VALUE
    var referenceStencil = 0

}