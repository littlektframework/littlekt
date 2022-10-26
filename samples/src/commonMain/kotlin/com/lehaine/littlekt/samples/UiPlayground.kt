package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 10/26/2022
 */
class UiPlayground(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val graph = sceneGraph(context, ExtendViewport(960, 540)) {
            control {
                anchorRight = 1f
                anchorBottom = 1f

                paddedContainer {
                    padding(10)

                    vBoxContainer {
                        separation = 20

                        vBoxContainer {
                            separation = 10
                            label { text = "Labels:" }
                            label {
                                text = "Different colored label"
                                horizontalAlign = HAlign.CENTER
                                fontColor = Color.RED
                            }
                            label {
                                text = "Really long text that has ellipsis to continue"
                                ellipsis = "..."
                                width = 150f
                            }
                        }

                        vBoxContainer {
                            separation = 10
                            label { text = "Line Edits (Text Fields):" }
                            label { text = "Normal" }
                            lineEdit {
                                text = "you can see this"
                                minWidth = 150f
                            }

                            label { text = "Secret text" }
                            lineEdit {
                                secret = true
                                text = "you can't see this"
                                minWidth = 150f
                            }
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

            if (input.isKeyJustPressed(Key.ENTER)) {
                graph.showDebugInfo = !graph.showDebugInfo
            }

            if (input.isKeyJustPressed(Key.T)) {
                logger.info { "\n" + graph.root.treeString() }
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