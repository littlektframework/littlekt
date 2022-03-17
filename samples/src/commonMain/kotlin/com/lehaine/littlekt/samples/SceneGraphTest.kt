package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.node
import com.lehaine.littlekt.graph.node.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class SceneGraphTest(context: Context) : ContextListener(context) {

    class TestNode : Node() {
        var updated = false
        override fun ready() {
            println("$name ready")
        }

        override fun onAddedToScene() {
            println("$name onAddedToScene")
        }

        override fun update(dt: Duration) {
            if (!updated) {
                println("$name update")
                updated = true
            }
        }
    }

    override suspend fun Context.start() {
        val graph = sceneGraph(context, ExtendViewport(480, 270)) {
            node(TestNode()) {
                name = "TestRoot"
                node(TestNode()) {
                    name = "TestChild1"
                    node(TestNode()) {
                        name = "TestChild3"
                    }
                }
                node(TestNode()) {
                    name = "TestChild2"
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