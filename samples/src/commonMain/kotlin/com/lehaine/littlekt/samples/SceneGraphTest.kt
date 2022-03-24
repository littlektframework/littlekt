package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.component.InputEvent
import com.lehaine.littlekt.graph.node.node
import com.lehaine.littlekt.graph.node.ui.button
import com.lehaine.littlekt.graph.sceneGraph
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

    fun SceneGraph<*>.init() {
        node(TestNode()) {
            name = "TestRoot"
            node(TestNode()) {
                name = "TestChild1"
                onInput += {
                    if (it.type == InputEvent.Type.ACTION_DOWN && it.inputType == "ui_accept") {
                        println("$name consumed 'ui_accept' action")
                        it.handle()
                    }
                }
                node(TestNode()) {
                    name = "TestChild3"
                    onInput += {
                        if (it.type == InputEvent.Type.KEY_DOWN && it.key == Key.ENTER) {
                            println("$name consumed ${it.key} pressed down")
                            it.handle()
                        }
                    }
                    onUnhandledInput += {
                        if (it.type == InputEvent.Type.KEY_DOWN) {
                            println("$name received ${it.key} down as unhandled input")
                            it.handle()
                        }
                        if (it.type == InputEvent.Type.KEY_UP) {
                            println("$name consumed ${it.key} up as unhandled input")
                            it.handle()
                        }
                    }
                }
            }
            node(TestNode()) {
                name = "TestChild2"
                onUnhandledInput += {
                    if (it.type == InputEvent.Type.TOUCH_DOWN) {
                        println("$name consumed ${it.pointer} down as unhandled input")
                        it.handle()
                    }
                }
            }

            button {
                x = (50..100).random()
                y = (50..100).random()
                text = "Press Me!"
            }
        }
    }

    override suspend fun Context.start() {
        val graph = sceneGraph(context, ExtendViewport(480, 270)) {
            init()
        }.also { it.initialize() }

        onResize { width, height ->
            graph.resize(width, height, true)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            graph.update(dt)
            graph.render()

            if (input.isKeyJustPressed(Key.R)) {
                println("graph before destroy")
                println(graph.root.treeString())
                graph.destroyRoot()
                println("graph after destroy")
                println(graph.root.treeString())
                graph.init()
                println("graph after init")
                println(graph.root.treeString())
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