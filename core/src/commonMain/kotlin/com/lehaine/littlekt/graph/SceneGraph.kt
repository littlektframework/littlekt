package com.lehaine.littlekt.graph

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.internal.InternalResources
import com.lehaine.littlekt.graphics.use
import com.lehaine.littlekt.input.InputProcessor
import com.lehaine.littlekt.util.viewport.ScreenViewport
import com.lehaine.littlekt.util.viewport.Viewport
import kotlin.time.Duration

/**
 * Create a new scene graph with a [callback] with the [SceneGraph] in context.
 * @param context the current context
 * @param viewport the viewport that the camera of the scene graph will own
 * @param batch an option sprite batch. If omitted, the scene graph will create and manage its own.
 * @param callback the callback that is invoked with a [SceneGraph] context
 * in order to initialize any values and create nodes
 * @return the newly created [SceneGraph]
 */
inline fun sceneGraph(
    context: Context,
    viewport: Viewport = ScreenViewport(context.graphics.width, context.graphics.height),
    batch: SpriteBatch? = null,
    callback: @SceneGraphDslMarker SceneGraph.() -> Unit = {}
) = SceneGraph(context, viewport, batch).also(callback)

/**
 * A class for creating a scene graph of nodes.
 * @param context the current context
 * @param viewport the viewport that the camera of the scene graph will own
 * @param batch an option sprite batch. If omitted, the scene graph will create and manage its own.
 * @author Colton Daily
 * @date 1/1/2022
 */
open class SceneGraph(
    val context: Context,
    val viewport: Viewport = ScreenViewport(context.graphics.width, context.graphics.height),
    batch: SpriteBatch? = null,
) : InputProcessor, Disposable {

    private var ownsBatch = true
    val batch: SpriteBatch = batch?.also { ownsBatch = false } ?: SpriteBatch(context)
    private val gpuFontBatch = SpriteBatch(context, size = 100).apply {
        shader = InternalResources.INSTANCE.gpuFontShader
    }

    val root: Node by lazy {
        Node().apply {
            scene = this@SceneGraph
            this.viewport = this@SceneGraph.viewport
        }
    }

    val width: Int get() = viewport.virtualWidth
    val height: Int get() = viewport.virtualHeight

    private val camera = OrthographicCamera(context.graphics.width, context.graphics.height).apply {
        this.viewport = this@SceneGraph.viewport
    }

    private var frameCount = 0

    fun initialize() {
        context.input.addInputProcessor(this)
        root.initialize()
        onStart()
        root._onPostEnterScene()
    }

    fun render() {
        gpuFontBatch.use(camera.viewProjection) {
            batch.use(camera.viewProjection) {
                root._render(batch, gpuFontBatch, camera)
            }
        }
    }


    /**
     * Lifecycle method. This is called whenever the [SceneGraph] is set before [initialize] is called.
     * Any nodes added to this [Node] context won't be added until the next frame update.
     */
    open fun Node.initialize() = Unit

    /**
     * Lifecycle method. This is called when this scene becomes the active scene.
     */
    open fun onStart() = Unit

    open fun update(dt: Duration) {
        camera.update()
        if (root.enabled && (root.updateInterval == 1 || frameCount % root.updateInterval == 0)) {
            root._update(dt)
        }
        frameCount++
    }

    /**
     * Lifecycle method. Do any necessary unloading / disposing here. This is called when this scene is removed
     * from the active slot.
     */
    override fun dispose() {
        root.destroy()
        if (ownsBatch) {
            batch.dispose()
        }
        gpuFontBatch.dispose()
        context.input.removeInputProcessor(this)
    }
}