package com.littlekt.graphics.g3d

import com.littlekt.math.Mat4

/**
 * @author Colton Daily
 * @date 11/24/2024
 */
class Skin {
    val nodes = mutableListOf<SkinNode>()

    fun updateJointTransforms() {
        for (i in nodes.indices) {
            if (!nodes[i].hasParent) {
                nodes[i].updateJointTransform()
            }
        }
    }

    fun printHierarchy() {
        nodes.filter { !it.hasParent }.forEach { it.printHierarchy("") }
    }

    fun copy(): Skin {
        // TODO
        return Skin()
    }

    class SkinNode(val joint: Node3D, val inverseBindMatrix: Mat4) {
        val jointTransform = Mat4()

        private val tmpMat4f = Mat4()
        private var parent: SkinNode? = null
        private val children = mutableListOf<SkinNode>()

        val hasParent: Boolean
            get() = parent != null

        fun addChild(node: SkinNode) {
            node.parent = this
            children += node
        }

        fun updateJointTransform() {
            jointTransform.set(joint.transform)
            for (i in children.indices) {
                children[i].updateJointTransform(jointTransform)
            }
            jointTransform.mul(inverseBindMatrix)
        }

        private fun updateJointTransform(parentTransform: Mat4) {
            tmpMat4f.set(joint.transform)
            jointTransform.set(parentTransform).mul(tmpMat4f)
            for (i in children.indices) {
                children[i].updateJointTransform(jointTransform)
            }
            jointTransform.mul(inverseBindMatrix)
        }

        fun printHierarchy(indent: String) {
            println("$indent${joint.name}")
            children.forEach { it.printHierarchy("$indent    ") }
        }
    }
}
