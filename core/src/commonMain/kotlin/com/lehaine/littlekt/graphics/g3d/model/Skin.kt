package com.lehaine.littlekt.graphics.g3d.model

import com.lehaine.littlekt.graph.node.node3d.Node3D
import com.lehaine.littlekt.math.Mat4

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
            children.forEach {
                it.printHierarchy("$indent    ")
            }
        }
    }
}