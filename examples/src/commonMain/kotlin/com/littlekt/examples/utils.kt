package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.graphics.webgpu.SurfaceConfiguration
import com.littlekt.graphics.webgpu.SurfaceTexture
import com.littlekt.graphics.webgpu.TextureStatus
import com.littlekt.input.InputProcessor
import com.littlekt.input.Key
import com.littlekt.util.milliseconds

fun SurfaceTexture.isValid(context: Context, onConfigure: () -> SurfaceConfiguration): Boolean {
    val surfaceTexture = this
    when (val status = surfaceTexture.status) {
        TextureStatus.SUCCESS -> {
            // all good, could check for `surfaceTexture.suboptimal` here.
        }
        TextureStatus.TIMEOUT,
        TextureStatus.OUTDATED,
        TextureStatus.LOST -> {
            surfaceTexture.texture?.release()
            context.graphics.surface.configure(onConfigure())
            context.logger.info { "getCurrentTexture status=$status" }
            return false
        }
        else -> {
            // fatal
            context.logger.fatal { "getCurrentTexture status=$status" }
            context.close()
            return false
        }
    }
    return true
}

fun Context.addStatsHandler() {
    onPostUpdate {
        if (input.isKeyJustPressed(Key.P)) {
            println(stats)
        }
    }
}

fun Context.addCloseOnEsc() {
    onPostUpdate {
        if (input.isKeyJustPressed(Key.ESCAPE)) {
            close()
        }
    }
}

fun Context.addWASDMovement(camera: Camera, speed: Float) {
    onUpdate { dt ->
        val sprint = input.isKeyPressed(Key.SHIFT_LEFT) || input.isKeyPressed(Key.SHIFT_RIGHT)
        val slow = input.isKeyPressed(Key.CTRL_LEFT) || input.isKeyPressed(Key.CTRL_RIGHT)
        var ultimateSpeed = speed
        if (sprint) ultimateSpeed *= 3f
        if (slow) ultimateSpeed *= 0.25f
        if (input.isKeyPressed(Key.W)) {
            camera.position.y += ultimateSpeed * dt.milliseconds
        }
        if (input.isKeyPressed(Key.S)) {
            camera.position.y -= ultimateSpeed * dt.milliseconds
        }
        if (input.isKeyPressed(Key.D)) {
            camera.position.x += ultimateSpeed * dt.milliseconds
        }
        if (input.isKeyPressed(Key.A)) {
            camera.position.x -= ultimateSpeed * dt.milliseconds
        }
    }
}

fun Context.addZoom(camera: Camera, speed: Float) {
    onUpdate { dt ->
        input.addInputProcessor(
            object : InputProcessor {
                override fun scrolled(amountX: Float, amountY: Float): Boolean {
                    camera.translate(0f, 0f, -amountY * speed * dt.milliseconds)
                    return true
                }
            }
        )
    }
}
