package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graph.node.node2d.ui.button
import com.lehaine.littlekt.graph.node.node2d.ui.centerContainer
import com.lehaine.littlekt.graph.node.node2d.ui.label
import com.lehaine.littlekt.graph.node.node2d.ui.vBoxContainer
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.viewport.FitViewport

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class UiTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val graph = sceneGraph(context, FitViewport(480, 270)) {
            centerContainer {
                anchorBottom = 1f
                anchorRight = 1f
                vBoxContainer {
                    separation = 20
                    label {
                        text = "Select a Sample:"
                    }

                    vBoxContainer {
                        separation = 10
                        button {
                            text = "Platformer - Collect all the Diamonds!"
                        }
                        button {
                            text = "Another!!"
                        }
                    }

                    button {
                        text = "Exit"
                        onPressed += {
                            context.close()
                        }
                    }
                }
            }
        }.also { it.initialize() }

        onResize { width, height ->
            graph.resize(width, height)
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