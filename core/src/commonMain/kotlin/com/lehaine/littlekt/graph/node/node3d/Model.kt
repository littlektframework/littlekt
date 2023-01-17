package com.lehaine.littlekt.graph.node.node3d

import com.lehaine.littlekt.graphics.g3d.model.Animation
import com.lehaine.littlekt.graphics.g3d.model.Skin
import com.lehaine.littlekt.util.fastForEach
import com.lehaine.littlekt.util.fastForEachWithIndex
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class Model : VisualInstance() {

    val nodes3d = mutableMapOf<String, Node3D>()
    val meshes = mutableMapOf<String, MeshNode>()

    val animations = mutableListOf<Animation>()
    val skins = mutableListOf<Skin>()

    fun disableAllAnimations() {
        enableAnimation(-1)
    }

    fun enableAnimation(animationIdx: Int) {
        animations.fastForEachWithIndex { i, anim ->
            anim.weight = if (i == animationIdx) 1f else 0f
        }
    }

    fun setAnimationWeight(iAnimation: Int, weight: Float) {
        animations.fastForEach {
            it.weight = weight
        }
    }

    fun applyAnimation(dt: Duration) {
        var firstActive = true
        animations.fastForEach { anim ->
            if (anim.weight > 0f) {
                anim.apply(dt, firstActive)
                firstActive = false
            }
        }

        skins.fastForEach {
            it.updateJointTransforms()
        }

    }
}