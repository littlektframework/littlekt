package com.littlekt.graphics.g3d

import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.math.*
import com.littlekt.math.geom.Angle
import com.littlekt.math.spatial.BoundingBox
import com.littlekt.math.spatial.BoundingSphere
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.datastructure.fastForEachWithIndex
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * Represents a 3D node in a hierarchical scene graph.
 *
 * A `Node3D` stores and manages transformations such as position, rotation, and scale in 3D space.
 * It supports a parent-child hierarchy, allowing transformations to propagate through the scene
 * graph.
 *
 * ## Features:
 * - Maintains both local and global transformations.
 * - Supports hierarchical transformations via parent-child relationships.
 * - Provides conversion between global and local coordinate spaces.
 * - Automatically updates transformations when modified.
 *
 * ## Properties:
 * - `transform`: The local transformation matrix of the node.
 * - `globalTransform`: The transformation matrix in world space.
 * - `globalToLocalTransform`: Converts world-space coordinates to local space.
 *
 * ## Usage Example:
 * ```
 * val parentNode = Node3D()
 * val childNode = Node3D()
 * parentNode.addChild(childNode)
 * childNode.translate(1f, 0f, 0f) // Move 1 unit on the X-axis relative to the parent
 * println(childNode.globalTransform) // Prints world-space transform
 * ```
 *
 * @author Colton Daily
 * @date 11/24/2024
 */
open class Node3D {
    /** The name of the node. */
    var name: String = this::class.simpleName ?: "Node3D"

    /** The parent [Node3D], if any. */
    var parent: Node3D?
        get() = _parent
        set(value) {
            parent(value)
        }

    protected var _parent: Node3D? = null

    open var frustumCulled: Boolean = false

    /** The current child count for this [Node3D]. Alias for [children.size]. */
    val childCount: Int
        get() = children.size

    private val _children = mutableListOf<Node3D>()

    /** The list of [Node3D]s in this scene. */
    val children: List<Node3D>
        get() = _children

    /** If destroy was called, this will be true until the next time nodes are processed. */
    val isDestroyed
        get() = _isDestroyed

    /**
     * Global transform. Don't call `globalTransform.set` directly, the data won't be marked dirty.
     * Set the globalTransform directly with `globalTransform = myMat4`. This will copy the mat
     * values into the existing matrix.
     */
    var globalTransform: Mat4
        get() {
            updateTransform()
            return _globalTransform
        }
        set(value) {
            _globalTransform.set(value)
            _globalTransform.getTranslation(_globalPosition)
            updateGlobalPosition()
            _globalTransform.getRotation(_globalRotation)
            updateGlobalRotation()
            _globalTransform.getScale(_globalScale)
            updateGlobalScale()
            dirty()
        }

    private var _globalTransform = Mat4()

    /** The global transform matrix. */
    val localToGlobalTransform: Mat4
        get() {
            updateTransform()
            return _globalTransform
        }

    /** A matrix to convert global (world) space to local space. */
    val globalToLocalTransform: Mat4
        get() {
            if (_globalToLocalDirty) {
                parent?.let {
                    it.updateTransform()
                    _globalToLocalTransform.set(it.globalInverseTransform)
                } ?: run { _globalToLocalTransform.setToIdentity() }
                _globalToLocalDirty = false
            }
            return _globalToLocalTransform
        }

    private val _globalToLocalTransform = Mat4()

    /**
     * Local transform based on translation, scale, and rotation. Don't call `transform.set`
     * directly, the data won't be marked dirty. Set the transform directly with `transform =
     * myMat4`.
     */
    var transform: Mat4
        get() {
            updateTransform()
            return _transform
        }
        set(value) {
            _transform.set(value)
            _transform.getTranslation(_localPosition)
            _transform.getRotation(_localRotation)
            _transform.getScale(_localScale)
            dirty()
        }

    private var _transform = Mat4()

