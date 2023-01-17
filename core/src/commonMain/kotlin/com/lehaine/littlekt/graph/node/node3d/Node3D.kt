package com.lehaine.littlekt.graph.node.node3d

import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.math.*
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.util.LazyMat4

/**
 * Based off of the kool engine implementation.
 * @author Colton Daily
 * @date 12/17/2022
 */
open class Node3D : Node() {
    /**
     * Global transform. Product of [_transform] and the [_transform] of the parent [Node3D].
     */
    val globalTransform: Mat4
        get() {
            updateTransform()
            return _globalTransform
        }


    private val _globalTransform = Mat4()

    val globalToLocalTransform: Mat4
        get() {
            (parent as? Node3D)?.let {
                it.updateTransform()
                _globalToLocalTransform.set(it.globalInverseTransform)
            } ?: run {
                _globalToLocalTransform.setToIdentity()
            }
            return _globalToLocalTransform
        }

    private val _globalToLocalTransform = Mat4()

    /**
     * Local transform based on translation, scale, and rotation.
     */
    val transform: Mat4
        get() {
            updateTransform()
            return _transform
        }

    private val _transform = Mat4()

    /**
     * The position of the [Node3D] in global space. If you want to set the [x,y] properties of this [Vec3f] then use
     * the [globalX], [globalY], and [globalZ] properties of this [Node3D]
     */
    var globalPosition: Vec3f
        get() {
            updateTransform()
            (parent as? Node3D)?.let {
                _globalPosition.set(_localPosition).mul(it._globalPosition)
            } ?: run {
                _globalPosition.set(_localPosition)
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
            if (value == _globalPosition.x) return
            _globalPosition.x = value
            updateGlobalPosition()
        }
    var globalY: Float
        get() {
            return globalPosition.y
        }
        set(value) {
            if (value == _globalPosition.y) return
            _globalPosition.y = value
            updateGlobalPosition()
        }
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
     * The position of the [Node3D] relative to the parent transform. If the [Node3D] has no parent or if the parent node is NOT
     * a [Node3D], then it is the same a [globalPosition]. If you want to set the [x,y] properties of this [Vec3f] then use
     * the [x], [y], and [z] properties of this [Node3D]
     */
    var position: Vec3f
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
            setDirty()
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
            setDirty()
        }
    var z: Float
        get() {
            return position.z
        }
        set(value) {
            if (value == _localPosition.z) {
                return
            }
            _localPosition.z = value
            setDirty()
        }


    /**
     * The rotation of the [Node3D] in global space as a Quaternion.
     */
    var globalRotation: Vec4f
        get() {
            updateTransform()
            return _globalRotation
        }
        set(value) {
            globalRotation(value)
        }


    /**
     * The rotation of the [Node3D] relative to the parent transform's rotation as a Quaternion.
     * If the [Node3D] has no parent or if the parent node is NOT a [Node3D], then it is the same a [globalRotation]
     */
    var rotation: Vec4f
        get() {
            updateTransform()
            return _localRotation
        }
        set(value) {
            rotation(value)
        }

    /**
     * The global scale of the [Node3D]. If you want to set the [x,y] properties of this [Vec3f] then use
     * the [globalScaleX], [globalScaleY], [globalScaleZ] properties of this [Node3D].
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
     * @see globalScaling
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
     * The global y-scale of the [Node3D].
     * @see globalScaling
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
     * The global z-scale of the [Node3D].
     * @see globalScaling
     */
    var globalScaleZ: Float
        get() {
            return _globalScale.z
        }
        set(value) {
            _globalScale.z = value
            updateScale()
        }


    /**
     * The scale of the [Node3D] relative to the parent transform's scales. If the [Node3D] has no parent or if the parent node is NOT
     * a [Node3D], then it is the same a [globalScaling]. If you want to set the [x,y] properties of this [Vec3f] then use
     * the [scaleX], [scaleY], [scaleZ] properties of this [Node3D].
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
     * @see scaling
     */
    var scaleX: Float
        get() {
            return _localScale.x
        }
        set(value) {
            if (_localScale.x == value) return
            _localScale.x = value
            setDirty()
        }

    /**
     * The y-scale of the [Node3D] relative to the parent transform's scales.
     * @see scaling
     */
    var scaleY: Float
        get() {
            return _localScale.y
        }
        set(value) {
            if (_localScale.y == value) return
            _localScale.y = value
            setDirty()
        }

    /**
     * The z-scale of the [Node3D] relative to the parent transform's scales.
     * @see scaling
     */
    var scaleZ: Float
        get() {
            return _localScale.z
        }
        set(value) {
            if (_localScale.z == value) return
            _localScale.z = value
            setDirty()
        }

