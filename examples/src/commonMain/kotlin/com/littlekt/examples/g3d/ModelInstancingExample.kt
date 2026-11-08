package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.async.KtScope
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.file.gltf.GltfLoaderUnlitConfig
import com.littlekt.file.vfs.readGltfModel
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.UnlitEnvironment
import com.littlekt.graphics.g3d.util.UnlitMaterialPipelineProvider
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.math.random
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.surfaceColor
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.seconds
import kotlinx.coroutines.launch

/**
 * An example of instancing a glTF model. A grid of thousands of randomly rotated and colored copies
 * of an unlit duck share the same geometry and are drawn as instances by the [ModelBatch].
 *
 * @author Colton Daily
 * @date 4/13/2024
 */
class ModelInstancingExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.translate(45f, 69f, 60f)
        camera.rotate((-45).degrees, Vec3f.X_AXIS)
        camera.rotate((30).degrees, Vec3f.Y_AXIS)
        val environment = UnlitEnvironment(device)

        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)
        val duckModel =
            resourcesVfs["models/Duck.glb"].readGltfModel(config = GltfLoaderUnlitConfig())
        val duckModelInstances = mutableListOf<Node3D>()

        KtScope.launch {
            repeat(15) { y ->
                repeat(15) { x ->
                    repeat(15) { z ->
                        duckModelInstances +=
                            duckModel.copy().apply {
                                rotate(
                                    (0..360).random().degrees,
                                    (0..360).random().degrees,
                                    (0..360).random().degrees,
                                )
                                translate(x * 3f, y * 3f, z * 3f)
                                color(
                                    Color(
                                        (0.1f..1f).random(),
                                        (0.1f..1f).random(),
                                        (0.1f..1f).random(),
                                    )
                                )
                            }
                    }
                }
            }
        }

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

        addFlyController(camera)
        onUpdate { dt ->
            duckModel.rotate(y = 0.1.degrees * dt.seconds)
            duckModel.update(dt)
            duckModelInstances.fastForEach { instance ->
                instance.rotate(
                    x = 10f.degrees * dt.seconds,
                    y = 10f.degrees * dt.seconds,
                    z = 10f.degrees * dt.seconds,
                )
                instance.update(dt)
            }
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

                modelBatch.render(duckModel, camera, environment)
                duckModelInstances.fastForEach { modelBatch.render(it, camera, environment) }
                modelBatch.flush(renderPassEncoder, camera, dt)
                renderPassEncoder.end()
                renderPassEncoder.release()
            }
        }

        onRelease {
            duckModel.release()
            modelBatch.release()
            environment.release()
        }
    }
}
