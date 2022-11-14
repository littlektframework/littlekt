package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.node
import com.lehaine.littlekt.graph.node.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Textures
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.random
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

    var testControl = Control()

    fun SceneGraph<*>.init(): Node {
        return node(TestNode()) {
            name = "TestRoot"


            testControl = control {
                anchorRight = 1f
                anchorBottom = 1f

                button {
                    x = (50..100).random()
                    y = (50..100).random()
                    text = "Press Me!"
                }
            }
        }
    }

    override suspend fun Context.start() {
        val graph = sceneGraph(context, ExtendViewport(480, 270))
        var testRoot = graph.init()
        graph.initialize()
        graph.root.apply {
            node {

            }
            node {}
            node {}
            node {}
        }

        onResize { width, height ->
            graph.resize(width, height, true)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            graph.update(dt)
            graph.render()

            if (input.isKeyPressed(Key.SHIFT_LEFT) && input.isKeyJustPressed(Key.ENTER)) {
                testRoot.removeChildAt(2)
                testRoot.addChildAt(Label().apply { text = "Newly added!" }, 0)
            } else if (input.isKeyJustPressed(Key.ENTER)) {
                testControl.apply {
                    textureRect {
                        anchorRight = 1f
                        anchorBottom = 1f
                        stretchMode = TextureRect.StretchMode.SCALE
                        slice = Textures.white
                    }
                }
            }

            if (input.isKeyJustPressed(Key.F)) {
                testRoot.sendChildAtToBottom(0)
            }

            if (input.isKeyJustPressed(Key.B)) {
                testRoot.sendChildAtToTop(2)
            }

            if (input.isKeyJustPressed(Key.M)) {
                testRoot.swapChildrenAt(0, 2)
            }

            if (input.isKeyJustPressed(Key.R)) {
                println("graph before destroy")
                println(graph.root.treeString())
                graph.root.destroyAllChildren()
                println("graph after destroy")
                println(graph.root.treeString())
                testRoot = graph.init()
                println("graph after init")
                println(graph.root.treeString())
            }

            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.T)) {
                println(graph.root.treeString())
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}