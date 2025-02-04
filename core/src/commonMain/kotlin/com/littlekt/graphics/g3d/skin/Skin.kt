package com.littlekt.graphics.g3d.skin

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Mat4
import com.littlekt.util.UniqueId
import com.littlekt.util.datastructure.fastForEach

/**
 * @author Colton Daily
 * @date 11/24/2024
 */
class Skin(val device: Device, val nodes: List<SkinNode>) : Releasable {
    var id: Int = UniqueId.next<Skin>()
        private set

    private val jointsStagingBuffer = FloatBuffer(nodes.size * 16)
    private val inverseBindingStagingBuffer = FloatBuffer(nodes.size * 16)

    private val jointBuffer =
        device.createGPUFloatBuffer(
            "Skin joints",
            jointsStagingBuffer,
            BufferUsage.STORAGE or BufferUsage.COPY_DST,
        )

    private val jointBufferBinding = BufferBinding(jointBuffer)

    private val inverseBindingBuffer =
        device.createGPUFloatBuffer(
            "Skin inverse binding",
            inverseBindingStagingBuffer,
            BufferUsage.STORAGE or BufferUsage.COPY_DST,
        )
    private val inverseBindingBufferBinding = BufferBinding(inverseBindingBuffer)

    fun createBindGroup(shader: Shader): BindGroup {
        return device.createBindGroup(
            BindGroupDescriptor(
                shader.getBindGroupLayoutByUsage(BindingUsage.SKIN),
                listOf(
                    BindGroupEntry(0, jointBufferBinding),
                    BindGroupEntry(1, inverseBindingBufferBinding),
                ),
            )
        )
    }

    fun writeToBuffer() {
        nodes.fastForEach { node ->
            jointsStagingBuffer.put(node.joint.globalTransform.data)
            inverseBindingStagingBuffer.put(node.inverseBindMatrix.data)
        }
        device.queue.writeBuffer(jointBuffer, jointsStagingBuffer)
        device.queue.writeBuffer(inverseBindingBuffer, inverseBindingStagingBuffer)
        jointsStagingBuffer.flip()
        inverseBindingStagingBuffer.flip()
    }

    fun printHierarchy() {
        nodes.forEach { it.printHierarchy("") }
    }

    fun copy(): Skin {
        // TODO
        return Skin(device, nodes)
    }

    override fun release() {
        jointBuffer.release()
        inverseBindingBuffer.release()
    }

    class SkinNode(val joint: Node3D, val inverseBindMatrix: Mat4) {
        private var parent: SkinNode? = null
        private val children = mutableListOf<SkinNode>()

        fun addChild(node: SkinNode) {
            node.parent = this
            children += node
        }

        fun printHierarchy(indent: String) {
            println("$indent${joint.name}")
            children.forEach { it.printHierarchy("$indent    ") }
        }
    }
}
