package com.littlekt.graphics.g3d

import com.littlekt.Releasable
import com.littlekt.graphics.Texture
import com.littlekt.graphics.TextureState
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.g3d.skin.Animation
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.datastructure.fastForEachWithIndex
import kotlin.time.Duration

/**
 * A node that is usually a parent of one or many [MeshNode]. This class handles animations for any
 * skinned meshes. A loaded model, such as a glTF model, owns the meshes, materials, skins, and
 * textures it created, which are freed with [release]. Copies of the model share these resources.
 *
 * @author Colton Daily
 * @date 2/3/2025
 */
open class Model : Node3D(), Releasable {
    val animations = mutableListOf<Animation>()

    private val resources = mutableListOf<Releasable>()

    /** Adds a [resource] this model owns to be released with [release]. */
    fun own(resource: Releasable) {
        if (resources.none { it === resource }) resources += resource
    }

    /**
     * Releases the meshes, materials, skins, and textures this model owns. Any copies of this
     * model can't be rendered after.
     */
    override fun release() {
        resources.fastForEach {
            if (it !is Texture || it.state == TextureState.LOADED) it.release()
        }
        resources.clear()
    }

    /** Set's material for each [MeshPrimitive] child. */
    fun setMaterial(material: Material) {
        forEachMeshPrimitive { it.material = material }
    }

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
