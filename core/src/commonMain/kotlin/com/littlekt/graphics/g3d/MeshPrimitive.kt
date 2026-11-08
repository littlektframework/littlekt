package com.littlekt.graphics.g3d

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.IndexedMesh
import com.littlekt.graphics.Mesh
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.g3d.skin.Skin
import com.littlekt.graphics.g3d.util.MaterialPipeline
import com.littlekt.graphics.webgpu.IndexFormat
import com.littlekt.graphics.webgpu.PrimitiveTopology
import com.littlekt.log.Logger
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.datastructure.fastForEachWithIndex

/**
 * A mesh primitive holds an instance of a [Mesh] and a [Material] with some rasterizing info such
 * as topology and the index format. This class also handles instancing of the [Mesh].
 *
 * @param instanceSize pre-allocate this number of instances in the instance data buffer. Defaults
 *   to `0`.
 * @author Colton Daily
 * @date 11/25/2024
 */
open class MeshPrimitive(
    val mesh: Mesh<*>,
    var material: Material,
    val topology: PrimitiveTopology = PrimitiveTopology.TRIANGLE_LIST,
    val stripIndexFormat: IndexFormat? = null,
    var skin: Skin? = null,
    var morphWeights: FloatArray? = null,
    instanceSize: Int = 0,
) : Releasable {

    /**
     * A pre-cast of the underlying [mesh] as an [IndexedMesh], if applicable. If this value is
     * `null`, then the underlying mesh is not an [IndexedMesh].
     */
    val indexedMesh = mesh as? IndexedMesh<*>

    /** if `true` then [instanceCount] is `> 1`; `false` otherwise. */
    val isInstanced: Boolean
        get() = instances.size > 1

    /**
     * The amount of all instances allocated in the staging buffer. If this is `0` then it is not
     * drawing. A size `> 1` will be marked as instanced.
     */
    val instanceCount: Int
        get() = instances.size

    /**
     * The amount of instances that are visible in the frustum. This does not necessarily match
     * [instanceCount] and can be `0`.
     */
    val visibleInstanceCount: Int
        get() = visibleInstances.size

    internal var queuedFlush = -1L
    internal var queuedPipeline: MaterialPipeline? = null
    internal var drawnFlush = -1L
    internal var drawnPipeline: MaterialPipeline? = null

    internal val pipelineCache = Array(PIPELINE_CACHE_SIZE) { PipelineCacheEntry() }
    internal var pipelineCacheNext = 0

    private val instances = mutableListOf<MeshPrimitiveInstance>()
    private var instanceData = FloatBuffer((instanceSize + 1) * TRANSFORM_COMPONENTS_PER_INSTANCE)
    private val visibleInstances = mutableListOf<MeshPrimitiveInstance>()
    private val dirtyInstances = mutableListOf<MeshPrimitiveInstance>()

    init {
        if (
            topology == PrimitiveTopology.TRIANGLE_STRIP || topology == PrimitiveTopology.LINE_STRIP
        ) {
            check(stripIndexFormat != null) {
                error("VisualInstance.stripIndexFormat is required to be set for strip topologies!")
            }
        }
    }

    /**
     * Marks the designated [MeshPrimitiveInstance] as dirty. A dirty instance will write the
     * transform data to the buffer at update time.
     */
    fun instanceDirty(instance: MeshPrimitiveInstance) {
        if (instance.dirtyQueued || instance.instanceOf !== this) return
        instance.dirtyQueued = true
        dirtyInstances += instance
    }

    /**
     * Writes the data of each visible instance into [dst] starting at [dstOffset]. Any dirty
     * instances are updated beforehand.
     *
     * @return the number of instances written.
     */
    fun writeVisibleInstanceData(dst: FloatBuffer, dstOffset: Int): Int {
        if (dirtyInstances.isNotEmpty()) {
            dirtyInstances.fastForEach { dirtyInstance ->
                dirtyInstance.dirtyQueued = false
                updateInstance(dirtyInstance)
            }
            dirtyInstances.clear()
        }
        visibleInstances.fastForEachWithIndex { index, instance ->
            dst.put(
                instanceData,
                dstOffset = dstOffset + index * TRANSFORM_COMPONENTS_PER_INSTANCE,
                srcOffset = instance.instanceIndex * TRANSFORM_COMPONENTS_PER_INSTANCE,
                len = TRANSFORM_COMPONENTS_PER_INSTANCE,
            )
        }
        return visibleInstances.size
    }

    /**
     * Adds the designated [MeshPrimitiveInstance] as an instance. The instance is assigned an
     * internal index and is tracked. If a [MeshPrimitiveInstance] changes, on must mark it dirty
     * with [instanceDirty]. By default, a [MeshPrimitiveInstance] will mark itself dirty via the
     * [Node3D.dirty] function.
     */
    fun addInstance(instance: MeshPrimitiveInstance) {
        if (instance.instanceOf === this) return
        if (instance.instanceOf != null) {
            logger.warn {
                "MeshPrimitiveInstance is already an instance of another MeshPrimitive. Remove old instance before adding new one."
            }
            return
        }
        val requiredSize = (instances.size + 1) * TRANSFORM_COMPONENTS_PER_INSTANCE
        if (instanceData.capacity < requiredSize) {
            val newData = FloatBuffer(maxOf(instanceData.capacity * 2, requiredSize))
            newData.put(
                instanceData,
                dstOffset = 0,
                srcOffset = 0,
                len = instances.size * TRANSFORM_COMPONENTS_PER_INSTANCE,
            )
            instanceData = newData
        }
        instance.instanceIndex = instances.size
        instance.instanceOf = this
        instances += instance
        instanceDirty(instance)
    }

    /** Removes the designated [MeshPrimitiveInstance] as an instance. */
    fun removeInstance(instance: MeshPrimitiveInstance) {
        if (instance.instanceOf !== this) return
        val removeIdx = instance.instanceIndex
        val lastIdx = instances.lastIndex
        if (removeIdx != lastIdx) {
            val last = instances[lastIdx]
            instances[removeIdx] = last
            last.instanceIndex = removeIdx
            instanceData.put(
                instanceData,
                dstOffset = removeIdx * TRANSFORM_COMPONENTS_PER_INSTANCE,
                srcOffset = lastIdx * TRANSFORM_COMPONENTS_PER_INSTANCE,
                len = TRANSFORM_COMPONENTS_PER_INSTANCE,
            )
        }
        instances.removeAt(lastIdx)
        if (instance.dirtyQueued) {
            dirtyInstances -= instance
        }
        if (instance.visible) {
            visibleInstances -= instance
        }
        instance.reset()
    }

    /**
     * Update the CPU transfer storage buffer for the following [instance], as long as it has been
     * added via [addInstance], to prepare it for writing to the GPU buffer.
     */
    fun updateInstance(instance: MeshPrimitiveInstance) {
        if (instance.instanceOf !== this) {
            logger.warn { "Instance does not exist to be updated!" }
            return
        }

        instanceData.position = instance.instanceIndex * TRANSFORM_COMPONENTS_PER_INSTANCE
        instanceData.put(instance.globalTransform.data)
        instanceData.put(instance.color.fields)
    }

    /** Marks the [MeshPrimitiveInstance] as visible to be rendered in a model batch. */
    fun markInstanceVisible(instance: MeshPrimitiveInstance) {
        if (instance.instanceOf !== this) {
            logger.warn {
                "Attempting to mark an instance as visible when it hasn't been added yet! Ignoring..."
            }
            return
        }
        if (instance.visible) return
        instance.visible = true
        visibleInstances += instance
    }

    /** Removes all visible [MeshPrimitiveInstance]. */
    fun resetVisibility() {
        visibleInstances.fastForEach { it.visible = false }
        visibleInstances.clear()
    }

    /**
     * Clears all instance data. Any existing [MeshPrimitiveInstance] tied to this primitive will
     * need to be added back via [addInstance].
     */
    fun clearInstances() {
        instances.fastForEach { it.reset() }
        instanceData.clear()
        instances.clear()
        dirtyInstances.clear()
        visibleInstances.clear()
    }

    private fun MeshPrimitiveInstance.reset() {
        instanceOf = null
        instanceIndex = -1
        dirtyQueued = false
        visible = false
    }

    override fun release() {
        clearInstances()
    }

    companion object {
        private const val TRANSFORM_COMPONENTS_PER_INSTANCE = 20
        private const val PIPELINE_CACHE_SIZE = 4

        /** The number of floats each instance uses. */
        const val INSTANCE_DATA_SIZE = TRANSFORM_COMPONENTS_PER_INSTANCE
        private val logger: Logger = Logger<MeshPrimitive>()
    }
}

internal class PipelineCacheEntry {
    var pipeline: MaterialPipeline? = null
    var material: Material? = null
    var environment: Environment? = null
    var batch: ModelBatch? = null
    var version = -1
    var materialKey = 0
}
