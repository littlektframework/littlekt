package com.lehaine.littlekt.graphics.g3d.model

import com.lehaine.littlekt.graphics.Mesh
import com.lehaine.littlekt.graphics.Texture

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class Model(name: String? = null) : Node(name) {

    val nodes = mutableMapOf<String, Node>()
    val meshes = mutableMapOf<String, Mesh>()
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

    private fun Node.printHierarchy(indent: String) {
        println("$indent$name [${children.filterIsInstance<Mesh>().count()} meshes]")
        children.forEach {
            it.printHierarchy("$indent    ")
        }
    }

    override fun dispose() {
        textures.values.forEach { it.dispose() }
    }
}