    /**
     * The position of the [Node3D] in global space. If you want to set the [x,y] properties of this
     * [Vec3f] then use the [globalX], [globalY], and [globalZ] properties of this [Node3D]
     */
    var globalPosition: Vec3f
        get() {
            updateTransform()
            if (_globalPositionDirty) {
                parent?.let {
                    it.updateTransform()
                    _globalPosition.set(_localPosition).mul(it._globalTransform)
                } ?: run { _globalPosition.set(_localPosition) }
                _globalPositionDirty = false
            }
            return _globalPosition
        }
        set(value) {
            globalPosition(value)
        }

    /** Global x-position. */
    var globalX: Float
        get() {
            return globalPosition.x
        }
        set(value) {
            if (value == _globalPosition.x) return
            _globalPosition.x = value
            updateGlobalPosition()
        }

    /** Global y-position. */
    var globalY: Float
        get() {
            return globalPosition.y
        }
        set(value) {
            if (value == _globalPosition.y) return
            _globalPosition.y = value
            updateGlobalPosition()
        }

    /** Global z-position. */
    var globalZ: Float
        get() {
            return globalPosition.z
        }
        set(value) {
            if (value == _globalPosition.z) return
            _globalPosition.z = value
            updateGlobalPosition()
        }

    /**
     * The position of the [Node3D] relative to the parent transform. If the [Node3D] has no parent
     * or if the parent node is NOT a [Node3D], then it is the same a [globalPosition]. If you want
     * to set the [x,y] properties of this [Vec3f] then use the [x], [y], and [z] properties of this
     * [Node3D]
     */
    var position: Vec3f
        get() {
            updateTransform()
            return _localPosition
        }
        set(value) {
            position(value)
        }

    /** Local x-position. */
    var x: Float
        get() {
            return position.x
        }
        set(value) {
            if (value == _localPosition.x) {
                return
            }
            _localPosition.x = value
            dirty()
        }

    /** Local y-position. */
    var y: Float
        get() {
            return position.y
        }
        set(value) {
            if (value == _localPosition.y) {
                return
            }
            _localPosition.y = value
            dirty()
        }

    /** Local z-position. */
    var z: Float
        get() {
            return position.z
        }
        set(value) {
            if (value == _localPosition.z) {
                return
            }
            _localPosition.z = value
            dirty()
        }

    /** The rotation of the [Node3D] in global space as a Quaternion. */
    var globalRotation: Quaternion
        get() {
            updateTransform()
            return _globalRotation
        }
        set(value) {
            globalRotation(value)
        }

    /**
     * The rotation of the [Node3D] relative to the parent transform's rotation as a Quaternion. If
     * the [Node3D] has no parent or if the parent node is NOT a [Node3D], then it is the same a
     * [globalRotation]
     */
    var rotation: Quaternion
        get() {
            updateTransform()
            return _localRotation
        }
        set(value) {
            rotation(value)
        }

    /**
     * The global scale of the [Node3D]. If you want to set the [x,y] properties of this [Vec3f]
     * then use the [globalScaleX], [globalScaleY], [globalScaleZ] properties of this [Node3D].
     */
    var globalScale: Vec3f
        get() {
            updateTransform()
            return _globalScale
        }
        set(value) {
            globalScaling(value)
        }

    /**
     * The global x-scale of the [Node3D].
     *
     * @see globalScaling
     */
    var globalScaleX: Float
        get() {
            return _globalScale.x
        }
        set(value) {
            _globalScale.x = value
            updateGlobalScale()
        }

    /**
     * The global y-scale of the [Node3D].
     *
     * @see globalScaling
     */
    var globalScaleY: Float
        get() {
            return _globalScale.y
        }
        set(value) {
            _globalScale.y = value
            updateGlobalScale()
        }

    /**
     * The global z-scale of the [Node3D].
     *
     * @see globalScaling
     */
    var globalScaleZ: Float
        get() {
            return _globalScale.z
        }
        set(value) {
            _globalScale.z = value
            updateGlobalScale()
        }

