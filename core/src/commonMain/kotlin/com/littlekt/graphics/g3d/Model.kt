package com.littlekt.graphics.g3d

import com.littlekt.graphics.g3d.skin.Animation
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.datastructure.fastForEachWithIndex
import kotlin.time.Duration

/**
 * A node that is usually a parent of one or many [MeshNode]. This class handles animations for any
 * skinned meshes.
 *
 * @author Colton Daily
 * @date 2/3/2025
 */
open class Model : Node3D() {
    val animations = mutableListOf<Animation>()

    fun disableAllAnimations() {
        enableAnimation(-1)
    }

    fun enableAnimation(animationIdx: Int) {
        animations.fastForEachWithIndex { i, anim ->
            anim.weight = if (i == animationIdx) 1f else 0f
        }
    }

    fun setAnimationWeight(idx: Int, weight: Float) {
        animations[idx].weight = weight
    }

    override fun onUpdate(dt: Duration) {
        super.onUpdate(dt)
        var firstActive = true
        animations.fastForEach { anim ->
            if (anim.weight > 0f) {
                anim.apply(dt, firstActive)
                firstActive = false
            }
        }
    }
}