    private val globalInverseLazy = LazyMat4 { _globalTransform.invert(it) }
    private val globalInverseTransform: Mat4
        get() = globalInverseLazy.get()

    private val invTransform = LazyMat4 { _transform.invert(it) }

    private val _globalPosition = MutableVec3f()
    private val _globalScale = MutableVec3f(1f, 1f, 1f)
    private val _globalRotation = MutableVec4f(0f, 0f, 0f, 1f)

    private val _localPosition = MutableVec3f()
    private val _localScale = MutableVec3f(1f, 1f, 1f)
    private val _localRotation = MutableVec4f(0f, 0f, 0f, 1f)

    private var _rotationMatrix = Mat4()
    private var _translationMatrix = Mat4()
    private var _scaleMatrix = Mat4()

    private val tmpTransformVec = MutableVec3f()

    fun setDirty() {
        invTransform.isDirty = true
    }


    fun updateTransform() {
        if (!invTransform.isDirty) return

        val parent = parent as? Node3D

        parent?.updateTransform()

        _translationMatrix.setToTranslate(_localPosition)
        _rotationMatrix.setToRotation(_localRotation)
        _scaleMatrix.setToScaling(_localScale)

        _transform.set(_translationMatrix).mul(_rotationMatrix).mul(_scaleMatrix)

        if (parent != null) {
            _globalTransform.set(parent._globalTransform).mul(_transform)
            _globalRotation.set(parent._globalRotation).add(_localRotation)
            _globalScale.set(parent._globalScale).set(_localScale)

        } else {
            _globalTransform.set(_transform)
            _globalRotation.set(_localRotation)
            _globalScale.set(_localScale)
        }
    }

    fun getTransform(result: Mat4): Mat4 = result.set(_transform)

    fun getInverseTransform(result: Mat4): Mat4 {
        return result.set(invTransform.get())
    }

    /**
     * Sets the position of the [Node3D] in global space.
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
        setDirty()
    }

    /**
     * Sets the position of the [Node3D] relative to the parent [Node]. If the [Node3D] has no parent or if the parent node is NOT
     * a [Node3D], then it is the same a [globalPosition]
     * @param value the new position
     * @return the current [Node3D]
     */
    fun position(value: Vec3f): Node3D = position(value.x, value.y, value.z)


    /**
     * Sets the position of the [Node3D] relative to the parent [Node]. If the [Node3D] has no parent or if the parent node is NOT
     * a [Node3D], then it is the same a [globalPosition]
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
        setDirty()

        return this
    }

    /**
     * Sets the rotation of the [Node3D] in global space to the given Quaternion.
     * @param quaternion the new quaternion
     * @return the current [Node3D]
     */
    fun globalRotation(quaternion: Vec4f): Node3D {
        if (_globalRotation == quaternion) {
            return this
        }
        _globalRotation.set(quaternion)
        (parent as? Node3D)?.let {
            _localRotation.set(it.globalRotation).add(quaternion)
        } ?: run {
            _localRotation.set(quaternion)
        }

        setDirty()

        return this
    }

    /**
     * Sets the rotation of the [Node3D] relative to the parent [Node3D] as a Quaternion.
     * If the [Node3D] has no parent or if the parent node is NOT [Node3D], then it is the same a [globalRotation]
     * @param quaternion the rotation quaternion
     * @return the current [Node3D]
     */
    fun rotation(quaternion: Vec4f): Node3D {
        if (_localRotation == quaternion) {
            return this
        }
        _localRotation.set(quaternion)
        setDirty()
        return this
    }

    /**
     * Sets the global scale of the [Node3D].
     * @param value the new scale
     * @return the current [Node3D]
     */
    fun globalScaling(value: Vec3f): Node3D {
        if (_globalScale == value) {
            return this
        }
        _globalScale.set(value)
        updateScale()
        return this
    }

    /**
     * Sets the global scale of the [Node3D].
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
        updateScale()
        return this
    }

    /**
     * Sets the local scale of the [Node3D].
     * @param value the new scale
     * @return the current [Node3D]
     */
    fun scaling(value: Float) = scaling(value, value, value)

    /**
     * Sets the local scale of the [Node3D].
     * @param value the new scale
     * @return the current [Node3D]
     */
    fun scaling(value: Vec3f): Node3D {
        if (_globalScale == value) {
            return this
        }
        _localScale.set(value)
        setDirty()
        return this
    }