    /**
     * The scale of the [Node3D] relative to the parent transform's scales. If the [Node3D] has no
     * parent or if the parent node is NOT a [Node3D], then it is the same a [globalScaling]. If you
     * want to set the [x,y] properties of this [Vec3f] then use the [scaleX], [scaleY], [scaleZ]
     * properties of this [Node3D].
     */
    var scale: Vec3f
        get() {
            updateTransform()
            return _localScale
        }
        set(value) {
            scaling(value)
        }

    /**
     * The x-scale of the [Node3D] relative to the parent transform's scales.
     *
     * @see scaling
     */
    var scaleX: Float
        get() {
            return _localScale.x
        }
        set(value) {
            if (_localScale.x == value) return
            _localScale.x = value
            dirty()
        }

    /**
     * The y-scale of the [Node3D] relative to the parent transform's scales.
     *
     * @see scaling
     */
    var scaleY: Float
        get() {
            return _localScale.y
        }
        set(value) {
            if (_localScale.y == value) return
            _localScale.y = value
            dirty()
        }

    /**
     * The z-scale of the [Node3D] relative to the parent transform's scales.
     *
     * @see scaling
     */
    var scaleZ: Float
        get() {
            return _localScale.z
        }
        set(value) {
            if (_localScale.z == value) return
            _localScale.z = value
            dirty()
        }

    private val _globalInverseTransform = Mat4()

    /** The inverse matrix of [globalTransform]. */
    val globalInverseTransform: Mat4
        get() {
            updateTransform()
            if (_globalInverseDirty) {
                _globalInverseTransform.set(_globalTransform).invert()
                _globalInverseDirty = false
            }
            return _globalInverseTransform
        }

    /** Local axis-aligned bounding box. */
    val bounds: BoundingBox = BoundingBox()

    /** Global bounding sphere. */
    val globalBoundsSphere: BoundingSphere = BoundingSphere()

    private val tmpTransformVec = MutableVec3f()
    private val parentBoundsCache = BoundingBox()
    private val cachedBoundsMin = MutableVec3f()
    private val cachedBoundsMax = MutableVec3f()

    private var dirty = false

    private val _globalPosition = MutableVec3f()
    private val _globalScale = MutableVec3f(1f, 1f, 1f)
    private val _globalRotation = MutableQuaternion()

    private var _globalPositionDirty = false
    private var _globalToLocalDirty = false
    private var _globalInverseDirty = false

    private val _localPosition = MutableVec3f()
    private val _localScale = MutableVec3f(1f, 1f, 1f)
    private val _localRotation = MutableQuaternion()

    private var _rotationMatrix = Mat4()
    private var _translationMatrix = Mat4()
    private var _scaleMatrix = Mat4()

    private var _isDestroyed = false

    /** Marks the node and its children as dirty. */
    protected open fun dirty() {
        dirty = true
        children.fastForEach { it.propagateDirty() }
    }

    private fun Node3D.propagateDirty() {
        dirty()
    }

    /** Iterate over any [MeshPrimitive] descendants. Does not use frustum culling. */
    open fun forEachMeshPrimitive(action: (MeshPrimitive) -> Unit) {
        children.fastForEach { it.forEachMeshPrimitive(action) }
    }

    /** Iterate over any [MeshPrimitive] descendants using a [Camera] for frustum culling. */
    open fun forEachMeshPrimitive(camera: Camera, action: (MeshPrimitive) -> Unit) {
        if (frustumCulled) {
            val inFrustum =
                camera.sphereInFrustum(globalBoundsSphere.center, globalBoundsSphere.radius)
            if (inFrustum) {
                children.fastForEach { it.forEachMeshPrimitive(camera, action) }
            }
        } else {
            children.fastForEach { it.forEachMeshPrimitive(camera, action) }
        }
    }

    /** Set the color for this node and any descendants. */
    open fun color(color: Color) {
        children.fastForEach { it.propagateSetColor(color) }
    }

