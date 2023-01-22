package com.lehaine.littlekt.graph.node

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.render.Material
import com.lehaine.littlekt.graph.node.resource.InputEvent
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.g2d.shape.ShapeRenderer
import com.lehaine.littlekt.math.*
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.util.Signal
import com.lehaine.littlekt.util.TripleSignal
import com.lehaine.littlekt.util.signal
import com.lehaine.littlekt.util.signal3v
import kotlin.js.JsName


/**
 * A [Node] with 2D transformations.
 * @author Colton Daily
 * @date 1/1/2022
 */
abstract class CanvasItem : Node() {

    companion object {
        const val POSITION_DIRTY = 1
        const val SCALE_DIRTY = 2
        const val ROTATION_DIRTY = 3
        const val CLEAN = 0

        private val tempVec = MutableVec2f()
    }

    var material: Material? = null

    /**
     * List of 'preRender' callbacks called when [onPreRender] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onPreRender += { batch, camera, shapeRenderer ->
     *         // handle render logic
     *     }
     * }
     * ```
     */
    val onPreRender: TripleSignal<Batch, Camera, ShapeRenderer> = signal3v()

    /**
     * List of 'render' callbacks called when [propagateInternalRender] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onRender += { batch, camera, shapeRenderer ->
     *         // handle render logic
     *     }
     * }
     * ```
     */
    val onRender: TripleSignal<Batch, Camera, ShapeRenderer> = signal3v()

    /**
     * List of 'postRender' callbacks called after [postRender] is called. Add any additional callbacks directly to this list.
     * The main use is to add callbacks directly to nodes inline when building a [SceneGraph] vs having to extend
     * a class directly.
     *
     * ```
     * node {
     *     onPostRender += { batch, camera, shapeRenderer ->
     *         // handle render logic
     *     }
     * }
     * ```
     */
    val onPostRender: TripleSignal<Batch, Camera, ShapeRenderer> = signal3v()

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
    val onDebugRender: TripleSignal<Batch, Camera, ShapeRenderer> = signal3v()

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

    /**
     * The position of the [CanvasItem] in global space. If you want to set the [x,y] properties of this [Vec2f] then use
     * the [globalX] and [globalY] properties of this [CanvasItem]
     */
    var globalPosition: Vec2f
        get() {
            updateTransform()
            if (_globalPositionDirty) {
                (parent as? CanvasItem)?.let {
                    it.updateTransform()
                    _globalPosition.set(_localPosition).mul(it._globalTransform)
                } ?: run {
                    _globalPosition.set(_localPosition)
                }

                _globalPositionDirty = false
            }
            return _globalPosition
        }
        set(value) {
            globalPosition(value)
        }


    var globalX: Float
        get() {
            return globalPosition.x
        }
        set(value) {
            if (value == _globalPosition.x) {
                return
            }
            _globalPosition.x = value
            updateGlobalPosition()
        }

    var globalY: Float
        get() {
            return globalPosition.y
        }
        set(value) {
            if (value == _globalPosition.y) {
                return
            }
            _globalPosition.y = value
            updateGlobalPosition()
        }

    /**
     * The position of the [CanvasItem] relative to the parent transform. If the [CanvasItem] has no parent or if the parent node is NOT
     * a [CanvasItem], then it is the same a [globalPosition]. If you want to set the [x,y] properties of this [Vec2f] then use
     * the [x] and [y] properties of this [CanvasItem]
     */
    var position: Vec2f
        get() {
            updateTransform()
            return _localPosition
        }
        set(value) {
            position(value)
        }


    var x: Float
        get() {
            return position.x
        }
        set(value) {
            if (value == _localPosition.x) {
                return
            }
            _localPosition.x = value
            updateLocalPosition()
        }

    var y: Float
        get() {
            return position.y
        }
        set(value) {
            if (value == _localPosition.y) {
                return
            }
            _localPosition.y = value
            updateLocalPosition()
        }


    /**
     * The rotation of the [CanvasItem] in global space in radians
     */
    var globalRotation: Angle
        get() {
            updateTransform()
            return _globalRotation
        }
        set(value) {
            globalRotation(value)
        }

