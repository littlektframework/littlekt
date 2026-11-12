package com.littlekt

import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.webgpu.CommandEncoder
import com.littlekt.graphics.webgpu.TextureStatus
import com.littlekt.graphics.webgpu.TextureFormat
import com.littlekt.graphics.webgpu.TextureUsage
import com.littlekt.graphics.webgpu.TextureView

/** Configures the surface and configures it again whenever the window is resized. */
fun Context.configureSurfaceOnResize() {
    graphics.configureSurface()
    onResize { _, _ -> graphics.configureSurface() }
}

/**
 * Gets the current surface texture and, if it's ready, runs [block] with a view of it and a new
 * [CommandEncoder]. The commands are then submitted, the surface is presented, and everything is
 * released. The frame is skipped if the surface texture isn't ready and the surface is configured
 * again if it's outdated or lost.
 *
 * ```
 * onUpdate {
 *     renderFrame { frame, commandEncoder ->
 *         val renderPass = commandEncoder.beginRenderPass(RenderPassDescriptor(colorAttachment(frame, clearColor)))
 *         batch.flush(renderPass, camera.viewProjection)
 *         renderPass.end()
 *         renderPass.release()
 *     }
 * }
 * ```
 */
inline fun Context.renderFrame(
    label: String? = null,
    block: (frame: TextureView, commandEncoder: CommandEncoder) -> Unit,
) {
    if (graphics.backBufferWidth <= 0 || graphics.backBufferHeight <= 0) return
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

/**
 * @return an [EmptyTexture] the size of the window to render to, that resizes and releases with
 *   this context.
 */
fun Context.renderTarget(
    format: TextureFormat,
    sampleCount: Int = 1,
    usage: TextureUsage = TextureUsage.TEXTURE or TextureUsage.RENDER_ATTACHMENT,
): EmptyTexture {
    val target =
        EmptyTexture(
            graphics.device,
            format,
            graphics.width,
            graphics.height,
            sampleCount = sampleCount,
            usage = usage,
        )
    onResize { width, height -> target.resize(width, height) }
    onRelease { target.release() }
    return target
}
