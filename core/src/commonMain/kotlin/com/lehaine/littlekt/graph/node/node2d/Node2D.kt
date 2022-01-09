package com.lehaine.littlekt.graph.node.node2d

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.math.Mat3
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.plus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Adds a [Node2D] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [Node2D] context in order to initialize any values
 * @return the newly created [Node2D]
 */
inline fun Node.node2d(callback: @SceneGraphDslMarker Node2D.() -> Unit = {}) =
    Node2D().also(callback).addTo(this)

/**
 * Adds a [Node2D] to the current [SceneGraph.root] as a child and then triggers the [Node2D]
 * @param callback the callback that is invoked with a [Node2D] context in order to initialize any values
 * @return the newly created [Node2D]
 */
inline fun SceneGraph.node2d(callback: @SceneGraphDslMarker Node2D.() -> Unit = {}) = root.node2d(callback)

/**
 * A [Node] with 2D transformations.
 * @author Colton Daily
 * @date 1/1/2022
 */
open class Node2D : Node() {

    companion object {
        const val POSITION_DIRTY = 1
        const val SCALE_DIRTY = 2
        const val ROTATION_DIRTY = 3
    }

    /**
     * The position of the [Node2D] in world space. If you want to set the [x,y] properties of this [Vector2] then use
     * the [globalX] and [globalY] properties of this [Node2D]
     */
    var globalPosition: Vec2f
        get() {
            updateHierarchy()
            if (_globalPositionDirty) {
                (parent as? Node2D)?.let {
                    it.updateHierarchy()
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
     * The position of the [Node2D] relative to the parent transform. If the [Node2D] has no parent or if the parent node is NOT
     * a [Node2D], then it is the same a [globalPosition]. If you want to set the [x,y] properties of this [Vector2] then use
     * the [x] and [y] properties of this [Node2D]
     */
    var position: Vec2f
        get() {
            updateHierarchy()
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
     * The rotation of the [Node2D] in world space in radians
     */
    var globalRotation: Angle
        get() {
            updateHierarchy()
            return _globalRotation
        }
        set(value) {
            globalRotation(value)
        }

    /**
     * The rotation of the [Node2D] relative to the parent transform's rotation. If the [Node2D] has no parent or if the parent node is NOT
     * a [Node2D], then it is the same a [globalRotation]
     */
    var rotation: Angle
        get() {
            updateHierarchy()
            return _localRotation
        }
        set(value) {
            rotation(value)
        }


    /**
     * The global scale of the [Node2D]. If you want to set the [x,y] properties of this [Vector2] then use
     * the [globalScaleX] and [globalScaleY] properties of this [Node2D].
     */
    var globalScale: Vec2f
        get() {
            updateHierarchy()
            return _globalScale
        }
        set(value) {
            globalScale(value)
        }

    var globalScaleX: Float
        get() {
            return _globalScale.x
        }
        set(value) {
            _globalScale.x = value
            updateScale()
        }

    var globalScaleY: Float
        get() {
            return _globalScale.y
        }
        set(value) {
            _globalScale.y = value
            updateScale()
        }


    /**
     * The scale of the [Node2D] relative to the parent transform's scales. If the [Node2D] has no parent or if the parent node is NOT
     * a [Node2D], then it is the same a [globalScale]. If you want to set the [x,y] properties of this [Vector2] then use
     * the [scaleX] and [scaleY] properties of this [Node2D].
     */
    var scale: Vec2f
        get() {
            updateHierarchy()
            return _localScale
        }
        set(value) {
            scale(value)
        }

    var scaleX: Float
        get() {
            return _localScale.x
        }
        set(value) {
            _localScale.x = value
            updateLocalScale()
        }

    var scaleY: Float
        get() {
            return _localScale.y
        }
        set(value) {
            _localScale.y = value
            updateLocalScale()
        }

    val globalInverseTransform: Mat3
        get() {
            updateHierarchy()
            if (_globalInverseDirty) {
                _globalInverseTransform.set(_globalTransform).invert()
                _globalInverseDirty = false
            }
            return _globalInverseTransform
        }

    val localToGlobalTransform: Mat3
        get() {
            updateHierarchy()
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
                (parent as? Node2D)?.let {
                    it.updateHierarchy()
                    _globalToLocalTransform.set(it._globalTransform).invert()
                } ?: run {
                    _globalToLocalTransform.setToIdentity()
                }
                _globalToLocalDirty = false
            }
            return _globalToLocalTransform
        }

    override val membersAndPropertiesString: String
        get() = "${super.membersAndPropertiesString}, globalPosition=$globalPosition, position=$position, globalRotation=$globalRotation, rotation=$rotation, globalScale=$globalScale, scale=$scale"

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
     * Sets the position of the [Node2D] in world space.
     * @param value the new position
     * @return the current [Node2D]
     */
    fun globalPosition(value: Vec2f): Node2D {
        if (value == _globalPosition) {
            return this
        }
        _globalPosition.set(value)
        updateGlobalPosition()
        return this
    }

    fun globalPosition(x: Float, y: Float): Node2D {
        if (_globalPosition.x == x && _globalPosition.y == y) {
            return this
        }
        _globalPosition.set(x, y)
        updateGlobalPosition()
        return this
    }

    private fun updateGlobalPosition() {
        _localPosition.set(_globalPosition)
        updateHierarchy()
        if (parent is Node2D) {
            _localPosition.mul(globalToLocalTransform)
        }
        updateLocalPosition()
        _globalPositionDirty = false
    }

    /**
     * Sets the position of the [Node2D] relative to the parent [Node]. If the [Node2D] has no parent or if the parent node is NOT
     * a [Node2D], then it is the same a [globalPosition]
     * @param value the new position
     * @return the current [Node2D]
     */
    fun position(value: Vec2f): Node2D {
        if (value == _localPosition) {
            return this
        }

        _localPosition.set(value)
        updateLocalPosition()

        return this
    }

    fun position(x: Float, y: Float): Node2D {
        if (_localPosition.x == x && _localPosition.y == y) {
            return this
        }
        _localPosition.set(x, y)
        updateLocalPosition()
        return this
    }

    protected fun updateLocalPosition() {
        onPositionChanged()
        _localDirty = true
        _globalPositionDirty = true
        _localPositionDirty = true
        _localRotationDirty = true
        _localScaleDirty = true
        dirty(POSITION_DIRTY)
    }

    protected open fun onPositionChanged() {}

    /**
     * Sets the rotation of the [Node2D] in world space in radians.
     * @param angle the new rotation
     * @return the current [Node2D]
     */
    fun globalRotation(angle: Angle): Node2D {
        if (_globalRotation == angle) {
            return this
        }
        _globalRotation = angle
        (parent as? Node2D)?.let {
            rotation = it.globalRotation + angle
        } ?: run {
            rotation = angle
        }

        return this
    }

    /**
     * Sets the rotation of the [Node2D] relative to the parent [Node] in radians. If the [Node2D] has no parent or if the parent node is NOT
     * a [Node2D], then it is the same a [globalRotation]
     * @param angle the new rotation
     * @return the current [Node2D]
     */
    fun rotation(angle: Angle): Node2D {
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
     * Sets the global scale of the [Node2D].
     * @param value the new scale
     * @return the current [Node2D]
     */
    fun globalScale(value: Vec2f): Node2D {
        if (_globalScale == value) {
            return this
        }
        _globalScale.set(value)
        updateScale()
        return this
    }

    fun globalScale(x: Float, y: Float): Node2D {
        if (_globalScale.x == x && _globalScale.y == y) {
            return this
        }
        _globalScale.set(x, y)
        updateScale()
        return this
    }

    private fun updateScale() {
        _localScale.set(_globalScale)
        val node2d = parent as? Node2D
        if (node2d != null) {
            _localScale /= node2d._globalScale
        }
        updateLocalScale()
    }

    /**
     * Sets the scale of the [Node2D] relative to the parent transform's scales. If the [Node2D] has no parent or if the parent node is NOT
     * a [Node2D], then it is the same a [globalScale]
     */
    fun scale(value: Vec2f): Node2D {
        if (value == _localScale) {
            return this
        }
        _localScale.set(value)

        updateLocalScale()

        return this
    }

    fun scale(x: Float, y: Float): Node2D {
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

    override fun updateHierarchy() {
        if (hierarchyDirty != CLEAN) {
            parent?.updateHierarchy()
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

                _localTransform.set(_scaleMatrix).mulLeft(_rotationMatrix).mulLeft(_translationMatrix)
                if (parent !is Node2D) {
                    _globalTransform.set(_localTransform)
                    _globalRotation = _localRotation
                    _globalScale.set(_localScale)
                    _globalInverseDirty = true
                }
                _localDirty = false
            }

            (parent as? Node2D)?.let {
                _globalTransform.set(_localTransform).mulLeft(it._globalTransform)

                _globalRotation = _localRotation + it._globalRotation
                _globalScale.set(it._globalScale).scale(_localScale)
                _globalInverseDirty = true
            }

            _globalToLocalDirty = true
            _globalPositionDirty = true
            hierarchyDirty = CLEAN
        }
    }

    fun translate(offset: Vec2f) = translate(offset.x, offset.y)

    fun translate(x: Float, y: Float) {
        if (x != 0f || y != 0f) {
            _localPosition.add(x, y)
            updateLocalPosition()
        }
    }

    /**
     * Transforms the provided local position into a position in world coordinate space.
     * The input is expected to be local relative to the Node2D it is called on. e.g.
     * Applying this method to the positions of child nodes will correctly transform their positions into the global coordinate space,
     * but applying it to a node's own position will give an incorrect result, as it will incorporate the node's own transformation into its global position.
     */
    fun toWorld(point: Vec2f): Vec2f {
        val x = (point.x - globalPosition.x) * cos(PI) + (point.y - globalPosition.y) * sin(PI)
        val y = -(point.x - globalPosition.y) * sin(PI) + (point.y - globalPosition.y) * cos(PI)
        return Vec2f(x.toFloat(), y.toFloat())
    }

    /**
     * Transforms the provided world position into a position in local coordinate space.
     * The output will be local relative to the Node2D it is called on. e.g. It is appropriate for determining the positions of child nodes,
     * but it is not appropriate for determining its own position relative to its parent.
     */
    fun toLocal(point: Vec2f): Vec2f {
        val x = globalPosition.x * cos(PI) - globalPosition.y * sin(PI) + point.x
        val y = globalPosition.x * sin(PI) + globalPosition.y * cos(PI) + point.y
        return Vec2f(x.toFloat(), y.toFloat())
    }

    fun copyFrom(node: Node2D) {
        _globalPosition = node.globalPosition.toMutableVec()
        _localPosition = node.position.toMutableVec()
        _globalRotation = node.globalRotation
        _localRotation = node.rotation
        _globalScale = node.globalScale.toMutableVec()
        _localScale = node.scale.toMutableVec()

        dirty(POSITION_DIRTY)
        dirty(ROTATION_DIRTY)
        dirty(SCALE_DIRTY)
    }

}