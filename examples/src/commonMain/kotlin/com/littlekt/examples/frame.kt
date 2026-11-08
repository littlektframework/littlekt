package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.Graphics
import com.littlekt.Releasable
import com.littlekt.graphics.Color
import com.littlekt.graphics.webgpu.*

/** Configures the surface and configures it again whenever the window is resized. */
fun Context.configureSurfaceOnResize() {
    graphics.configureSurface()
    onResize { _, _ -> graphics.configureSurface() }
}

/**
 * Gets the current surface texture and, if it's ready, runs [block] with a view of it and a new
 * [CommandEncoder]. The commands are then submitted, the surface is presented, and everything is
 * released. Skips the frame if the surface texture isn't ready.
 */
inline fun Context.renderFrame(
    label: String? = null,
    block: (frame: TextureView, commandEncoder: CommandEncoder) -> Unit,
) {
    val surfaceTexture = graphics.surface.getCurrentTexture()
    when (val status = surfaceTexture.status) {
        TextureStatus.SUCCESS -> Unit
        TextureStatus.TIMEOUT -> {
            surfaceTexture.texture?.release()
            return
        }
        TextureStatus.OUTDATED,
        TextureStatus.LOST -> {
            surfaceTexture.texture?.release()
            graphics.configureSurface()
            logger.info { "getCurrentTexture status=$status" }
            return
        }
        else -> {
            logger.fatal { "getCurrentTexture status=$status" }
            close()
            return
        }
    }
    val texture = checkNotNull(surfaceTexture.texture)
    val frame = texture.createView()
    val commandEncoder = graphics.device.createCommandEncoder(label)
    block(frame, commandEncoder)
    val commandBuffer = commandEncoder.finish()
    graphics.device.queue.submit(commandBuffer)
    graphics.surface.present()
    commandBuffer.release()
    commandEncoder.release()
    frame.release()
    texture.release()
}

/** @return the [color] as linear when the surface is sRGB, so it shows up as the given [color]. */
fun Graphics.surfaceColor(color: Color): Color = if (preferredFormat.srgb) color.toLinear() else color

/** @return a color attachment that clears to [clearColor], or loads if `null`. */
fun colorAttachment(
    view: TextureView,
    clearColor: Color? = null,
    resolveTarget: TextureView? = null,
) =
    RenderPassColorAttachmentDescriptor(
        view = view,
        loadOp = if (clearColor != null) LoadOp.CLEAR else LoadOp.LOAD,
        storeOp = StoreOp.STORE,
        clearColor = clearColor,
        resolveTarget = resolveTarget,
    )

/**
 * A texture to render to that is the size of the window. Use [Context.renderTarget] to resize it
 * with the window automatically.
 */
class RenderTarget(
    val device: Device,
    val format: TextureFormat,
    width: Int,
    height: Int,
    val sampleCount: Int = 1,
    val usage: TextureUsage = TextureUsage.RENDER_ATTACHMENT,
) : Releasable {
    var width: Int = width
        private set

    var height: Int = height
        private set

    var texture: WebGPUTexture = create()
        private set

    var view: TextureView = texture.createView()
        private set

    fun resize(width: Int, height: Int) {
        if (this.width == width && this.height == height) return
        this.width = width
        this.height = height
        view.release()
        texture.release()
        texture = create()
        view = texture.createView()
    }

    /** @return a depth attachment of this target that clears the depth to `1`. */
    fun depthAttachment(): RenderPassDepthStencilAttachmentDescriptor {
        val stencil = format == TextureFormat.DEPTH24_PLUS_STENCIL8
        return RenderPassDepthStencilAttachmentDescriptor(
            view = view,
            depthClearValue = 1f,
            depthLoadOp = LoadOp.CLEAR,
            depthStoreOp = StoreOp.STORE,
            stencilLoadOp = if (stencil) LoadOp.CLEAR else null,
            stencilStoreOp = if (stencil) StoreOp.STORE else null,
        )
    }

    private fun create() =
        device.createTexture(
            TextureDescriptor(
                Extent3D(width, height, 1),
                1,
                sampleCount,
                TextureDimension.D2,
                format,
                usage,
            )
        )

    override fun release() {
        view.release()
        texture.release()
    }
}

/** @return a [RenderTarget] the size of the window that resizes and releases with the context. */
fun Context.renderTarget(
    format: TextureFormat,
    sampleCount: Int = 1,
    usage: TextureUsage = TextureUsage.RENDER_ATTACHMENT,
): RenderTarget {
    val target =
        RenderTarget(graphics.device, format, graphics.width, graphics.height, sampleCount, usage)
    onResize { width, height -> target.resize(width, height) }
    onRelease { target.release() }
    return target
}
