package com.lehaine.littlekt.graph.node

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.util.SingleSignal
import com.lehaine.littlekt.util.signal1v
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun Node.frameBuffer(
    callback: @SceneGraphDslMarker FrameBufferNode.() -> Unit = {}
): FrameBufferNode {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return FrameBufferNode().also(callback).addTo(this)
}

@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.frameBuffer(
    callback: @SceneGraphDslMarker FrameBufferNode.() -> Unit = {}
): FrameBufferNode {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.frameBuffer(callback)
}

/**
 * @author Colton Daily
 * @date 3/14/2022
 */
class FrameBufferNode : Node() {

    var width: Int = 0
    var height: Int = 0
    val fboTexture: Texture get() = fbo?.colorBufferTexture ?: error("FBO hasn't been created yet!")
    val onFboChanged: SingleSignal<Texture> = signal1v()

    private val fboCamera = OrthographicCamera(width, height)
    private var lastWidth = width
    private var lastHeight = height

    private var fbo: FrameBuffer? = null

    override fun onAddedToScene() {
        super.onAddedToScene()
        if (lastWidth != width || lastHeight != height) {
            scene?.let { scene ->
                fbo?.dispose()
                fbo = FrameBuffer(
                    width,
                    height,
                    minFilter = TexMinFilter.NEAREST,
                    magFilter = TexMagFilter.NEAREST
                ).also { it.prepare(scene.context) }
                lastWidth = width
                lastHeight = height
                fboCamera.virtualWidth = width
                fboCamera.virtualHeight = height
                fboCamera.position.set(fboCamera.virtualWidth * 0.5f, fboCamera.virtualHeight * 0.5f, 0f)
                onFboChanged.emit(fboTexture)
            }
        }
    }

    override fun _render(batch: Batch, camera: Camera, renderCallback: ((Node, Batch, Camera) -> Unit)?) {
        if (lastWidth != width || lastHeight != height) {
            scene?.let { scene ->
                fbo?.dispose()
                fbo = FrameBuffer(
                    width,
                    height,
                    minFilter = TexMinFilter.NEAREST,
                    magFilter = TexMagFilter.NEAREST
                ).also { it.prepare(scene.context) }
                lastWidth = width
                lastHeight = height
                fboCamera.virtualWidth = width
                fboCamera.virtualHeight = height
                fboCamera.position.set(fboCamera.virtualWidth * 0.5f, fboCamera.virtualHeight * 0.5f, 0f)
                onFboChanged.emit(fboTexture)
            }
        }
        val fbo = fbo ?: return
        val context = scene?.context ?: return
        val gl = context.gl
        if (width == 0 || height == 0) return
        batch.end()
        val prevProjection = batch.projectionMatrix

        fboCamera.update()
        fbo.begin()
        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        batch.use(fboCamera.viewProjection) {
            super._render(batch, fboCamera, renderCallback)
        }
        fbo.end()
        batch.begin(prevProjection)
    }

    override fun onDestroy() {
        super.onDestroy()
        onFboChanged.clear()
    }
}