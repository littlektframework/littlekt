package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.graphics.Textures
import com.lehaine.littlekt.input.GameAxis
import com.lehaine.littlekt.input.InputMultiplexer
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.math.geom.minus
import com.lehaine.littlekt.math.geom.plus
import com.lehaine.littlekt.util.viewport.ExtendViewport
import scene.node.node
import scene.node.node2d.Node2D
import scene.sceneGraph
import kotlin.time.Duration


/**
 * @author Colton Daily
 * @date 1/1/2022
 */
class SceneGraphTest(context: Context) : Game<Scene>(context) {

    val sceneGraph by prepare {
        sceneGraph(context, ExtendViewport(480, 270)) {
            node {
                name = "Test Node"
            }
            node(TextureNode(Textures.white)) {
                x = 150f
                y = 125f
                rotation = 45.degrees
                onUpdate += {
                    val velocity = controller.vector(GameInput.MOVEMENT)

                    translate(velocity.x * 2f, velocity.y * 2f)

                    if (input.isKeyPressed(Key.X)) {
                        scaleX += 0.1f
                        scaleY += 0.1f
                    }

                    if (input.isKeyPressed(Key.Z)) {
                        scaleX -= 0.1f
                        scaleY -= 0.1f
                    }

                    if (input.isKeyPressed(Key.E)) {
                        rotation += 1.degrees
                    }

                    if (input.isKeyPressed(Key.Q)) {
                        rotation -= 1.degrees
                    }
                }
                //   scale = Vec2f(5f, 5f)
                node(TextureNode(Textures.red)) {
                    x = 5f
                    y = 5f

                }
            }
        }
    }

    enum class GameInput {
        MOVE_LEFT,
        MOVE_RIGHT,
        MOVE_UP,
        MOVE_DOWN,
        HORIZONTAL,
        VERTICAL,
        MOVEMENT,
    }

    private val controller = InputMultiplexer<GameInput>(input)

    init {
        input.addInputProcessor(controller)
        controller.addBinding(
            GameInput.MOVE_LEFT,
            listOf(Key.A, Key.ARROW_LEFT),
            axes = listOf(GameAxis.LX)
        )
        controller.addBinding(
            GameInput.MOVE_RIGHT,
            listOf(Key.D, Key.ARROW_RIGHT),
            axes = listOf(GameAxis.LX)
        )
        controller.addBinding(GameInput.MOVE_UP, listOf(Key.W, Key.ARROW_UP), axes = listOf(GameAxis.LY))
        controller.addBinding(
            GameInput.MOVE_DOWN,
            listOf(Key.S, Key.ARROW_DOWN),
            axes = listOf(GameAxis.LY)
        )
        controller.addAxis(
            GameInput.HORIZONTAL,
            GameInput.MOVE_RIGHT,
            GameInput.MOVE_LEFT
        )
        controller.addAxis(
            GameInput.VERTICAL,
            GameInput.MOVE_DOWN,
            GameInput.MOVE_UP
        )
        controller.addVector(
            GameInput.MOVEMENT,
            GameInput.MOVE_RIGHT,
            GameInput.MOVE_DOWN,
            GameInput.MOVE_LEFT,
            GameInput.MOVE_UP
        )
    }

    override fun create() {
        sceneGraph.initialize()
        val node = sceneGraph.root.children[1].children[0] as Node2D
        println(node.globalPosition)
        println(node.localToGlobalTransform)
        logger.info { "\n" + sceneGraph.root.treeString() }
    }

    override fun update(dt: Duration) {
        sceneGraph.update(dt)
        sceneGraph.render()
    }

    override fun dispose() {
        sceneGraph.dispose()
    }
}

private class TextureNode(val slice: TextureSlice) : Node2D() {

    override fun render(batch: SpriteBatch, camera: Camera) {
        batch.draw(
            slice,
            globalX,
            globalY,
            scaleX = globalScaleX,
            scaleY = globalScaleY,
            rotation = globalRotation
        )
    }
}