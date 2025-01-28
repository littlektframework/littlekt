package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.graphics.Camera
import com.littlekt.input.Key
import com.littlekt.util.milliseconds
import io.ygdrasil.webgpu.SurfaceTexture
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.SurfaceTextureStatus

fun SurfaceTexture.isValid(context: Context, onConfigure: () -> SurfaceConfiguration): Boolean {
    val surfaceTexture = this
    when (val status = surfaceTexture.status) {
        SurfaceTextureStatus.success -> {
            // all good, could check for `surfaceTexture.suboptimal` here.
        }
        SurfaceTextureStatus.timeout,
        SurfaceTextureStatus.outdated,
        SurfaceTextureStatus.lost -> {
            surfaceTexture.texture.close()
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
