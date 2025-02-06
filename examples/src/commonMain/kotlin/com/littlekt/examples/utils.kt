package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.async.VfsScope
import com.littlekt.file.gltf.GltfLoaderConfig
import com.littlekt.file.vfs.VfsFile
import com.littlekt.file.vfs.readGltfModel
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g3d.Environment
import com.littlekt.graphics.g3d.Model
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.webgpu.SurfaceConfiguration
import com.littlekt.graphics.webgpu.SurfaceTexture
import com.littlekt.graphics.webgpu.TextureStatus
import com.littlekt.input.InputProcessor
import com.littlekt.input.Key
import com.littlekt.input.Pointer
import com.littlekt.math.MutableQuaternion
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.math.geom.radians
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.milliseconds
import kotlin.math.asin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

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
        if (!input.cursorLocked && input.isKeyJustPressed(Key.ESCAPE)) {
            close()
        }
    }
}

data class GltfModel(
    val file: VfsFile,
    val config: GltfLoaderConfig,
    val environment: Environment,
    val scale: Float,
    val translate: Vec3f,
    val animIdx: Int = -1,
) {
    var scene: Model? = null
}

fun ModelBatch.renderGltfModels(models: List<GltfModel>, camera: Camera) {
    models.fastForEach { model -> model.scene?.let { render(it, camera, model.environment) } }
}

fun loadGltfModels(models: List<GltfModel>, modelBatch: ModelBatch? = null) {
    VfsScope.launch {
        models
            .map { gltfModel ->
                VfsScope.async {
                    val scene =
                        gltfModel.file.readGltfModel(gltfModel.config).apply {
                            scale(gltfModel.scale)
                            translate(gltfModel.translate)
                            enableAnimation(gltfModel.animIdx)
                        }
                    modelBatch?.preparePipeline(scene, gltfModel.environment)
                    gltfModel.scene = scene
                }
            }
            .awaitAll()
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

fun Context.addFlyController(camera: Camera, speed: Float = 0.0015f) {
    val forward = MutableVec3f()
    val right = MutableVec3f()
    val up = MutableVec3f()

    onPostUpdate {
        if (!input.cursorLocked && input.isJustTouched(Pointer.POINTER1)) {
            input.lockCursor()
        }

        if (input.cursorLocked && input.isKeyJustPressed(Key.ESCAPE)) {
            input.releaseCursor()
        }
    }

    val quat = MutableQuaternion()

    onUpdate { dt ->
        if (!input.cursorLocked) return@onUpdate

        val sprint = input.isKeyPressed(Key.SHIFT_LEFT) || input.isKeyPressed(Key.SHIFT_RIGHT)
        val slow = input.isKeyPressed(Key.CTRL_LEFT) || input.isKeyPressed(Key.CTRL_RIGHT)
        var ultimateSpeed = speed
        if (sprint) ultimateSpeed *= 3f
        if (slow) ultimateSpeed *= 0.25f

        forward.set(camera.direction).norm().scale(ultimateSpeed * dt.milliseconds)
        right.set(camera.right).norm().scale(ultimateSpeed * dt.milliseconds)
        up.set(camera.up).norm().scale(ultimateSpeed * dt.milliseconds)

        val dx = -input.deltaX * 0.5f
        val dy = -input.deltaY * 0.5f

        val currentPitch = asin(camera.direction.y).radians
        val newPitch = (currentPitch + dy.degrees).coerceIn((-89).degrees, 89.degrees)
        val dp = newPitch - currentPitch

        quat.identity()
        quat.rotate(dx.degrees, Vec3f.UP)
        quat.rotate(dp, camera.right)
        camera.rotate(quat)

        if (input.isKeyPressed(Key.W)) {
            camera.position += forward
        }
        if (input.isKeyPressed(Key.S)) {
            camera.position -= forward
        }
        if (input.isKeyPressed(Key.D)) {
            camera.position += right
        }
        if (input.isKeyPressed(Key.A)) {
            camera.position -= right
        }
        if (input.isKeyPressed(Key.Q)) {
            camera.position -= up
        }
        if (input.isKeyPressed(Key.E)) {
            camera.position += up
        }

        camera.update()
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
