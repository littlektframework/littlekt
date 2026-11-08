package com.littlekt.examples.ui

import com.littlekt.examples.*
import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readLDtkMapLoader
import com.littlekt.graph.node.ui.centerContainer
import com.littlekt.graph.node.ui.label
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.viewport.ExtendViewport

/**
 * An example using a render pass for the game world and a render pass for the UI, each with its
 * own viewport. Move the world camera with WASD.
 *
 * @author Colton Daily
 * @date 5/2/2024
 */
class GameWorldAndUiViewportsExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val mapLoader = resourcesVfs["ldtk/world-1.5.3.ldtk"].readLDtkMapLoader()
        val world = mapLoader.loadMap(0, translateMapHeight = false)

        val batch = SpriteBatch(graphics.device, graphics, graphics.preferredFormat, size = 400)
        val worldViewport = ExtendViewport(270, 135)
        val worldCamera = worldViewport.camera
        addWASDMovement(worldCamera, 0.05f)
        val bgColor = graphics.surfaceColor(world.defaultLevelBackgroundColor)
        val graph =
            sceneGraph(this, viewport = ExtendViewport(960, 540), batch = batch) {
                    centerContainer {
                        anchorRight = 1f
                        anchorTop = 1f
                        label { text = "This is my UI!" }
                    }
                }
                .also { it.initialize() }

        graph.requestShowDebugInfo = true

        onResize { width, height ->
            graph.resize(width, height)
            worldViewport.update(width, height)
        }

        onUpdate { dt ->
            renderFrame { frame, commandEncoder ->
                val worldRenderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            listOf(colorAttachment(frame, bgColor)),
                            label = "World render pass",
                        )
                    )
                worldCamera.update()
                batch.begin(worldCamera.viewProjection)
                world.render(batch, worldCamera, scale = 1f)
                batch.flush(worldRenderPass)
                worldRenderPass.end()
                worldRenderPass.release()

                graph.update(dt)
                graph.render(
                    commandEncoder,
                    RenderPassDescriptor(listOf(colorAttachment(frame)), label = "UI render pass"),
                )
                batch.end()
            }
        }

        onRelease {
            graph.release()
            batch.release()
            mapLoader.release()
        }
    }
}