    /**
     * The rotation of the [CanvasItem] relative to the parent transform's rotation. If the [CanvasItem] has no parent or if the parent node is NOT
     * a [CanvasItem], then it is the same a [globalRotation]
     */
    var rotation: Angle
        get() {
            updateTransform()
            return _localRotation
        }
        set(value) {
            rotation(value)
        }


    /**
     * The global scale of the [CanvasItem]. If you want to set the [x,y] properties of this [Vec2f] then use
     * the [globalScaleX] and [globalScaleY] properties of this [CanvasItem].
     */
    var globalScale: Vec2f
        get() {
            updateTransform()
            return _globalScale
        }
        set(value) {
            globalScale(value)
        }

    /**
     * The global x-scale of the [CanvasItem].
     * @see globalScale
     */
    var globalScaleX: Float
        get() {
            return _globalScale.x
        }
        set(value) {
            _globalScale.x = value
            updateScale()
        }

    /**
     * The global y-scale of the [CanvasItem].
     * @see globalScale
     */
    var globalScaleY: Float
        get() {
            return _globalScale.y
        }
        set(value) {
            _globalScale.y = value
            updateScale()
        }


    /**
     * The scale of the [CanvasItem] relative to the parent transform's scales. If the [CanvasItem] has no parent or if the parent node is NOT
     * a [CanvasItem], then it is the same a [globalScale]. If you want to set the [x,y] properties of this [Vec2f] then use
     * the [scaleX] and [scaleY] properties of this [CanvasItem].
     */
    var scale: Vec2f
        get() {
            updateTransform()
            return _localScale
        }
        set(value) {
            scale(value)
        }

    /**
     * The x-scale of the [CanvasItem] relative to the parent transform's scales.
     * @see scale
     */
    var scaleX: Float
        get() {
            return _localScale.x
        }
        set(value) {
            if (_localScale.x == value) return
            _localScale.x = value
            updateLocalScale()
        }

    /**
     * The y-scale of the [CanvasItem] relative to the parent transform's scales.
     * @see scale
     */
    var scaleY: Float
        get() {
            return _localScale.y
        }
        set(value) {
            if (_localScale.y == value) return
            _localScale.y = value
            updateLocalScale()
        }

    val globalInverseTransform: Mat3
        get() {
            updateTransform()
            if (_globalInverseDirty) {
                _globalInverseTransform.set(_globalTransform).invert()
                _globalInverseDirty = false
            }
            return _globalInverseTransform
        }

    val localToGlobalTransform: Mat3
        get() {
            updateTransform()
            return _globalTransform
        }

    val localToGlobalTransformMat4: Mat4 = Mat4()
        get() {
            field.set(localToGlobalTransform)
            return field
        }

    val globalToLocalTransform: Mat3
        get() {
            if (_globalToLocalDirty) {
                (parent as? CanvasItem)?.let {
                    it.updateTransform()
                    _globalToLocalTransform.set(it.globalInverseTransform)
                } ?: run {
                    _globalToLocalTransform.setToIdentity()
                }
                _globalToLocalDirty = false
            }
            return _globalToLocalTransform
        }

    override val membersAndPropertiesString: String
        get() = "${super.membersAndPropertiesString}, visible=$visible, globalPosition=$globalPosition, position=$position, globalRotation=$globalRotation, rotation=$rotation, globalScale=$globalScale, scale=$scale"

    private var _visible = true
    protected var hierarchyDirty: Int = CLEAN

    private var _localDirty = false
    private var _localPositionDirty = false
    private var _localScaleDirty = false
    private var _localRotationDirty = false
    private var _globalPositionDirty = false
    private var _globalToLocalDirty = false
    private var _globalInverseDirty = false

    private val _localTransform = Mat3()

    private var _globalTransform = Mat3()
    private var _globalToLocalTransform = Mat3()
    private var _globalInverseTransform = Mat3()

    private var _rotationMatrix = Mat3()
    private var _translationMatrix = Mat3()
    private var _scaleMatrix = Mat3()

    private var _globalPosition = MutableVec2f()
    private var _globalScale = MutableVec2f(1f, 1f)
    private var _globalRotation = Angle.ZERO

