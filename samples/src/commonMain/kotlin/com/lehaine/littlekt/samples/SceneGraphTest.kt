package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.util.viewport.ExtendViewport
import scene.node.Node
import scene.node.node
import scene.sceneGraph
import kotlin.time.Duration


/**
 * @author Colton Daily
 * @date 1/1/2022
 */
class SceneGraphTest(context: Context) : Game<Scene>(context) {


    val texture by load<Texture>(resourcesVfs["atlas.png"])
    val slices: Array<Array<TextureSlice>> by prepare { texture.slice(16, 16) }
    val person by prepare { slices[0][0] }

    val sceneGraph by prepare {
        sceneGraph(context, ExtendViewport(480, 270)) {
            node {
                name = "Test Node"
            }
            node(TextureNode(person)) {
                x = 150f
                y = 125f
            }
        }
    }

    override fun create() {
        sceneGraph.initialize()
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

private class TextureNode(val slice: TextureSlice) : Node() {
    var x = 0f
    var y = 0f

    override fun render(batch: SpriteBatch, camera: Camera) {
        batch.draw(slice, x, y)
    }
}