    private fun Node3D.propagateSetColor(color: Color) {
        color(color)
    }

    /**
     * Sets the parent [Node3D] of this [Node3D].
     *
     * @param parent this Nodes parent
     */
    open fun parent(parent: Node3D?): Node3D {
        if (_parent == parent) {
            return this
        }
        _parent?.removeChild(this)
        parent?.addChild(this)
        return this
    }

    /** @see addChild */
    operator fun plusAssign(child: Node3D) {
        addChild(child)
    }

    /** @see removeChild */
    operator fun minusAssign(child: Node3D) {
        removeChild(child)
    }

    /**
     * Sets the parent of the child to this [Node3D].
     *
     * @param child the child to add
     */
    fun addChild(child: Node3D): Node3D {
        _children.add(child)
        child._parent = this

        return this
    }

    /**
     * Sets the parent of the children to this [Node3D].
     *
     * @param children the children to add
     */
    fun addChildren(vararg children: Node3D): Node3D {
        children.fastForEach { addChild(it) }
        return this
    }

    /**
     * Removes the node if this node is its parent.
     *
     * @param child the child node to remove
     */
    fun removeChild(child: Node3D): Node3D {
        if (child.parent != this) return this

        child._parent = null
        _children.remove(child)

        return this
    }

    /** Update the node and any of its children. */
    fun update(dt: Duration) {
        onUpdate(dt)
        bounds.batchUpdate {
            clear()
            children.fastForEach {
                it.update(dt)
                addToBounds(it.bounds)
            }
            addToBoundingBox(bounds)
        }
        globalBoundsSphere.update(
            toGlobal(tempVec3f.set(bounds.center)),
            toGlobal(tmpTransformVec.set(bounds.size), 0f).length() * 0.5f,
        )
    }

    /** Override to add own update logic without worrying about not updating children. */
    protected open fun onUpdate(dt: Duration) = Unit

    private fun addToBounds(parentBounds: BoundingBox) {
        if (!bounds.isEmpty) {
            if (dirty || cachedBoundsMin != bounds.min || cachedBoundsMax != bounds.max) {
                cachedBoundsMin.set(bounds.min)
                cachedBoundsMax.set(bounds.max)

                parentBoundsCache.batchUpdate {
                    val minX = bounds.min.x
                    val minY = bounds.min.y
                    val minZ = bounds.min.z
                    val maxX = bounds.max.x
                    val maxY = bounds.max.y
                    val maxZ = bounds.max.z
                    clear()
                    add(transform.transform(tmpTransformVec.set(minX, minY, minZ)))
                    add(transform.transform(tmpTransformVec.set(minX, minY, maxZ)))
                    add(transform.transform(tmpTransformVec.set(minX, maxY, minZ)))
                    add(transform.transform(tmpTransformVec.set(minX, maxY, maxZ)))
                    add(transform.transform(tmpTransformVec.set(maxX, minY, minZ)))
                    add(transform.transform(tmpTransformVec.set(maxX, minY, maxZ)))
                    add(transform.transform(tmpTransformVec.set(maxX, maxY, minZ)))
                    add(transform.transform(tmpTransformVec.set(maxX, maxY, maxZ)))
                }
            }
            parentBounds.add(parentBoundsCache)
        }
    }

    protected open fun addToBoundingBox(bounds: BoundingBox) = Unit

    /**
     * Update the nodes transforms, if dirty. This will also invoke the parents [updateTransform]
     * call, moving up the tree.
     */
    open fun updateTransform() {
        if (!dirty) return

        val parent = parent

        parent?.updateTransform()

        _translationMatrix.setToTranslate(_localPosition)
        _rotationMatrix.setToRotation(_localRotation)
        _scaleMatrix.setToScaling(_localScale)

        _transform.set(_translationMatrix).mul(_rotationMatrix).mul(_scaleMatrix)

        if (parent != null) {
            _globalTransform.set(parent._globalTransform).mul(_transform)
            _globalRotation.set(parent._globalRotation).add(_localRotation)
            _globalScale.set(parent._globalScale).scale(_localScale.x, _localScale.y, _localScale.z)
        } else {
            _globalTransform.set(_transform)
            _globalRotation.set(_localRotation)
            _globalScale.set(_localScale)
        }
        _globalInverseDirty = true
        _globalPositionDirty = true
        _globalToLocalDirty = true

        dirty = false
    }

