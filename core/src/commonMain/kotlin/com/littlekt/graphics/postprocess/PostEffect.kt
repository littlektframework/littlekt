package com.littlekt.graphics.postprocess

import com.littlekt.Releasable
import com.littlekt.graphics.webgpu.RenderPassEncoder
import com.littlekt.graphics.webgpu.TextureFormat
import com.littlekt.graphics.webgpu.TextureView

/**
 * A fullscreen pass of a [PostProcessor] that reads the image so far and draws a new one. Extend
 * [ShaderPostEffect] to write one in WGSL.
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
interface PostEffect : Releasable {
    /** Whether the [PostProcessor] runs this effect. A disabled effect is skipped. */
    var enabled: Boolean

    /**
     * Draws this effect of [source] over the whole of [renderPassEncoder].
     *
     * @param source the image so far, in linear color
     * @param format the format of the render pass target
     * @param width the width of [source] in pixels
     * @param height the height of [source] in pixels
     */
    fun render(
        renderPassEncoder: RenderPassEncoder,
        source: TextureView,
        format: TextureFormat,
        width: Int,
        height: Int,
    )
}