    private var _localPosition = MutableVec2f()
    private var _localScale = MutableVec2f(1f, 1f)
    private var _localRotation = Angle.ZERO


    override fun parent(parent: Node?): Node {
        if (_parent == parent) {
            return this
        }
        super.parent(parent)
        dirty(POSITION_DIRTY)

        return this
    }

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
        if (!batch.drawing) batch.begin()
        renderCallback?.invoke(this, batch, camera, camera3d, shapeRenderer)
        debugRender(batch, camera, shapeRenderer)
        onDebugRender.emit(batch, camera, shapeRenderer)
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
        propagatePreRender(batch, camera, shapeRenderer)
        propagateRender(batch, camera, camera3d, shapeRenderer, renderCallback)
        propagatePostRender(batch, camera, shapeRenderer)
    }

    /**
     * Internal pre rendering that needs to be done on the node that shouldn't be overridden. Calls [propagatePreRender] method.
     */
    fun propagatePreRender(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        if (!enabled || !visible || isDestroyed) return
        if (!batch.drawing) batch.begin()
        preRender(batch, camera, shapeRenderer)
        onPreRender.emit(batch, camera, shapeRenderer)
        nodes.forEachSorted {
            if (it is CanvasItem) {
                it.propagatePreRender(batch, camera, shapeRenderer)
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
        render(batch, camera, shapeRenderer)
        onRender.emit(batch, camera, shapeRenderer)
        nodes.forEachSorted {
            it.propagateInternalRender(batch, camera, camera3d, shapeRenderer, renderCallback)
        }
    }

    /**
     * Internal post rendering that needs to be done on the node that shouldn't be overridden. Calls [propagatePostRender] method.
     */
    fun propagatePostRender(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        if (!enabled || !visible || isDestroyed) return
        postRender(batch, camera, shapeRenderer)
        onPostRender.emit(batch, camera, shapeRenderer)
        nodes.forEachSorted {
            if (it is CanvasItem) {
                it.propagatePostRender(batch, camera, shapeRenderer)
            }
        }
    }

    /**
     * Invoked before [render]. Calculations or logic that needs to be done before rendering such as flushing the batch
     * or starting a frame buffer. The [Camera] can be used for culling and the [Batch] instance to draw with.
     * @param batch the batcher
     * @param camera the Camera2D node
     * @param shapeRenderer the shape renderer
     */
    open fun preRender(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {}

    /**
     * The main render method. The [Camera] can be used for culling and the [Batch] instance to draw with.
     * @param batch the batcher
     * @param camera the Camera2D node
     * @param shapeRenderer the shape renderer
     */
    open fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {}

    /**
     * Invoked after [render]. Calculations or logic that needs to be done after rendering such as flushing the batch
     * or ending a frame buffer. The [Camera] can be used for culling and the [Batch] instance to draw with.
     * @param batch the batcher
     * @param camera the Camera2D node
     * @param shapeRenderer the shape renderer
     */
    open fun postRender(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {}

    /**
     * Draw any debug related items here.
     * @param batch the sprite batch to draw with
     * @param camera the Camera2D node
     * @param shapeRenderer the shape renderer
     */
    open fun debugRender(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {}


    /**
     * Called when [visible] is set to `true`.
     */
    protected open fun onVisible() = Unit

    /**
     * Called when [visible] is set to `false`.
     */
    protected open fun onInvisible() = Unit

    override fun callInput(event: InputEvent<*>) {
        if (!enabled || !insideTree || isDestroyed) return

        event.apply {
            val localCoords = toLocal(event.sceneX, event.sceneY, tempVec)
            localX = localCoords.x
            localY = localCoords.y
        }
        onInput.emit(event) // signal is first due to being able to handle the event
        if (event.handled) {
            return
        }
        input(event)
    }

    override fun callUnhandledInput(event: InputEvent<*>) {
        if (!enabled || !insideTree || isDestroyed) return

        event.apply {
            val localCoords = toLocal(event.sceneX, event.sceneY, tempVec)
            localX = localCoords.x
            localY = localCoords.y
        }
        onUnhandledInput.emit(event) // signal is first due to being able to handle the event
        if (event.handled) {
            return
        }
        unhandledInput(event)
    }

    /**
     * Sets the position of the [CanvasItem] in global space.
     * @param value the new position
     * @return the current [CanvasItem]
     */
    fun globalPosition(value: Vec2f): CanvasItem {
        if (value == _globalPosition) {
            return this
        }
        _globalPosition.set(value)
        updateGlobalPosition()
        return this
    }

    /**
     * Sets the position of the [CanvasItem] in global space.
     * @param x the new x position
     * @param y the new y position
     * @return the current [CanvasItem]
     */
    fun globalPosition(x: Float, y: Float): CanvasItem {
        if (_globalPosition.x == x && _globalPosition.y == y) {
            return this
        }
        _globalPosition.set(x, y)
        updateGlobalPosition()
        return this
    }

    private fun updateGlobalPosition() {
        _localPosition.set(_globalPosition)
        updateTransform()
        if (parent is CanvasItem) {
            _localPosition.mul(globalToLocalTransform)
        }
        updateLocalPosition()
        _globalPositionDirty = false
    }

    /**
     * Sets the position of the [CanvasItem] relative to the parent [Node]. If the [CanvasItem] has no parent or if the parent node is NOT
     * a [CanvasItem], then it is the same a [globalPosition]
     * @param value the new position
     * @return the current [CanvasItem]
     */
    fun position(value: Vec2f): CanvasItem {
        if (value == _localPosition) {
            return this
        }

        _localPosition.set(value)
        updateLocalPosition()

        return this
    }

    /**
     * Sets the position of the [CanvasItem] relative to the parent [Node]. If the [CanvasItem] has no parent or if the parent node is NOT
     * a [CanvasItem], then it is the same a [globalPosition]
     * @param x the new x position
     * @param y the new y position
     * @return the current [CanvasItem]
     */
    fun position(x: Float, y: Float, invokeCallback: Boolean = true): CanvasItem {
        if (_localPosition.x == x && _localPosition.y == y) {
            return this
        }
        _localPosition.set(x, y)
        updateLocalPosition(invokeCallback)
        return this
    }

    private fun updateLocalPosition(invokeCallback: Boolean = true) {
        if (invokeCallback) {
            onPositionChanged()
        }
        _localDirty = true
        _globalPositionDirty = true
        _localPositionDirty = true
        _localRotationDirty = true
        _localScaleDirty = true
        dirty(POSITION_DIRTY)
    }

    /**
     * Invoked when this [CanvasItem] position is changed.
     */
    protected open fun onPositionChanged() {}

    /**
     * Sets the rotation of the [CanvasItem] in global space in radians.
     * @param angle the new rotation
     * @return the current [CanvasItem]
     */
    fun globalRotation(angle: Angle): CanvasItem {
        if (_globalRotation == angle) {
            return this
        }
        _globalRotation = angle
        (parent as? CanvasItem)?.let {
            rotation = it.globalRotation + angle
        } ?: run {
            rotation = angle
        }

        return this
    }

    /**
     * Sets the rotation of the [CanvasItem] relative to the parent [Node] in radians. If the [CanvasItem] has no parent or if the parent node is NOT
     * a [CanvasItem], then it is the same a [globalRotation]
     * @param angle the new rotation
     * @return the current [CanvasItem]
     */
    fun rotation(angle: Angle): CanvasItem {
        if (_localRotation == angle) {
            return this
        }
        _localRotation = angle

        _localDirty = true
        _globalPositionDirty = true
        _localPositionDirty = true
        _localRotationDirty = true
        _localScaleDirty = true
        dirty(ROTATION_DIRTY)

        return this
    }

    /**
     * Sets the global scale of the [CanvasItem].
     * @param value the new scale
     * @return the current [CanvasItem]
     */
    fun globalScale(value: Vec2f): CanvasItem {
        if (_globalScale == value) {
            return this
        }
        _globalScale.set(value)
        updateScale()
        return this
    }

    /**
     * Sets the global scale of the [CanvasItem].
     * @param x the new x scale
     * @param y the new y scale
     * @return the current [CanvasItem]
     */
    fun globalScale(x: Float, y: Float): CanvasItem {
        if (_globalScale.x == x && _globalScale.y == y) {
            return this
        }
        _globalScale.set(x, y)
        updateScale()
        return this
    }

    private fun updateScale() {
        _localScale.set(_globalScale)
        val canvasItem = parent as? CanvasItem
        if (canvasItem != null) {
            _localScale /= canvasItem._globalScale
        }
        updateLocalScale()
    }

    /**
     * Sets the scale of the [CanvasItem] relative to the parent transform's scales. If the [CanvasItem] has no parent or if the parent node is NOT
     * a [CanvasItem], then it is the same a [globalScale]
     */
    fun scale(value: Vec2f): CanvasItem {
        if (value == _localScale) {
            return this
        }
        _localScale.set(value)

        updateLocalScale()

        return this
    }

    fun scale(x: Float, y: Float): CanvasItem {
        if (x == _localScale.x && y == _localScale.y) {
            return this
        }
        _localScale.set(x, y)

        updateLocalScale()

        return this
    }

    private fun updateLocalScale() {
        _localDirty = true
        _globalPositionDirty = true
        _localScaleDirty = true
        dirty(SCALE_DIRTY)
    }


    /**
     * Dirties the hierarchy for the current [Node] and all of it's [children].
     */
    protected open fun dirty(dirtyFlag: Int) {
        if ((hierarchyDirty and dirtyFlag) == 0) {
            hierarchyDirty = hierarchyDirty or dirtyFlag

            nodes.forEach {
                it.propagateDirty(dirtyFlag)
            }
            propagateDirty(dirtyFlag)
            _onHierarchyChanged(dirtyFlag)
        }
    }

    private fun Node.propagateDirty(dirtyFlag: Int) {
        if (this is CanvasItem) {
            dirty(dirtyFlag)
        } else {
            nodes.forEach { it.propagateDirty(dirtyFlag) }
        }
    }

    /**
     * Internal. Called when the hierarchy of this [CanvasItem] is changed.
     * Example changes that can trigger this includes: `position`, `rotation`, and `scale`
     */
    private fun _onHierarchyChanged(flag: Int) {
        onHierarchyChanged(flag)
    }

    /**
     * Called when the hierarchy of this [CanvasItem] is changed.
     * Example changes that can trigger this include: `position`, `rotation`, and `scale`
     */
    protected open fun onHierarchyChanged(flag: Int) {}

    /**
     * Updates the current [CanvasItem] transform if it is dirty.
     */
    open fun updateTransform() {
        if (hierarchyDirty != CLEAN) {
            (parent as? CanvasItem)?.updateTransform()
            if (_localDirty) {
                if (_localPositionDirty) {
                    _translationMatrix.setToTranslate(_localPosition)
                    _localPositionDirty = false
                }

                if (_localRotationDirty) {
                    _rotationMatrix.setToRotation(_localRotation)
                    _localRotationDirty = false
                }

                if (_localScaleDirty) {
                    _scaleMatrix.setToScale(_localScale)
                    _localScaleDirty = false
                }

                _localTransform.set(_translationMatrix).mul(_rotationMatrix).mul(_scaleMatrix)
                if (parent !is CanvasItem) {
                    _globalTransform.set(_localTransform)
                    _globalRotation = _localRotation
                    _globalScale.set(_localScale)
                    _globalInverseDirty = true
                }
                _localDirty = false
            }

            (parent as? CanvasItem)?.let {
                _globalTransform.set(it._globalTransform).mul(_localTransform)

                _globalRotation = _localRotation + it._globalRotation
                _globalScale.set(it._globalScale).scale(_localScale)
                _globalInverseDirty = true
            }

            _globalToLocalDirty = true
            _globalPositionDirty = true
            hierarchyDirty = CLEAN
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        onPreRender.clear()
        onRender.clear()
        onPostRender.clear()
        onDebugRender.clear()
        onVisible.clear()
        onInvisible.clear()
    }

    /**
     * Translates the position by the offset vector in `local` coordinates.
     * @param offset the amount to translate by
     */
    fun translate(offset: Vec2f) = translate(offset.x, offset.y)

    /**
     * Translates the position by the offset vector in `local` coordinates.
     * @param x the amount to translate x by
     * @param y the amount to translate y by
     */
    fun translate(x: Float, y: Float) {
        if (x != 0f || y != 0f) {
            _localPosition.add(x, y)
            updateLocalPosition()
        }
    }

    /**
     * Transforms the provided local position into a position in global coordinate space.
     * The input is expected to be local relative to the Node2D it is called on. e.g.
     * Applying this method to the positions of child nodes will correctly transform their positions into the global coordinate space,
     * but applying it to a node's own position will give an incorrect result, as it will incorporate the node's own transformation into its global position.
     */
    fun toGlobal(point: Vec2f): Vec2f = toGlobal(point, MutableVec2f())

    /**
     * Transforms the provided local position into a position in global coordinate space.
     * The input is expected to be local relative to the Node2D it is called on. e.g.
     * Applying this method to the positions of child nodes will correctly transform their positions into the global coordinate space,
     * but applying it to a node's own position will give an incorrect result, as it will incorporate the node's own transformation into its global position.
     */
    fun toGlobal(x: Float, y: Float): Vec2f = toGlobal(x, y, MutableVec2f())

    /**
     * Transforms the provided local position into a position in global coordinate space.
     * The input is expected to be local relative to the Node2D it is called on. e.g.
     * Applying this method to the positions of child nodes will correctly transform their positions into the global coordinate space,
     * but applying it to a node's own position will give an incorrect result, as it will incorporate the node's own transformation into its global position.
     */
    fun toGlobal(point: Vec2f, out: MutableVec2f): MutableVec2f = toGlobal(point.x, point.y, out)

    /**
     * Transforms the provided local position into a position in global coordinate space.
     * The input is expected to be local relative to the Node2D it is called on. e.g.
     * Applying this method to the positions of child nodes will correctly transform their positions into the global coordinate space,
     * but applying it to a node's own position will give an incorrect result, as it will incorporate the node's own transformation into its global position.
     */
    fun toGlobal(x: Float, y: Float, out: MutableVec2f): MutableVec2f = localToGlobalTransform.transform(out.set(x, y))


    /**
     * Transforms the provided global position into a position in local coordinate space.
     * The output will be local relative to the Node2D it is called on. e.g. It is appropriate for determining the positions of child nodes,
     * but it is not appropriate for determining its own position relative to its parent.
     */
    fun toLocal(point: Vec2f): Vec2f = toLocal(point, MutableVec2f())

    /**
     * Transforms the provided global position into a position in local coordinate space.
     * The output will be local relative to the Node2D it is called on. e.g. It is appropriate for determining the positions of child nodes,
     * but it is not appropriate for determining its own position relative to its parent.
     */
    fun toLocal(x: Float, y: Float): Vec2f = toLocal(x, y, MutableVec2f())

    /**
     * Transforms the provided global position into a position in local coordinate space.
     * The output will be local relative to the Node2D it is called on. e.g. It is appropriate for determining the positions of child nodes,
     * but it is not appropriate for determining its own position relative to its parent.
     */
    fun toLocal(point: Vec2f, out: MutableVec2f): MutableVec2f = toLocal(point.x, point.y, out)

    /**
     * Transforms the provided global position into a position in local coordinate space.
     * The output will be local relative to the Node2D it is called on. e.g. It is appropriate for determining the positions of child nodes,
     * but it is not appropriate for determining its own position relative to its parent.
     */
    fun toLocal(x: Float, y: Float, out: MutableVec2f): MutableVec2f = globalInverseTransform.transform(out.set(x, y))


    fun copyFrom(node: CanvasItem) {
        _globalPosition = node.globalPosition.toMutableVec2()
        _localPosition = node.position.toMutableVec2()
        _globalRotation = node.globalRotation
        _localRotation = node.rotation
        _globalScale = node.globalScale.toMutableVec2()
        _localScale = node.scale.toMutableVec2()

        dirty(POSITION_DIRTY)
        dirty(ROTATION_DIRTY)
        dirty(SCALE_DIRTY)
    }
}