    /**
     * Sets the position of the [Node3D] in global space.
     *
     * @param value the new position
     * @return the current [Node3D]
     */
    fun globalPosition(value: Vec3f): Node3D {
        if (value == _globalPosition) {
            return this
        }
        _globalPosition.set(value)
        updateGlobalPosition()
        return this
    }

    private fun updateGlobalPosition() {
        _localPosition.set(_globalPosition)
        updateTransform()
        if (parent is Node3D) {
            _localPosition.mul(globalToLocalTransform)
        }
        dirty()
    }

    /**
     * Sets the position of the [Node3D] relative to the parent [Node]. If the [Node3D] has no
     * parent or if the parent node is NOT a [Node3D], then it is the same a [globalPosition]
     *
     * @param value the new position
     * @return the current [Node3D]
     */
    fun position(value: Vec3f): Node3D = position(value.x, value.y, value.z)

    /**
     * Sets the position of the [Node3D] relative to the parent [Node]. If the [Node3D] has no
     * parent or if the parent node is NOT a [Node3D], then it is the same a [globalPosition]
     *
     * @param x the new x position
     * @param y the new y position
     * @param z the new z position
     * @return the current [Node3D]
     */
    fun position(x: Float, y: Float, z: Float): Node3D {
        if (_localPosition.x == x && _localPosition.y == y && _localPosition.z == z) {
            return this
        }

        _localPosition.set(x, y, z)
        dirty()

        return this
    }

    /**
     * Sets the rotation of the [Node3D] in global space to the given Quaternion.
     *
     * @param quaternion the new quaternion
     * @return the current [Node3D]
     */
    fun globalRotation(quaternion: Quaternion): Node3D {
        if (_globalRotation == quaternion) {
            return this
        }
        _globalRotation.set(quaternion)
        updateGlobalRotation()
        return this
    }

    private fun updateGlobalRotation() {
        _localRotation.set(_globalRotation)
        parent?.let { _localRotation.add(it.globalRotation) }
        dirty()
    }

    /**
     * Sets the rotation of the [Node3D] relative to the parent [Node3D] as a Quaternion. If the
     * [Node3D] has no parent or if the parent node is NOT [Node3D], then it is the same a
     * [globalRotation]
     *
     * @param quaternion the rotation quaternion
     * @return the current [Node3D]
     */
    fun rotation(quaternion: Quaternion): Node3D {
        if (_localRotation == quaternion) {
            return this
        }
        _localRotation.set(quaternion)
        dirty()
        return this
    }

    /**
     * Sets the global scale of the [Node3D].
     *
     * @param value the new scale
     * @return the current [Node3D]
     */
    fun globalScaling(value: Vec3f): Node3D = globalScaling(value.x, value.y, value.z)

    /**
     * Sets the global scale of the [Node3D].
     *
     * @param x the new x scale
     * @param y the new y scale
     * @param z the new z scale
     * @return the current [Node3D]
     */
    fun globalScaling(x: Float, y: Float, z: Float): Node3D {
        if (_globalScale.x == x && _globalScale.y == y && _globalScale.z == z) {
            return this
        }
        _globalScale.set(x, y, z)
        updateGlobalScale()
        return this
    }

    /**
     * Sets the local scale of the [Node3D].
     *
     * @param value the new scale
     * @return the current [Node3D]
     */
    fun scaling(value: Float) = scaling(value, value, value)

    /**
     * Sets the local scale of the [Node3D].
     *
     * @param value the new scale
     * @return the current [Node3D]
     */
    fun scaling(value: Vec3f): Node3D = scaling(value.x, value.y, value.z)

