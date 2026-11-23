package com.littlekt.graphics.g3d.skin

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Mat4
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.datastructure.fastForEachWithIndex

/**
 * The joints of a skinned mesh. Each frame the joint matrices are written to a palette relative to
 * [root], and every instance of a skinned [com.littlekt.graphics.g3d.MeshPrimitive] is drawn with
 * its own transform on top of that pose.
 *
 * @param root the node the joint matrices are relative to, usually the mesh node using this skin
 *   or the parent shared by every mesh node using it. If `null`, the joints are in world space and
 *   instances should have an identity transform.
 * @author Colton Daily
 * @date 11/24/2024
 */
class Skin(val device: Device, val nodes: List<SkinNode>, var root: Node3D? = null) : Releasable {
    val id: Int
        get() = palette.id

    private val jointsStagingBuffer = FloatBuffer(nodes.size * 16)
    private val tempMat = Mat4()

    internal val palette = JointPalette(device, "Skin joints", jointsStagingBuffer)

    fun createBindGroup(shader: Shader): BindGroup = palette.createBindGroup(shader)

    /** Writes the current joint matrices, relative to [root], to the GPU. */
    fun writeToBuffer() {
        val inverseRoot = root?.globalInverseTransform
        nodes.fastForEachWithIndex { index, node ->
            if (inverseRoot != null) tempMat.set(inverseRoot) else tempMat.setToIdentity()
            tempMat.mul(node.joint.globalTransform).mul(node.inverseBindMatrix)
            jointsStagingBuffer.put(tempMat.data, dstOffset = index * 16)
        }
        palette.write(jointsStagingBuffer)
    }

    fun printHierarchy() {
        nodes.forEach { it.printHierarchy("") }
    }

    fun copy(): Skin = Skin(device, nodes, root)

    /**
     * Creates a skin with its own palette whose joints are the nodes [joint] maps each joint to,
     * such as the joints of a copied [com.littlekt.graphics.g3d.Model].
     *
     * @param root the node the joint matrices of the new skin are relative to
     * @param joint maps each joint of this skin to the joint of the new skin
     */
    fun copy(root: Node3D?, joint: (Node3D) -> Node3D): Skin {
        val copies = nodes.map { SkinNode(joint(it.joint), it.inverseBindMatrix) }
        nodes.fastForEachWithIndex { index, node ->
            node.children.fastForEach { child ->
                val childIndex = nodes.indexOf(child)
                if (childIndex >= 0) copies[index].addChild(copies[childIndex])
            }
        }
        return Skin(device, copies, root)
    }

    override fun release() {
        palette.release()
    }

    class SkinNode(val joint: Node3D, val inverseBindMatrix: Mat4) {
        private var parent: SkinNode? = null
        internal val children = mutableListOf<SkinNode>()

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