    /**
     * Sets the local of the [Node3D].
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
        setDirty()
        return this
    }


    private fun updateScale() {
        _localScale.set(_globalScale)
        val node3d = parent as? Node3D
        if (node3d != null) {
            _localScale /= node3d._globalScale
        }
        setDirty()
    }

    /**
     * Transforms [vec] in-place from local to global coordinates.
     */
    fun toGlobal(vec: MutableVec3f, w: Float = 1f): MutableVec3f {
        _globalTransform.transform(vec, w)
        return vec
    }

    /**
     * Transforms [vec] in-place from global to local coordinates.
     */
    fun toLocal(vec: MutableVec3f, w: Float = 1f): MutableVec3f {
        globalInverseTransform.transform(vec, w)
        return vec
    }

    /**
     * Translates the position by the offset vector in `local` coordinates.
     * @param offset the amount to translate by
     */
    fun translate(offset: Vec3f) = translate(offset.x, offset.y, offset.z)

    /**
     * Translates the position by the offset vector in `local` coordinates.
     * @param tx the amount to translate x by
     * @param ty the amount to translate y by
     * @param tz the amount to translate z by
     */
    fun translate(tx: Float, ty: Float, tz: Float): Node3D {
        if (tx != 0f || ty != 0f || tz != 0f) {
            _localPosition.add(tx, ty, tz)
            setDirty()
        }
        return this
    }

    /**
     * Translates the position by the offset vector in `global` coordinates.
     * @param offset the amount to translate by
     */
    fun globalTranslate(offset: Vec3f) = globalTranslate(offset.x, offset.y, offset.z)

    /**
     * Translates the position by the offset vector in `global` coordinates.
     * @param tx the amount to translate x by
     * @param ty the amount to translate y by
     * @param tz the amount to translate z by
     */
    fun globalTranslate(tx: Float, ty: Float, tz: Float): Node3D {
        if (tx != 0f || ty != 0f || tz != 0f) {
            _globalPosition.add(tx, ty, tz)
            setDirty()
        }
        return this
    }

    /**
     * Rotates in 'local' coordinates
     * @param x the angle of the pitch
     * @param y the angle of the yaw
     * @param z the angle of the roll
     */
    fun rotate(x: Angle = Angle.ZERO, y: Angle = Angle.ZERO, z: Angle = Angle.ZERO): Node3D {
        if (x != Angle.ZERO || y != Angle.ZERO || z != Angle.ZERO) {
            tempQuat.setEuler(x, y, z)
            _localRotation.quatMul(tempQuat)
            setDirty()
        }
        return this
    }

    /**
     * Rotates in 'global' coordinates.
     * @param x the angle of the pitch
     * @param y the angle of the yaw
     * @param z the angle of the roll
     */
    fun globalRotate(x: Angle = Angle.ZERO, y: Angle = Angle.ZERO, z: Angle = Angle.ZERO): Node3D {
        if (x != Angle.ZERO || y != Angle.ZERO || z != Angle.ZERO) {
            tempQuat.setEuler(x, y, z)
            _globalRotation.quatMul(tempQuat)
            setDirty()
        }
        return this
    }

    /**
     * Scales up or down by a factor in 'local' coordinates.
     * @param s the scale factor
     */
    fun scale(s: Float) = scale(s, s, s)


    /**
     * Scales up or down by a factor in 'local' coordinates.
     * @param sx the x-scale factor
     * @param sy the y-scale factor
     * @param sz the z-scale factor
     */
    fun scale(sx: Float, sy: Float, sz: Float): Node3D {
        if (sx != 1f || sy != 1f || sz != 1f) {
            _localScale.x *= sx
            _localScale.y *= sy
            _localScale.z *= sz
            setDirty()
        }
        return this
    }

    /**
     * Scales up or down by a factor in 'global' coordinates.
     * @param s the scale factor
     */
    fun globalScale(s: Float) = globalScale(s, s, s)


    /**
     * Scales up or down by a factor in 'global' coordinates.
     * @param sx the x-scale factor
     * @param sy the y-scale factor
     * @param sz the z-scale factor
     */
    fun globalScale(sx: Float, sy: Float, sz: Float): Node3D {
        if (sx != 1f || sy != 1f || sz != 1f) {
            _globalScale.x *= sx
            _globalScale.y *= sy
            _globalScale.z *= sz
            updateScale()
        }
        return this
    }


    fun setIdentity(): Node3D {
        _localPosition.set(0f, 0f, 0f)
        _localScale.set(1f, 1f, 1f)
        _localRotation.set(0f, 0f, 0f, 1f)
        invTransform.clear()
        setDirty()
        return this
    }

    companion object {
        private val tempQuat = MutableVec4f(0f, 0f, 0f, 1f)
    }

}