    /**
     * Sets the local of the [Node3D].
     *
     * @param x the new x scale
     * @param y the new y scale
     * @param z the new z scale
     * @return the current [Node3D]
     */
    fun scaling(x: Float, y: Float, z: Float): Node3D {
        if (_localScale.x == x && _localScale.y == y && _localScale.z == z) {
            return this
        }
        _localScale.set(x, y, z)
        dirty()
        return this
    }

    private fun updateGlobalScale() {
        _localScale.set(_globalScale)
        val node3d = parent
        if (node3d != null) {
            _localScale /= node3d._globalScale
        }
        dirty()
    }

    /** Transforms [vec] in-place from local to global coordinates. */
    fun toGlobal(vec: MutableVec3f, w: Float = 1f): MutableVec3f {
        localToGlobalTransform.transform(vec, w)
        return vec
    }

    /** Transforms [vec] in-place from global to local coordinates. */
    fun toLocal(vec: MutableVec3f, w: Float = 1f): MutableVec3f {
        globalInverseTransform.transform(vec, w)
        return vec
    }

    /**
     * Translates the position by the offset vector in `local` coordinates.
     *
     * @param offset the amount to translate by
     */
    fun translate(offset: Vec3f) = translate(offset.x, offset.y, offset.z)

    /**
     * Translates the position by the offset vector in `local` coordinates.
     *
     * @param tx the amount to translate x by
     * @param ty the amount to translate y by
     * @param tz the amount to translate z by
     */
    fun translate(tx: Float, ty: Float, tz: Float): Node3D {
        if (tx != 0f || ty != 0f || tz != 0f) {
            _localPosition.add(tx, ty, tz)
            dirty()
        }
        return this
    }

    /**
     * Translates the position by the offset vector in `global` coordinates.
     *
     * @param offset the amount to translate by
     */
    fun globalTranslate(offset: Vec3f) = globalTranslate(offset.x, offset.y, offset.z)

    /**
     * Translates the position by the offset vector in `global` coordinates.
     *
     * @param tx the amount to translate x by
     * @param ty the amount to translate y by
     * @param tz the amount to translate z by
     */
    fun globalTranslate(tx: Float, ty: Float, tz: Float): Node3D {
        if (tx != 0f || ty != 0f || tz != 0f) {
            _globalPosition.add(tx, ty, tz)
            dirty()
        }
        return this
    }

    /**
     * Rotates in 'local' coordinates
     *
     * @param x the angle of the pitch
     * @param y the angle of the yaw
     * @param z the angle of the roll
     */
    fun rotate(x: Angle = Angle.ZERO, y: Angle = Angle.ZERO, z: Angle = Angle.ZERO): Node3D {
        if (x != Angle.ZERO || y != Angle.ZERO || z != Angle.ZERO) {
            tempQuat.setEuler(x, y, z)
            _localRotation.mul(tempQuat)
            dirty()
        }
        return this
    }

    fun rotate(quaternion: Quaternion): Node3D {
        _localRotation.mul(quaternion)
        dirty()
        return this
    }

    /**
     * Rotates in 'global' coordinates.
     *
     * @param x the angle of the pitch
     * @param y the angle of the yaw
     * @param z the angle of the roll
     */
    fun globalRotate(x: Angle = Angle.ZERO, y: Angle = Angle.ZERO, z: Angle = Angle.ZERO): Node3D {
        if (x != Angle.ZERO || y != Angle.ZERO || z != Angle.ZERO) {
            tempQuat.setEuler(x, y, z)
            _globalRotation.mul(tempQuat)
            parent?.let { _localRotation.set(it.globalRotation).add(_globalRotation) }
                ?: run { _localRotation.set(_globalRotation) }
            dirty()
        }
        return this
    }

    /**
     * Scales up or down by a factor in 'local' coordinates.
     *
     * @param s the scale factor
     */
    fun scale(s: Float) = scale(s, s, s)

