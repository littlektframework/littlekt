package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.file.gltf.GltfLoaderUnlitConfig
import com.littlekt.file.gltf.GltfModelConfig
import com.littlekt.file.vfs.readGltfModel
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.UnlitEnvironment
import com.littlekt.graphics.g3d.skin.BakedAnimationInstance
import com.littlekt.graphics.g3d.skin.BakedAnimations
import com.littlekt.graphics.g3d.util.UnlitMaterialPipelineProvider
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.math.random
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.surfaceColor

/**
 * An example of drawing a horde of animated, skinned models. The fox's animations are baked once
 * with [BakedAnimations] and every fox is a [BakedAnimationInstance] with its own clip, time and
 * speed, so all of them are drawn in one instanced draw call. Press up & down to add or remove
 * foxes.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
class SkinnedInstancingExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.far = 500f
        camera.translate(0f, 10f, 30f)
        camera.rotate((-20).degrees, Vec3f.X_AXIS)
        val environment = UnlitEnvironment(device)
        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)

        val fox =
            resourcesVfs["models/Fox.glb"].readGltfModel(
                GltfLoaderUnlitConfig(GltfModelConfig(skinned = true))
            )
        val baked = BakedAnimations(device, fox)
        val horde = Node3D()

        fun spawn(count: Int) {
            repeat(count) {
                val index = horde.childCount
                val x = index % COLUMNS - COLUMNS / 2
                val z = index / COLUMNS - 20
                val fox =
                    baked.createInstance().apply {
                        play(baked.clips.random(), loop = true)
                        time = (0f..clip!!.duration).random()
                        speed = (0.8f..1.2f).random()
                        scaling(0.02f)
                        rotate(y = (0..360).random().degrees)
                        position(x * 2.5f, 0f, z * 2.5f)
                        tint = Color((0.6f..1f).random(), (0.6f..1f).random(), (0.6f..1f).random())
                    }
                horde.addChild(fox)
            }
        }

        fun despawn(count: Int) {
            repeat(minOf(count, horde.childCount)) {
                val fox = horde.children.last() as BakedAnimationInstance
                fox.remove()
                horde.removeChild(fox)
            }
        }

        spawn(1000)

        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(UnlitMaterialPipelineProvider())
                colorFormat = graphics.preferredFormat
                depthFormat = depthTarget.format
            }
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
        }

        addFlyController(camera, 0.0015f)
        onUpdate { dt ->
            if (input.isKeyJustPressed(Key.ARROW_UP)) spawn(500)
            if (input.isKeyJustPressed(Key.ARROW_DOWN)) despawn(500)
            horde.update(dt)
        }
        onUpdate { dt ->
            renderFrame { frame, commandEncoder ->
                camera.update()
                val renderPassEncoder =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(frame, clearColor),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                        )
                    )
                modelBatch.render(horde, camera, environment)
                modelBatch.flush(renderPassEncoder, camera, dt)
                renderPassEncoder.end()
                renderPassEncoder.release()
            }
        }

        onRelease {
            baked.release()
            fox.release()
            modelBatch.release()
            environment.release()
        }
    }

    private companion object {
        const val COLUMNS = 40
    }
}
