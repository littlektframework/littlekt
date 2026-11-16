package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addFlyController
import com.littlekt.examples.addStatsHandler
import com.littlekt.file.gltf.GltfLoaderUnlitConfig
import com.littlekt.file.gltf.GltfModelConfig
import com.littlekt.file.vfs.readGltfModel
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.font.TextureFontCache
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.UnlitEnvironment
import com.littlekt.graphics.g3d.skin.Animation
import com.littlekt.graphics.g3d.skin.AnimationEvent
import com.littlekt.graphics.g3d.util.UnlitMaterialPipelineProvider
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.resources.Fonts
import com.littlekt.surfaceColor
import com.littlekt.util.viewport.ScreenViewport
import kotlin.time.Duration.Companion.milliseconds

/**
 * An example of playing the clips of a skinned glTF model. Keys `1`, `2`, and `3` crossfade between
 * the looping survey, walk, and run clips of the fox, `4` plays the survey clip once and fades back,
 * and the walk and run clips fire footstep events.
 *
 * @author Colton Daily
 * @date 11/16/2026
 */
class SkinnedAnimationExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val preferredFormat = graphics.preferredFormat
        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(1f, 0.8f, 1.8f)
        camera.lookAt(0f, 0.4f, 0f)
        val environment = UnlitEnvironment(device)
        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)
        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(UnlitMaterialPipelineProvider())
                colorFormat = preferredFormat
                depthFormat = depthTarget.format
            }
        val fox =
            resourcesVfs["models/Fox.glb"]
                .readGltfModel(GltfLoaderUnlitConfig(GltfModelConfig(skinned = true)))
                .apply { scale(0.01f) }
        modelBatch.preparePipeline(fox, environment)

        val batch = SpriteBatch(device, graphics, preferredFormat)
        val viewport = ScreenViewport(graphics.width, graphics.height)
        viewport.update(graphics.width, graphics.height, true)
        val text = TextureFontCache(Fonts.default)
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        fox.defaultFade = 300.milliseconds
        val survey = fox.findAnimation("Survey")!!
        val walk = fox.findAnimation("Walk")!!
        val run = fox.findAnimation("Run")!!
        var looping: Animation = walk
        var lastEvent = "-"
        var steps = 0

        fun updateText() {
            text.setText(
                "1 survey, 2 walk, 3 run, 4 survey once\n" +
                    "playing: ${fox.currentAnimation?.name}\n" +
                    "last event: $lastEvent ($steps)",
                16f,
                viewport.virtualHeight - 32f,
            )
        }

        walk.addEvent(walk.duration * 0.25f, "left step")
        walk.addEvent(walk.duration * 0.75f, "right step")
        run.addEvent(run.duration * 0.2f, "left step")
        run.addEvent(run.duration * 0.7f, "right step")
        val onStep: (AnimationEvent) -> Unit = {
            lastEvent = it.name
            steps++
            updateText()
        }
        walk.onEvent = onStep
        run.onEvent = onStep
        survey.onFinished = {
            fox.play(looping)
            updateText()
        }

        fox.play(walk, fade = 0.milliseconds)
        updateText()

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
            viewport.update(width, height, true)
            updateText()
        }

        addFlyController(camera, 0.0015f)
        onUpdate { dt ->
            when {
                input.isKeyJustPressed(Key.NUM1) -> looping = fox.play(survey).also { updateText() }
                input.isKeyJustPressed(Key.NUM2) -> looping = fox.play(walk).also { updateText() }
                input.isKeyJustPressed(Key.NUM3) -> looping = fox.play(run).also { updateText() }
                input.isKeyJustPressed(Key.NUM4) -> {
                    fox.play(survey, loop = false, restart = true)
                    updateText()
                }
            }
            fox.update(dt)

            renderFrame("Main Command Encoder") { frame, commandEncoder ->
                camera.update()
                val pass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(frame, clearColor),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Main Render Pass",
                        )
                    )
                modelBatch.render(fox, environment)
                modelBatch.flush(pass, camera, dt)
                pass.end()
                pass.release()

                val uiPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(frame),
                            label = "UI Render Pass",
                        )
                    )
                viewport.camera.update()
                batch.begin(viewport.camera.viewProjection)
                text.draw(batch)
                batch.flush(uiPass)
                batch.end()
                uiPass.end()
                uiPass.release()
            }
        }

        onRelease {
            fox.release()
            modelBatch.release()
            environment.release()
            batch.release()
        }
    }
}
