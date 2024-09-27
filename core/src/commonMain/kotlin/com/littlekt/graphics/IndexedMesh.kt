package com.littlekt.graphics

import com.littlekt.graphics.util.IndexedMeshGeometry
import io.ygdrasil.wgpu.BufferUsage
import io.ygdrasil.wgpu.Device
import kotlin.math.min

/**
 * A wrapper that handles the creation of a vertex GPU buffer and a index GPU buffer as well as
 * uploading data to these buffers, and recreating them if they run out of size.
 *
 * @param device the current device
 * @param geometry the underlying [IndexedMeshGeometry] of this mesh for uploading data.
 * @author Colton Daily
 * @date 4/10/2024
 */
class IndexedMesh<T : IndexedMeshGeometry>(device: Device, geometry: T) :
    Mesh<T>(device, geometry) {

    /** The GPU index buffer for this mesh, if created. */
    var ibo = device.createGPUShortBuffer(
            "ibo",
            geometry.indices.toArray(),
            setOf(BufferUsage.index, BufferUsage.copydst)
        )
        private set

    /**
     * Update the vertex and index buffers, if the underlying geometry has changed. If the
     * underlying geometry is bigger than the GPU buffers, the GPU buffers will be recreated and the
     * new data copied over.
     */
    override fun update() {
        if (geometry.dirty) {
            if (geometry.verticesDirty) {
                if (vbo.size < geometry.vertices.capacity * Float.SIZE_BYTES) {
                    logger.trace {
                        "Destroying and creating VBO from size: ${vbo.size} to  size: ${geometry.vertices.capacity}"
                    }
                    vbo.close()
                    vbo =
                        device.createGPUFloatBuffer(
                            "vbo",
                            geometry.vertices.toArray(),
                            setOf(BufferUsage.vertex,BufferUsage.copydst)
                        )
                    // need to remake ibo because indices won't correspond correctly to the new
                    // vertices
                    destroyAndRecreateIbo()
                } else {
                    val size =
                        min(
                            vbo.size / Float.SIZE_BYTES,
                            geometry.numVertices *
                                    geometry.layout.attributes.calculateComponents().toLong()
                        )
                    logger.trace { "Writing VBO to queue of size: $size" }
                    device.queue.writeBuffer(vbo, 0L, geometry.vertices.toArray(), size = size)
                }
            }
            if (geometry.indicesDirty) {
                if (ibo.size < geometry.indices.capacity * Short.SIZE_BYTES) {
                    destroyAndRecreateIbo()
                } else {
                    val size = min(ibo.size / Short.SIZE_BYTES, geometry.indices.limit.toLong())
                    logger.trace { "Writing IBO to queue of size: $size" }
                    device.queue.writeBuffer(ibo, 0L, geometry.indices.toArray(), size = size)
                }
                geometry.indicesDirty = false
            }
        }
    }

    private fun destroyAndRecreateIbo() {
        logger.trace { "Destroy and recreating IBO." }
        ibo.close()
        ibo.close()
        if (geometry.indicesType == IndexedMeshGeometry.IndicesType.QUAD) {
            logger.trace { "Regenerating indices as quads." }
            geometry.indicesAsQuad()
        } else if (geometry.indicesType == IndexedMeshGeometry.IndicesType.TRI) {
            logger.trace { "Regenerating indices as tris." }
            geometry.indicesAsTri()
        }
        logger.trace { "Creating IBO of size: ${geometry.indices.capacity}" }
        ibo =
            device.createGPUShortBuffer(
                "ibo",
                geometry.indices.toArray(),
                setOf( BufferUsage.index, BufferUsage.copydst)
            )
    }

    override fun release() {
        ibo.close()
        vbo.close()
        ibo.close()
    }
}
