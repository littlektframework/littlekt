package com.littlekt.graphics.postprocess

import com.littlekt.Releasable
import com.littlekt.graphics.Color
import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.webgpu.*

/**
 * Runs a chain of [effects] over a rendered image, such as the scene drawn into a
 * [TextureFormat.RGBA16_FLOAT] render target. Each enabled effect reads the result of the one
 * before it. Every effect but the last draws into one of two textures this owns, and the last draws
 * into a render pass of yours, such as the one for the screen.
 *
 * Usage each frame:
 * ```
 * bloom.render(commandEncoder, hdrTarget.view, width, height)
 * postProcessor.process(commandEncoder, hdrTarget.view, width, height)
 * val pass = commandEncoder.beginRenderPass(RenderPassDescriptor(colorAttachment(frame)))
 * postProcessor.render(pass, graphics.preferredFormat)
 * pass.end()
 * ```
 *
 * Effects work in linear color. Put a [com.littlekt.graphics.Tonemapper] first to map HDR colors,
 * followed by effects that expect a `[0, 1]` range, such as [FxaaEffect]. The effects aren't
 * released with this.
 *
 * @param format the format of the textures between effects. A float format keeps HDR colors.
 * @author Colton Daily
 * @date 11/21/2026
 */
class PostProcessor(
    val device: Device,
    val format: TextureFormat = TextureFormat.RGBA16_FLOAT,
) : Releasable {

    /** The effects to run, in order. */
    val effects = mutableListOf<PostEffect>()

    private val copy =
        ShaderPostEffect(
            device,
            "fn effect(uv: vec2f) -> vec4f { return textureSample(source, source_sampler, uv); }",
            label = "post processor copy",
        )

    private val targets = arrayOfNulls<EmptyTexture>(2)
    private val passes = arrayOfNulls<RenderPassDescriptor>(2)
    private val passViews = arrayOfNulls<TextureView>(2)
    private var targetWidth = 0
    private var targetHeight = 0

    private var finalEffect: PostEffect? = null
    private var finalSource: TextureView? = null
    private var finalWidth = 0
    private var finalHeight = 0

    /**
     * Runs every enabled effect but the last over [source]. Call [render] after with the render
     * pass to draw the last effect into.
     *
     * @param source the image to process, in linear color, that can be sampled
     * @param width the width of [source] in pixels
     * @param height the height of [source] in pixels
     */
    fun process(commandEncoder: CommandEncoder, source: TextureView, width: Int, height: Int) {
        var last = -1
        for (i in effects.indices) {
            if (effects[i].enabled) last = i
        }
        var current = source
        var ping = 0
        for (i in 0 until last) {
            val effect = effects[i]
            if (!effect.enabled) continue
            ensureTargets(width, height)
            val pass = commandEncoder.beginRenderPass(passDescriptor(ping))
            effect.render(pass, current, format, width, height)
            pass.end()
            pass.release()
            current = targets[ping]!!.view
            ping = ping xor 1
        }
        finalEffect = if (last >= 0) effects[last] else copy
        finalSource = current
        finalWidth = width
        finalHeight = height
    }

    /**
     * Draws the last enabled effect, or the processed image if there are no effects, over the whole
     * of [renderPassEncoder]. Must be called after [process] each frame.
     *
     * @param format the format of the render pass target
     */
    fun render(renderPassEncoder: RenderPassEncoder, format: TextureFormat) {
        val effect = checkNotNull(finalEffect) { "process must be called before render!" }
        effect.render(renderPassEncoder, finalSource!!, format, finalWidth, finalHeight)
    }

    private fun ensureTargets(width: Int, height: Int) {
        if (targets[0] != null && targetWidth == width && targetHeight == height) return
        targetWidth = width
        targetHeight = height
        for (i in targets.indices) {
            val target = targets[i]
            if (target == null) {
                targets[i] =
                    EmptyTexture(
                        device,
                        format,
                        width,
                        height,
                        usage = TextureUsage.TEXTURE or TextureUsage.RENDER_ATTACHMENT,
                    )
            } else {
                target.resize(width, height)
            }
        }
    }

    private fun passDescriptor(index: Int): RenderPassDescriptor {
        val view = targets[index]!!.view
        val current = passes[index]
        if (current != null && passViews[index] === view) return current
        passViews[index] = view
        return RenderPassDescriptor(
                listOf(
                    RenderPassColorAttachmentDescriptor(
                        view = view,
                        loadOp = LoadOp.CLEAR,
                        storeOp = StoreOp.STORE,
                        clearColor = Color.CLEAR,
                    )
                ),
                label = "post processor pass",
            )
            .also { passes[index] = it }
    }

    override fun release() {
        for (i in targets.indices) {
            targets[i]?.release()
            targets[i] = null
            passes[i] = null
            passViews[i] = null
        }
        finalEffect = null
        finalSource = null
        copy.release()
    }
}
