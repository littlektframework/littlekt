package com.littlekt.graphics.g3d

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.IndexedMesh
import com.littlekt.graphics.Mesh
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.g3d.skin.Skin
import com.littlekt.graphics.g3d.util.InstanceBuffers
import com.littlekt.graphics.webgpu.IndexFormat
import com.littlekt.graphics.webgpu.PrimitiveTopology
import com.littlekt.log.Logger

/**
 * A mesh primitive holds an instance of a [Mesh] and a [Material] with some rasterizing info such
 * as topology and the index format. This class also handles instancing of the [Mesh].
 *
 * @param instanceSize pre-allocate this number of instances in the instance data storage buffer.
 *   Defaults to `0`.
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
     * The size of all instances. If this is `0` then it is not drawing. A size `> 1` will be marked
     * as instanced.
     */
    val instanceCount: Int
        get() = instances.size

    /** The */
    val instanceBuffers: InstanceBuffers =
        InstanceBuffers(mesh.device, (instanceSize + 1) * TRANSFORM_COMPONENTS_PER_INSTANCE)

    private val instances = mutableListOf<MeshPrimitiveInstance>()
    private var instancesDirty = true
    private var instanceData = FloatBuffer((instanceSize + 1) * TRANSFORM_COMPONENTS_PER_INSTANCE)
    private val instanceIndices = mutableMapOf<InstanceId, Int>()
    private val instancesToId = mutableMapOf<MeshPrimitiveInstance, InstanceId>()
    private val dirtyInstances = mutableListOf<MeshPrimitiveInstance>()
    private var lastInstanceId: InstanceId = 0
    private val nextInstanceId: InstanceId
        get() = lastInstanceId++

    init {
        if (
            topology == PrimitiveTopology.TRIANGLE_STRIP || topology == PrimitiveTopology.LINE_STRIP
        ) {
            check(stripIndexFormat != null) {
                error("VisualInstance.stripIndexFormat is required to be set for strip topologies!")
            }
        }
    }

    // override fun dirty() {
    //     super.dirty()
    //     instancesDirty = true
    // }

    /**
     * Marks the designated [MeshPrimitiveInstance] as dirty. A dirty instance will write the
     * transform data to the buffer at update time.
     */
    fun instanceDirty(instance: MeshPrimitiveInstance) {
        if (dirtyInstances.contains(instance)) return
        instancesDirty = true
        dirtyInstances += instance
    }

    /** Write instance data to the underlying GPU buffer, if any instance data is dirty. */
    fun writeInstanceDataToBuffer() {
        if (instancesDirty) {
            dirtyInstances.forEach { dirtyInstance -> updateInstance(dirtyInstance) }
            dirtyInstances.clear()
            instanceBuffers.updateStaticStorage(instanceData)
            instancesDirty = false
        }
    }

    /**
     * Adds the designated [MeshPrimitiveInstance] as an instance. The instance is assigned an
     * internal ID and is tracked. If a [MeshPrimitiveInstance] changes, on must mark it dirty with
     * [instanceDirty]. By default, a [MeshPrimitiveInstance] will mark itself dirty via the
     * [Node3D.dirty] function.
     */
    fun addInstance(instance: MeshPrimitiveInstance) {
        if (instance.instanceOf != null) {
            logger.warn {
                "MeshPrimitiveInstance is already an instance of another MeshPrimitive. Remove old instance before adding new one."
            }
            return
        }
        if (instances.contains(instance)) {
            return
        }
        val id = nextInstanceId
        insertId(id, instanceCount)
        instancesToId[instance] = id
        instances += instance
        dirtyInstances += instance
        instance.instanceOf = this
        instancesDirty = true
    }

    /** Removes the designated [MeshPrimitiveInstance] as an instance. */
    fun removeInstance(instance: MeshPrimitiveInstance) {
        if (instance.instanceOf == this) {
            val id = instancesToId[instance]
            if (id == null) {
                logger.warn { "Instance does not exist or has already been removed!" }
                return
            }

            val removeIdx = instanceIndices[id]
            if (removeIdx == null) {
                logger.warn { "Instance does not exist or has already been removed!" }
                return
            }
            removeId(id, removeIdx)
            instances -= instance
            dirtyInstances -= instance
            instancesToId.remove(instance)
            instance.instanceOf = null
            instancesDirty = true
        }
    }

    /**
     * Update the CPU transfer storage buffer for the following [instance], as long as it has been
     * adeed via [addInstance], to prepare it for writing to the GPU buffer.
     */
    fun updateInstance(instance: MeshPrimitiveInstance) {
        val id = instancesToId[instance]
        if (id == null) {
            logger.warn { "Instance does not exist to be updated!" }
            return
        }

        val idx = instanceIndices[id]
        if (idx == null) {
            logger.warn { "Instance does not exist to be updated!" }
            return
        }

        instanceData.position = idx * TRANSFORM_COMPONENTS_PER_INSTANCE
        instanceData.put(instance.globalTransform.data)
        instanceData.put(instance.color.fields)
    }

    /**
     * Clears all instance data. Doing this will break any existing [MeshPrimitiveInstance] tied to
     * this primitive. Either destroy the node and recreate it or add it back via [addInstance].
     */
    fun clearInstances() {
        instanceData.clear()
        instancesDirty = true
        instanceIndices.clear()
        instancesToId.clear()
        instances.clear()
        dirtyInstances.clear()
    }

    private fun insertId(id: InstanceId, insertIdx: Int) {
        val staticEndIdx = instanceCount * TRANSFORM_COMPONENTS_PER_INSTANCE
        if (instanceData.capacity <= staticEndIdx) {
            logger.debug {
                "Instance Data buffer has run out of room. Increasing size by a factor of 2 to a size of ${instanceData.capacity * 2}"
            }
            val newData = FloatBuffer(instanceData.capacity * 2)
            newData.put(instanceData)
            instanceData = newData
            instanceData.position = 0
        }

        var shouldShift = false
        instanceIndices.forEach { (instanceId, idx) ->
            if (idx >= insertIdx) {
                shouldShift = true
                instanceIndices[instanceId] = idx + 1
            }
        }
        // shift data down from insertIdx to instanceCount-1
        if (shouldShift) {
            val staticOffset = (insertIdx + 1) * TRANSFORM_COMPONENTS_PER_INSTANCE
            val staticStartIdx = insertIdx * TRANSFORM_COMPONENTS_PER_INSTANCE
            instanceData.shiftDataDown(staticStartIdx, staticEndIdx, staticOffset)
        }

        instanceIndices[id] = insertIdx
    }

    private fun removeId(id: InstanceId, removeIdx: Int) {
        // shift up the instances after remove location by 1
        instanceIndices.forEach { (spriteId, idx) ->
            if (idx >= removeIdx) {
                instanceIndices[spriteId] = idx - 1
            }
        }

        // shift up all the data in the buffer from removeIdx to spriteCount-1
        val staticOffset = removeIdx * TRANSFORM_COMPONENTS_PER_INSTANCE
        instanceData.put(
            instanceData,
            dstOffset = staticOffset,
            srcOffset = removeIdx * TRANSFORM_COMPONENTS_PER_INSTANCE,
            len = instanceCount * TRANSFORM_COMPONENTS_PER_INSTANCE,
        )

        instanceIndices.remove(id)
    }

    private fun FloatBuffer.shiftDataDown(startIdx: Int, endIdx: Int, dstOffset: Int) {
        if (endIdx == 0) return
        var idx = endIdx - 1
        for (i in endIdx - 1 downTo startIdx) {
            this[dstOffset + idx--] = this[i]
        }
    }

    override fun release() {
        instanceBuffers.release()
    }

    companion object {
        private const val TRANSFORM_COMPONENTS_PER_INSTANCE = 20
        private val logger: Logger = Logger<MeshPrimitive>()
    }
}

private typealias InstanceId = Int
