package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.graphics.state.*
import com.lehaine.littlekt.shader.ShaderProgram

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
class Material(val shader: ShaderProgram? = null) {

    companion object {
        val DEFAULT_MATERIAL = Material()

        val STENCIL_WRITE
            get() = Material().apply {
                depthStencilState = DepthStencilState().apply {
                    stencilEnable = true
                    stencilFunction = CompareFunction.ALWAYS
                    stencilPass = StencilOperation.REPLACE
                    referenceStencil = 1
                    depthBufferEnable = false
                }
            }

        val STENCIL_READ
            get() = Material().apply {
                depthStencilState = DepthStencilState().apply {
                    stencilEnable = true
                    stencilFunction = CompareFunction.EQUAL
                    stencilPass = StencilOperation.KEEP
                    referenceStencil = 1
                    depthBufferEnable = false
                }
            }

        val BLEND_DARKEN
            get() = Material().apply {
                blendState = BlendState().apply {
                    colorSourceBlend = Blend.ONE
                    colorDestinationBlend = Blend.ONE
                    colorBlendFunction = BlendFunction.MIN
                    alphaSourceBlend = Blend.ONE
                    alphaDestinationBlend = Blend.ONE
                    alphaBlendFunction = BlendFunction.MIN
                }
            }

        val BLEND_LIGHTEN
            get() = Material().apply {
                blendState = BlendState().apply {
                    colorSourceBlend = Blend.ONE
                    colorDestinationBlend = Blend.ONE
                    colorBlendFunction = BlendFunction.MAX
                    alphaSourceBlend = Blend.ONE
                    alphaDestinationBlend = Blend.ONE
                    alphaBlendFunction = BlendFunction.MAX
                }
            }

        val BLEND_SCREEN
            get() = Material().apply {
                blendState = BlendState().apply {
                    colorSourceBlend = Blend.INVERSE_DESTINATION_COLOR
                    colorDestinationBlend = Blend.ONE
                    colorBlendFunction = BlendFunction.ADD
                }
            }

        val BLEND_MULTIPLY
            get() = Material().apply {
                blendState = BlendState().apply {
                    colorSourceBlend = Blend.DESTINATION_COLOR
                    colorDestinationBlend = Blend.ZERO
                    colorBlendFunction = BlendFunction.ADD
                    alphaSourceBlend = Blend.DESTINATION_ALPHA
                    alphaDestinationBlend = Blend.ZERO
                    alphaBlendFunction = BlendFunction.ADD
                }
            }

        val BLEND_MULTIPLY_2X
            get() = Material().apply {
                blendState = BlendState().apply {
                    colorSourceBlend = Blend.DESTINATION_COLOR
                    colorDestinationBlend = Blend.SOURCE_COLOR
                    colorBlendFunction = BlendFunction.ADD
                }
            }

        val BLEND_LINEAR_DODGE
            get() = Material().apply {
                blendState = BlendState().apply {
                    colorSourceBlend = Blend.ONE
                    colorDestinationBlend = Blend.ONE
                    colorBlendFunction = BlendFunction.ADD
                }
            }

        val BLEND_LINEAR_BURN
            get() = Material().apply {
                blendState = BlendState().apply {
                    colorSourceBlend = Blend.ONE
                    colorDestinationBlend = Blend.ONE
                    colorBlendFunction = BlendFunction.REVERSE_SUBTRACT
                }
            }

        val BLEND_DIFFERENCE
            get() = Material().apply {
                blendState = BlendState().apply {
                    colorSourceBlend = Blend.INVERSE_DESTINATION_COLOR
                    colorDestinationBlend = Blend.INVERSE_SOURCE_COLOR
                    colorBlendFunction = BlendFunction.ADD
                }
            }

        val BLEND_SUBTRACTIVE
            get() = Material().apply {
                blendState = BlendState().apply {
                    colorSourceBlend = Blend.SOURCE_ALPHA
                    colorDestinationBlend = Blend.ONE
                    colorBlendFunction = BlendFunction.REVERSE_SUBTRACT
                    alphaSourceBlend = Blend.SOURCE_ALPHA
                    alphaDestinationBlend = Blend.ONE
                    alphaBlendFunction = BlendFunction.REVERSE_SUBTRACT
                }
            }

        val BLEND_ADDITIVE
            get() = Material().apply {
                blendState = BlendState().apply {
                    colorSourceBlend = Blend.SOURCE_ALPHA
                    colorDestinationBlend = Blend.ONE
                    alphaSourceBlend = Blend.SOURCE_ALPHA
                    alphaDestinationBlend = Blend.ONE
                }
            }
    }

    var blendState = BlendState.ALPHA_BLEND
    var depthStencilState = DepthStencilState.NONE

    /**
     * Called when the [Material] is initially set before [Batch.begin] to allow
     * any [Shader]s that have parameters set if necessary based on the [Camera] matrix such as to set the
     * the MatrixTransform to [Camera.combined] mimicking what [Batch] does. This will only be called if
     * there is a non-null [Shader].
     */
  //  open fun onPreRender(camera: Camera) {}



}