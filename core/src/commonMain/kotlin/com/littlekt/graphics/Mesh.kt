package com.littlekt.graphics

import com.littlekt.Releasable
import com.littlekt.graphics.util.MeshGeometry
import io.ygdrasil.webgpu.Device
import com.littlekt.log.Logger
import io.ygdrasil.webgpu.Buffer
import io.ygdrasil.webgpu.BufferUsage
import kotlin.jvm.JvmStatic
import kotlin.math.min

/**
 * A wrapper that handles the creation of a vertex GPU buffer as well as uploading data to this
 * buffer, and recreating it if it runs out of size. If an index GPU buffer is required, see
 * [IndexedMesh].
 *
 * @param device the current device
 * @param geometry the underlying [MeshGeometry] of this mesh for uploading data.
 * @author Colton Daily
 * @date 4/10/2024
 */
open class Mesh<T : MeshGeometry>(val device: Device, val geometry: T) : Releasable {

    /** The GPU Vertex buffer for this mesh. */
    var vbo: Buffer =
        device.createGPUFloatBuffer(
            "vbo",
            geometry.vertices.toArray(),
            setOf(BufferUsage.Vertex, BufferUsage.CopyDst)
        )
        protected set

    /**
     * Update the vertex buffer, if the underlying geometry has changed. If the underlying geometry
     * is bigger than the GPU buffers, the GPU buffers will be recreated and the new data copied
     * over.
     */
    open fun update() {
        if (geometry.dirty) {
            if (geometry.verticesDirty) {
                if (vbo.size < (geometry.vertices.capacity * Float.SIZE_BYTES).toULong()) {
                    logger.trace {
                        "Destroying and creating VBO from size: ${vbo.size} to  size: ${geometry.vertices.capacity}"
                    }
                    vbo.close()
                    vbo =
                        device.createGPUFloatBuffer(
                            "vbo",
                            geometry.vertices.toArray(),
                            setOf(BufferUsage.Vertex, BufferUsage.CopyDst)
                        )
                } else {
                    val size =
                        min(
                            vbo.size,
                            geometry.numVertices.toULong() *
                                geometry.layout.attributes.calculateComponents().toULong()
                        )
                    logger.trace { "Writing VBO to queue of size: $size" }
                    device.queue.writeBuffer(vbo, 0uL, geometry.vertices.toArray(), size = size)
                }
            }
        }
    }

    /** Clears the underlying geometries vertices. */
    fun clearVertices() {
        geometry.clearVertices()
    }

    override fun release() {
        vbo.close()
    }

    companion object {
        @JvmStatic protected val logger = Logger<Mesh<*>>()
    }
}
