package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Fonts
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.slice
import com.lehaine.littlekt.graphics.use
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class UiTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val batch = SpriteBatch(this)
        val pixelFont = resourcesVfs["m5x7_16.fnt"].readBitmapFont()
        val icon = resourcesVfs["icon_16x16.png"].readTexture()
        val viewport = ExtendViewport(480, 270)
        val camera = viewport.camera

        val graph = sceneGraph(context, ExtendViewport(480, 270)) {
            centerContainer {
                anchorRight = 1f
                anchorBottom = 1f

                vBoxContainer {
                    separation = 5

                    label {
                        text = "A"
                        horizontalAlign = HAlign.CENTER
                        font = pixelFont
                    }
                    label {
                        text = "My Label Middle"
                        horizontalAlign = HAlign.CENTER
                        font = pixelFont
                    }
                    textureRect {
                        slice = icon.slice()
                        stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                    }

                }
            }

            vScrollBar {
                x = 50f
                y = 50f
            }
        }.also { it.initialize() }

        onResize { width, height ->
            viewport.update(width, height, this)
            graph.resize(width, height, true)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            camera.update()
            viewport.apply(this)
            batch.use(camera.viewProjection) {
                Fonts.default.draw(it, "My Label\nMiddle", 0f, 0f, targetWidth = 480f, align = HAlign.CENTER)
            }

            graph.update(dt)
            graph.render()

            if (input.isKeyJustPressed(Key.ENTER)) {
                graph.showDebugInfo = !graph.showDebugInfo
            }

            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}