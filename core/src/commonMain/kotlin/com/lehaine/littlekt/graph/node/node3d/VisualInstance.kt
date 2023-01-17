package com.lehaine.littlekt.graph.node.node3d

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.CanvasItem
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.render.ModelMaterial
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.g2d.shape.ShapeRenderer
import com.lehaine.littlekt.util.Signal
import com.lehaine.littlekt.util.SingleSignal
import com.lehaine.littlekt.util.signal
import com.lehaine.littlekt.util.signal1v
import kotlin.js.JsName

/**
 * @author Colton Daily
 * @date 12/27/2022
 */
abstract class VisualInstance : Node3D() {
    var material: ModelMaterial? = null

    /**
     * List of 'preRender' callbacks called when [onPreRender] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onPreRender += { camera ->
     *         // handle render logic
     *     }
     * }
     * ```
     */
    val onPreRender: SingleSignal<Camera> = signal1v()

    /**
     * List of 'render' callbacks called when [propagateInternalRender] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onRender += { camera ->
     *         // handle render logic
     *     }
     * }
     * ```
     */
    val onRender: SingleSignal<Camera> = signal1v()

    /**
     * List of 'postRender' callbacks called after [postRender] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onPostRender += { camera ->
     *         // handle render logic
     *     }
     * }
     * ```
     */
    val onPostRender: SingleSignal<Camera> = signal1v()

    /**
     * List of 'debugRender' callbacks called when [debugRender] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onDebugRender += { batch, camera, shapeRenderer ->
     *         // handle debug render logic
     *     }
     * }
     * ```
     */
    val onDebugRender: SingleSignal<Camera> = signal1v()

    /**
     * List of 'onVisible' callbacks called when [onVisible] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onVisible += {
     *         // visibility logic
     *     }
     * }
     * ```
     */
    @JsName("onVisibleSignal")
    val onVisible: Signal = signal()

    /**
     * List of 'onInvisible' callbacks called when [onInvisible] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onInvisible += {
     *         // invisibility logic
     *     }
     * }
     * ```
     */
    @JsName("onInvisibleSignal")
    val onInvisible: Signal = signal()

    /**
     * Shows/hides the node if it is renderable.
     */
    var visible: Boolean
        get() = _visible
        set(value) {
            visible(value)
        }


    private var _visible = true

    /**
     * Shows/hides the [Node]. When disabled [propagateInternalRender] is no longer called.
     * @param value true to enable this node; false otherwise
     */
    fun visible(value: Boolean): Node {
        if (_visible != value) {
            _visible = value
            nodes.forEach {
                if (it is CanvasItem) {
                    it.visible(value)
                }
            }

            if (_visible) {
                onVisible()
                onVisible.emit()
            } else {
                onInvisible()
                onInvisible.emit()
            }
        }
        return this
    }

    override fun propagateInternalDebugRender(
        batch: Batch,
        camera: Camera,
        camera3d: Camera,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        propagateDebugRender(batch, camera, camera3d, shapeRenderer, renderCallback)
    }


    /**
     * Internal debug rendering that needs to be done on the node that shouldn't be overridden. Calls [propagateInternalDebugRender] method.
     */
    fun propagateDebugRender(
        batch: Batch,
        camera: Camera,
        camera3d: Camera,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        if (!enabled || !visible || isDestroyed) return
        renderCallback?.invoke(this, batch, camera, camera3d, shapeRenderer)
        debugRender(camera)
        onDebugRender.emit(camera)
        nodes.forEachSorted {
            it.propagateInternalDebugRender(batch, camera, camera3d, shapeRenderer, renderCallback)
        }
    }

    override fun propagateInternalRender(
        batch: Batch,
        camera: Camera,
        camera3d: Camera,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        if (batch.drawing) batch.end()
        propagatePreRender(camera3d)
        propagateRender(batch, camera, camera3d, shapeRenderer, renderCallback)
        propagatePostRender(camera3d)
    }

    /**
     * Internal pre rendering that needs to be done on the node that shouldn't be overridden. Calls [propagatePreRender] method.
     */
    fun propagatePreRender(camera: Camera) {
        if (!enabled || !visible || isDestroyed) return
        preRender(camera)
        onPreRender.emit(camera)
        nodes.forEachSorted {
            if (it is VisualInstance) {
                it.propagatePreRender(camera)
            }
        }
    }

    /**
     * Internal rendering that needs to be done on the node that shouldn't be overridden. Calls [propagateInternalRender] method.
     */
    fun propagateRender(
        batch: Batch,
        camera: Camera,
        camera3d: Camera,
        shapeRenderer: ShapeRenderer,
        renderCallback: ((Node, Batch, Camera, Camera, ShapeRenderer) -> Unit)?,
    ) {
        if (!enabled || !visible || isDestroyed) return
        renderCallback?.invoke(this, batch, camera, camera3d, shapeRenderer)
        val material = material ?: scene?.currentMaterial as? ModelMaterial ?: return
        setMaterialParameters(material, camera3d)
        render(camera3d)
        onRender.emit(camera3d)
        nodes.forEachSorted {
            it.propagateInternalRender(batch, camera, camera3d, shapeRenderer, renderCallback)
        }
    }

    /**
     * Internal post rendering that needs to be done on the node that shouldn't be overridden. Calls [propagatePostRender] method.
     */
    fun propagatePostRender(camera: Camera) {
        if (!enabled || !visible || isDestroyed) return
        postRender(camera)
        onPostRender.emit(camera)
        nodes.forEachSorted {
            if (it is VisualInstance) {
                it.propagatePostRender(camera)
            }
        }
    }

    /**
     * Invoked before [render]. Calculations or logic that needs to be done before rendering such as flushing the batch
     * or starting a frame buffer. The [Camera] can be used for culling.
     * @param camera the camera node
     */
    open fun preRender(camera: Camera) {}

    /**
     * The main render method. The [Camera] can be used for culling.
     * @param camera the camera
     */
    open fun render(camera: Camera) {}

    /**
     * Invoked after [render]. Calculations or logic that needs to be done after rendering such as flushing the batch
     * or ending a frame buffer. The [Camera] can be used for culling.
     * @param camera the camera node
     */
    open fun postRender(camera: Camera) {}

    /**
     * Draw any debug related items here.
     * @param camera the camera node
     */
    open fun debugRender(camera: Camera) {}


    /**
     * Called when [visible] is set to `true`.
     */
    protected open fun onVisible() = Unit

    /**
     * Called when [visible] is set to `false`.
     */
    protected open fun onInvisible() = Unit

    protected open fun setMaterialParameters(material: ModelMaterial, camera: Camera) {
        scene?.environment?.updateMaterial(material)
        material.model = globalTransform
        material.projection = camera.viewProjection
        material.viewPosition = camera.position
    }
}