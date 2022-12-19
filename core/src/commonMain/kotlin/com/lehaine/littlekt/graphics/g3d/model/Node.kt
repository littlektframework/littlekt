package com.lehaine.littlekt.graphics.g3d.model

import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.MutableVec3f
import com.lehaine.littlekt.math.Vec3f
import com.lehaine.littlekt.math.spatial.BoundingBox
import com.lehaine.littlekt.util.LazyMat4

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
open class Node(val name: String? = null) : Disposable {
    protected val intChildren = mutableListOf<Node>()
    protected val childrenBounds = BoundingBox()
    val children: List<Node> get() = intChildren
    val size: Int get() = intChildren.size

    /**
     * Axis-aligned bounds of this node in local coordinates.
     * Implementations should set and refresh their bounds on every frame if applicable.
     */
    val bounds = BoundingBox()

    /**
     * Center point of this node's bounds in global coordinates.
     */
    val globalCenter: Vec3f get() = globalCenterMut

    /**
     * Radius of this node's bounding sphere in global coordinates.
     */
    var globalRadius = 0f
        protected set


    protected val globalCenterMut = MutableVec3f()
    protected val globalExtentMut = MutableVec3f()

    val modelMat = Mat4()

    private val modelMatInvLazy = LazyMat4 { modelMat.invert(it) }
    val modelMatInv: Mat4
        get() = modelMatInvLazy.get()

    val transform = Mat4()

    protected val invTransform = LazyMat4 { transform.invert(it) }
    protected var isIdentity = false

    private val tmpTransformVec = MutableVec3f()
    private val tmpBounds = BoundingBox()

    /**
     * Parent node is set when this node is added to another [Node]
     */
    var parent: Node? = null

    /**
     * Determines the visibility of this node. If visible is false this node will be skipped on
     * rendering.
     */
    var isVisible = true

    /**
     * Determines whether this node is considered for ray-picking tests.
     */
    var isPickable = true

    /**
     * Determines whether this node is checked for visibility during rendering. If true the node is only rendered
     * if it is within the camera frustum.
     */
    var isFrustumChecked = true

    init {
        setIdentity()

        // be default, frustum culling is disabled for groups. Potential benefit is rather small, and it can cause
        // problems if group content is not frustum checked as well (e.g. an instaned mesh)
        isFrustumChecked = false
    }

    fun setDirty() {
        invTransform.isDirty = true
        isIdentity = false
    }

    fun update() {
        updateModelMat()

        // update global center and radius
        toGlobalCoords(globalCenterMut.set(bounds.center))
        toGlobalCoords(globalExtentMut.set(bounds.max))
        globalRadius = globalCenter.distance(globalExtentMut)

        // call update on all children and update group bounding box
        childrenBounds.clear()
        for (i in intChildren.indices) {
            intChildren[i].update()
            childrenBounds.add(intChildren[i].bounds)
        }

        // update bounds based on updated children bounds
        setLocalBounds()
        globalCenterMut.set(bounds.center)
        globalExtentMut.set(bounds.max)
        modelMat.transform(globalCenterMut)
        modelMat.transform(globalExtentMut)
        globalRadius = globalCenter.distance(globalExtentMut)

        // transform group bounds
        if (!bounds.isEmpty && !isIdentity) {
            tmpBounds.clear()
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.min.x, bounds.min.y, bounds.min.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.min.x, bounds.min.y, bounds.max.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.min.x, bounds.max.y, bounds.min.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.min.x, bounds.max.y, bounds.max.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.max.x, bounds.min.y, bounds.min.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.max.x, bounds.min.y, bounds.max.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.max.x, bounds.max.y, bounds.min.z), 1f))
            tmpBounds.add(transform.transform(tmpTransformVec.set(bounds.max.x, bounds.max.y, bounds.max.z), 1f))
            bounds.set(tmpBounds)
        }
    }

    fun setLocalBounds() {
        bounds.set(childrenBounds)
    }

    fun updateModelMat() {
        modelMat.set(parent?.modelMat ?: MODEL_MAT_IDENTITY)
        modelMatInvLazy.isDirty = true

        if (!isIdentity) {
            modelMat.mul(transform)
        }
    }

    open fun render(shader: ShaderProgram<*, *>) {
        for (i in intChildren.indices) {
            intChildren[i].render(shader)
        }
    }

    fun getTransform(result: Mat4): Mat4 = result.set(transform)

    fun getInverseTransform(result: Mat4): Mat4 {
        return result.set(invTransform.get())
    }

    fun addNode(node: Node, index: Int = -1) {
        if (index >= 0) {
            intChildren.add(index, node)
        } else {
            intChildren.add(node)
        }
        node.parent = this
        bounds.add(node.bounds)
    }

    fun <R : Comparable<R>> sortChildrenBy(selector: (Node) -> R) {
        intChildren.sortBy(selector)
    }

    fun removeNode(node: Node): Boolean {
        if (intChildren.remove(node)) {
            node.parent = null
            return true
        }
        return false
    }

    fun removeAllChildren() {
        for (i in intChildren.indices) {
            intChildren[i].parent = null
        }
        intChildren.clear()
    }

    /**
     * Transforms [vec] in-place from local to global coordinates.
     */
    fun toGlobalCoords(vec: MutableVec3f, w: Float = 1f): MutableVec3f {
        modelMat.transform(vec, w)
        return vec
    }

    /**
     * Transforms [vec] in-place from global to local coordinates.
     */
    fun toLocalCoords(vec: MutableVec3f, w: Float = 1f): MutableVec3f {
        modelMatInv.transform(vec, w)
        return vec
    }

    operator fun plusAssign(node: Node) {
        addNode(node)
    }

    operator fun minusAssign(node: Node) {
        removeNode(node)
    }

    operator fun Node.unaryPlus() {
        addNode(this)
    }

    operator fun Node.unaryMinus() =
        removeNode(this)

    fun translate(t: Vec3f) = translate(t.x, t.y, t.z)

    fun translate(tx: Float, ty: Float, tz: Float): Node {
        transform.translate(tx, ty, tz)
        setDirty()
        return this
    }


    fun rotate(angleDeg: Float, axis: Vec3f) = rotate(angleDeg, axis.x, axis.y, axis.z)

    fun rotate(angleDeg: Float, axX: Float, axY: Float, axZ: Float): Node {
        transform.rotate(angleDeg, axX, axY, axZ)
        setDirty()
        return this
    }

    fun scale(s: Float) = scale(s, s, s)

    fun scale(sx: Float, sy: Float, sz: Float): Node {
        transform.scale(sx, sy, sz)
        setDirty()
        return this
    }

    fun mul(mat: Mat4): Node {
        transform.mul(mat)
        setDirty()
        return this
    }

    fun set(mat: Mat4): Node {
        transform.set(mat)
        setDirty()
        return this
    }


    fun setIdentity(): Node {
        transform.setToIdentity()
        invTransform.clear()
        isIdentity = true
        return this
    }

    override fun dispose() = Unit

    companion object {
        private val MODEL_MAT_IDENTITY = Mat4()
    }

}