package com.littlekt.graphics.g3d

import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.util.IndexedMeshGeometry
import com.littlekt.graphics.webgpu.PrimitiveTopology
import com.littlekt.math.Ray
import com.littlekt.math.spatial.BoundingBox
import com.littlekt.util.datastructure.fastForEach

/**
 * @author Colton Daily
 * @date 1/15/2025
 */
open class MeshPrimitiveInstance : Node3D() {
    var color: Color = Color.WHITE
        set(value) {
            field = value
            dirty()
        }

    override var frustumCulled: Boolean = true

    /** Don't set this */
    var instanceOf: MeshPrimitive? = null

    internal var instanceIndex = -1
    internal var dirtyQueued = false
    internal var visible = false

    internal var boundsOverride: BoundingBox? = null

    internal var skinFrame = 0
    internal var skinNextFrame = 0
    internal var skinBlend = 0f

    /**
     * Sets which joint matrices of the skin palette this instance is drawn with, blending from
     * [frame] to [nextFrame] by [blend]. Each frame is the index of its first joint matrix.
     */
    internal fun setSkinFrames(frame: Int, nextFrame: Int, blend: Float) {
        if (skinFrame == frame && skinNextFrame == nextFrame && skinBlend == blend) return
        skinFrame = frame
        skinNextFrame = nextFrame
        skinBlend = blend
        instanceOf?.instanceDirty(this)
    }

    override fun dirty() {
        super.dirty()
        instanceOf?.instanceDirty(this)
    }

    fun addTo(meshPrimitive: MeshPrimitive) {
        meshPrimitive.addInstance(this)
    }

    fun remove() {
        instanceOf?.removeInstance(this)
    }

    override fun color(color: Color) {
        this.color = color
        super.color(color)
    }

    override fun addToBoundingBox(bounds: BoundingBox) {
        super.addToBoundingBox(bounds)
        val override = boundsOverride
        if (override != null) bounds.add(override)
        else instanceOf?.let { bounds.add(it.mesh.geometry.bounds) }
    }

    override fun intersectGeometry(ray: Ray): Float {
        val primitive = instanceOf ?: return Ray.NO_HIT
        if (primitive.topology != PrimitiveTopology.TRIANGLE_LIST) return Ray.NO_HIT
        val geometry = primitive.mesh.geometry
        val position =
            geometry.layout.attributes.firstOrNull { it.usage == VertexAttrUsage.POSITION }
                ?: return Ray.NO_HIT
        val local = localRay.setTransformed(ray, globalInverseTransform)
        if (local.intersectBox(geometry.bounds) < 0f) return Ray.NO_HIT
        val vertices = geometry.vertices
        val stride = geometry.vertexStride
        val offset = position.offset.toInt()
        val indexed = (geometry as? IndexedMeshGeometry)?.takeIf { it.numIndices > 0 }
        val indices = indexed?.indices
        val count = indexed?.numIndices ?: geometry.numVertices
        var best = Ray.NO_HIT
        var i = 0
        while (i + 2 < count) {
            val a = (if (indices != null) indices[i].toInt() and 0xFFFF else i) * stride + offset
            val b = (if (indices != null) indices[i + 1].toInt() and 0xFFFF else i + 1) * stride + offset
            val c = (if (indices != null) indices[i + 2].toInt() and 0xFFFF else i + 2) * stride + offset
            val hit =
                local.intersectTriangle(
                    vertices.getFloat(a),
                    vertices.getFloat(a + 4),
                    vertices.getFloat(a + 8),
                    vertices.getFloat(b),
                    vertices.getFloat(b + 4),
                    vertices.getFloat(b + 8),
                    vertices.getFloat(c),
                    vertices.getFloat(c + 4),
                    vertices.getFloat(c + 8),
                )
            if (hit >= 0f && (best < 0f || hit < best)) best = hit
            i += 3
        }
        return best
    }

    override fun forEachMeshPrimitive(action: (MeshPrimitive) -> Unit) {
        instanceOf?.markInstanceVisible(this)
        instanceOf?.let(action)
        children.fastForEach { it.forEachMeshPrimitive(action) }
    }

    override fun forEachMeshPrimitive(camera: Camera, action: (MeshPrimitive) -> Unit) {
        if (frustumCulled) {
            val instanceOf = instanceOf
            if (instanceOf != null) {
                val inFrustum =
                    camera.sphereInFrustum(globalBoundsSphere.center, globalBoundsSphere.radius)
                if (inFrustum) {
                    instanceOf.markInstanceVisible(this)
                    action(instanceOf)
                }
            }
        } else {
            instanceOf?.markInstanceVisible(this)
            instanceOf?.let(action)
        }
        children.fastForEach { it.forEachMeshPrimitive(camera, action) }
    }

    /**
     * Copying a [MeshPrimitiveInstance] will reuse the underlying primitive, if applicable. In
     * other words, this will create an instance of the underlying geometry. Just the fact of
     * copying this node will update the [MeshPrimitive] instance data. This node will still need to
     * have [update] called and passed into the renderer to handle any frustum related updates.
     */
    override fun copy(): Node3D {
        val copy =
            MeshPrimitiveInstance().also {
                it.name = name
                it.frustumCulled = frustumCulled
                it.globalTransform = globalTransform
                instanceOf?.let { instance -> it.addTo(instance) }
            }
        children.fastForEach { child -> copy.addChild(child.copy()) }
        return copy
    }

    override fun onDestroy() {
        super.onDestroy()
        remove()
    }

    private companion object {
        val localRay = Ray()
    }
}