    /**
     * Scales up or down by a factor in 'local' coordinates.
     *
     * @param sx the x-scale factor
     * @param sy the y-scale factor
     * @param sz the z-scale factor
     */
    fun scale(sx: Float, sy: Float, sz: Float): Node3D {
        if (sx != 1f || sy != 1f || sz != 1f) {
            _localScale.x *= sx
            _localScale.y *= sy
            _localScale.z *= sz
            dirty()
        }
        return this
    }

    /**
     * Scales up or down by a factor in 'global' coordinates.
     *
     * @param s the scale factor
     */
    fun globalScale(s: Float) = globalScale(s, s, s)

    /**
     * Scales up or down by a factor in 'global' coordinates.
     *
     * @param sx the x-scale factor
     * @param sy the y-scale factor
     * @param sz the z-scale factor
     */
    fun globalScale(sx: Float, sy: Float, sz: Float): Node3D {
        if (sx != 1f || sy != 1f || sz != 1f) {
            _globalScale.x *= sx
            _globalScale.y *= sy
            _globalScale.z *= sz
            updateGlobalScale()
        }
        return this
    }

    /** Set local transform to the identity matrix. */
    fun setIdentity(): Node3D {
        _localPosition.set(0f, 0f, 0f)
        _localScale.set(1f, 1f, 1f)
        _localRotation.set(0f, 0f, 0f, 1f)
        dirty()
        return this
    }

    /**
     * Destroy this node and all of its children. Once destroyed, this node should no longer be
     * used.
     */
    fun destroy() {
        _isDestroyed = true
        while (children.isNotEmpty()) {
            children[0].destroy()
        }
        onDestroy()
    }

    /** Called when [destroy] is invoked and all of its children have been destroyed. */
    open fun onDestroy() = Unit

    /** @return a tree string for all the child nodes under this [Node3D]. */
    fun treeString(): String {
        val builder = StringBuilder()
        internalToTreeString(builder, "", "")
        return builder.toString()
    }

    private fun internalToTreeString(
        builder: StringBuilder,
        prefix: String,
        childrenPrefix: String,
    ) {
        builder.run {
            append(prefix)
            append(name)
            if (name != this@Node3D::class.simpleName) {
                append(" (${this@Node3D::class.simpleName})")
            }
            appendLine()
            children.fastForEachWithIndex { index, node ->
                if (index < children.size - 1) {
                    node.internalToTreeString(builder, "$childrenPrefix├── ", "$childrenPrefix│   ")
                } else {
                    node.internalToTreeString(builder, "$childrenPrefix└── ", "$childrenPrefix    ")
                }
            }
        }
    }

    fun set(node: Node3D) {
        globalTransform = node.globalTransform
    }

    /**
     * Create a new instance of this node, copying over the name and [globalTransform], along with a
     * copy of each of its children.
     */
    open fun copy(): Node3D {
        val copy =
            Node3D().also {
                it.name = name
                it.frustumCulled = frustumCulled
                it.globalTransform = globalTransform
            }
        children.fastForEach { child -> copy.addChild(child.copy()) }
        return copy
    }

    /** Filter all descendants, into a flatten list, by type. Excludes the current node. */
    fun <T : Node3D> filterDescendantsByType(type: KClass<T>): List<T> {
        val result = mutableListOf<T>()
        children.fastForEach { result.addAll(it.filterByType(type)) }
        return result
    }

    /** Filter all descendants, into a flatten list, by type. Includes the current node. */
    fun <T : Node3D> Node3D.filterByType(type: KClass<T>): List<T> {
        val result = mutableListOf<T>()

        if (type.isInstance(this)) {
            @Suppress("UNCHECKED_CAST") result.add(this as T)
        }

        children.fastForEach { result.addAll(it.filterByType(type)) }

        return result
    }

    companion object {
        private val tempQuat = MutableQuaternion()
        private val tempVec3f = MutableVec3f()
    }
}
