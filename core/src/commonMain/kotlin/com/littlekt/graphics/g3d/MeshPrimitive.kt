package com.littlekt.graphics.g3d

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Color
import com.littlekt.graphics.IndexedMesh
import com.littlekt.graphics.Mesh
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.g3d.util.InstanceBuffers
import com.littlekt.graphics.webgpu.IndexFormat
import com.littlekt.graphics.webgpu.PrimitiveTopology
import com.littlekt.log.Logger

/**
 * @param instanceSize pre-allocate this number of instances in the instance data storage buffer.
 *   Defaults to `0`.
 * @author Colton Daily
 * @date 11/25/2024
 */
open class MeshPrimitive(
    val mesh: Mesh<*>,
    val material: Material,
    val topology: PrimitiveTopology = PrimitiveTopology.TRIANGLE_LIST,
    val stripIndexFormat: IndexFormat? = null,
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

    val instanceBuffers: InstanceBuffers =
        InstanceBuffers(mesh.device, (instanceSize + 1) * TRANSFORM_COMPONENTS_PER_INSTANCE)

    private val instances = mutableListOf<VisualInstance>()
    private var instancesDirty = true
    private var instanceData = FloatBuffer((instanceSize + 1) * TRANSFORM_COMPONENTS_PER_INSTANCE)
    private val instanceIndices = mutableMapOf<InstanceId, Int>()
    private val instancesToId = mutableMapOf<VisualInstance, InstanceId>()
    private val dirtyInstances = mutableListOf<VisualInstance>()
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

    fun instanceDirty(instance: VisualInstance) {
        if (dirtyInstances.contains(instance)) return
        instancesDirty = true
        dirtyInstances += instance
    }

    fun writeInstanceDataToBuffer() {
        if (instancesDirty) {
            dirtyInstances.forEach { dirtyInstance -> updateInstance(dirtyInstance) }
            dirtyInstances.clear()
            instanceBuffers.updateStaticStorage(instanceData)
            instancesDirty = false
        }
    }

    fun setInstanceColor(instance: VisualInstance, color: Color) {
        if (!instances.contains(instance)) {
            return
        }
    }

    fun addInstance(instance: VisualInstance) {
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

    fun removeInstance(instance: VisualInstance) {
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

    fun updateInstance(instance: VisualInstance) {
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
            srcOffset = (removeIdx + 1) * TRANSFORM_COMPONENTS_PER_INSTANCE,
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
