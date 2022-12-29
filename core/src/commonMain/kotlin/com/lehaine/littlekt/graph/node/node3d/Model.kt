package com.lehaine.littlekt.graph.node.node3d

import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.g3d.model.Animation
import com.lehaine.littlekt.graphics.g3d.model.Skin

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class Model : Node3D() {

    val nodes3d = mutableMapOf<String, Node3D>()
    val meshes = mutableMapOf<String, MeshNode>()
    val textures = mutableMapOf<String, Texture>()

    val animations = mutableListOf<Animation>()
    val skins = mutableListOf<Skin>()

    fun disableAllAnimations() {
        enableAnimation(-1)
    }

    fun enableAnimation(iAnimation: Int) {
        for (i in animations.indices) {
            animations[i].weight = if (i == iAnimation) 1f else 0f
        }
    }

    fun setAnimationWeight(iAnimation: Int, weight: Float) {
        if (iAnimation in animations.indices) {
            animations[iAnimation].weight = weight
        }
    }

    fun applyAnimation(deltaT: Float) {
        var firstActive = true
        for (i in animations.indices) {
            if (animations[i].weight > 0f) {
                animations[i].apply(deltaT, firstActive)
                firstActive = false
            }
        }
        for (i in skins.indices) {
            skins[i].updateJointTransforms()
        }
    }

    fun printHierarchy() {
        printHierarchy("")
    }

    private fun Node3D.printHierarchy(indent: String) {
        println("$indent$name [${children.filterIsInstance<MeshNode>().count()} meshes]")
        children.forEach {
            if (it is Node3D) {
                it.printHierarchy("$indent    ")
            }
        }
    }
}