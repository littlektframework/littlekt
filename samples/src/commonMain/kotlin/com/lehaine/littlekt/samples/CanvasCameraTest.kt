package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graph.node.canvasLayer
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.node2d.camera2d
import com.lehaine.littlekt.graph.node.node2d.node2d
import com.lehaine.littlekt.graph.node.node2d.ui.centerContainer
import com.lehaine.littlekt.graph.node.node2d.ui.label
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class CanvasCameraTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val pixelFont = resourcesVfs["m5x7_16.fnt"].readBitmapFont()
        val icon = resourcesVfs["icon_16x16.png"].readTexture()
        val graph = sceneGraph(context, ExtendViewport(480, 270)) {
            node2d {
                onUpdate += {
                    if (input.isKeyPressed(Key.D)) {
                        globalX += 10f
                    } else if (input.isKeyPressed(Key.A)) {
                        globalX -= 10f
                    }
                }
                onRender += { batch, camera ->
                    batch.draw(icon, globalX, globalY)
                }
                camera2d {
                    x = 50f
                    active = true
                }
            }

            node2d {
                x = 100f
                y = 20f
                onRender += { batch, camera ->
                    batch.draw(icon, globalX, globalY)
                }
            }
            canvasLayer {
                centerContainer {
                    anchorRight = 1f
                    anchorBottom = 1f
                    label {
                        text = "Should be centered label"
                        horizontalAlign = HAlign.CENTER
                        font = pixelFont
                    }
                }
            }
        }.also { it.initialize() }

        onResize { width, height ->
            graph.resize(width, height, true)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            graph.update(dt)
            graph.render()

            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}