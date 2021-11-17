package com.lehaine.littlekt.graphics.state

import com.lehaine.littlekt.graphics.render.Color

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
class BlendState(
    var name: String = "",
    var colorSourceBlend: Blend = Blend.ONE,
    var alphaSourceBlend: Blend = Blend.ONE,
    var colorDestinationBlend: Blend = Blend.ZERO,
    var alphaDestinationBlend: Blend = Blend.ZERO
) {

    companion object {
        val ADDITIVE = BlendState("BlendState.ADDITIVE", Blend.SOURCE_ALPHA, Blend.SOURCE_ALPHA, Blend.ONE, Blend.ONE)
        val ALPHA_BLEND = BlendState(
            "BlendState.ALPHA_BLEND",
            Blend.ONE,
            Blend.ONE,
            Blend.INVERSE_SOURCE_ALPHA,
            Blend.INVERSE_SOURCE_ALPHA
        )
        val NON_PREMULTIPLIED = BlendState(
            "BlendState.NON_PREMULTIPLIED",
            Blend.SOURCE_ALPHA,
            Blend.SOURCE_ALPHA,
            Blend.INVERSE_SOURCE_ALPHA,
            Blend.INVERSE_SOURCE_ALPHA
        )
        val OPAQUE = BlendState("BlendState.OPAQUE", Blend.ONE, Blend.ONE, Blend.ZERO, Blend.ZERO)
    }

    var alpha = 0f
    var alphaBlendFunction = BlendFunction.ADD
    var colorBlendFunction = BlendFunction.ADD
    var blendFactor: Color = Color.WHITE
    var multiSampleMask = -1


}