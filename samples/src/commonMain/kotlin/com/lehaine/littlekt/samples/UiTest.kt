package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graph.node.node2d.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class UiTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val graph = sceneGraph(context, ExtendViewport(480, 270)) {
            paddedContainer {
                anchorRight = 1f
                anchorBottom = 1f
                padding(5)

                hBoxContainer {
                    vBoxContainer {
                        name = "Player Info"
                        separation = 5
                        horizontalSizeFlags = Control.SizeFlag.FILL or Control.SizeFlag.EXPAND


                        label {
                            text = "top left"
                        }
                    }

                    vBoxContainer {
                        name = "Zone Info"
                        separation = 5

                        label {
                            text = "Emerald Forest"
                        }

                        label {
                            text = "Levels 1-10"
                        }
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