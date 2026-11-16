package com.littlekt.graphics.g3d

import com.littlekt.Releasable
import com.littlekt.graphics.Texture
import com.littlekt.graphics.TextureState
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.g3d.skin.Animation
import com.littlekt.graphics.g3d.skin.MorphBlend
import com.littlekt.graphics.g3d.skin.TransformBlend
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.datastructure.fastForEachWithIndex
import com.littlekt.util.seconds
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

    /** The fade time [play] and [stop] use when none is given. */
    var defaultFade: Duration = 200.milliseconds

    /** The clip last started with [play] or [enableAnimation], or `null` once stopped. */
    var currentAnimation: Animation? = null
        private set

    private val transformBlends = mutableMapOf<Node3D, TransformBlend>()
    private val morphBlends = mutableMapOf<MeshPrimitive, MorphBlend>()
    private var sharedAnimations = 0

    /** @return the first clip named [name], or `null` if there is none */
    fun findAnimation(name: String): Animation? {
        animations.fastForEach { if (it.name == name) return it }
        return null
    }

    /**
     * Plays the clip named [name], crossfading every other clip out over [fade].
     *
     * @see play
     */
    fun play(
        name: String,
        loop: Boolean = true,
        fade: Duration = defaultFade,
        restart: Boolean = false,
    ): Animation {
        val animation = findAnimation(name) ?: error("Model has no animation named '$name'!")
        return play(animation, loop, fade, restart)
    }

    /**
     * Plays [animation], crossfading every other clip out over [fade]. A clip that isn't already
     * playing, or that has finished, starts from its beginning.
     *
     * @param animation one of [animations]
     * @param loop whether the clip wraps around at its end or holds its last pose
     * @param fade the time to crossfade over, or [Duration.ZERO] to switch at once
     * @param restart whether to start the clip from its beginning even if it's already playing
     * @return [animation]
     */
    fun play(
        animation: Animation,
        loop: Boolean = true,
        fade: Duration = defaultFade,
        restart: Boolean = false,
    ): Animation {
        require(animations.contains(animation)) { "Animation is not one of this model's animations!" }
        if (restart || animation.weight <= 0f || animation.isFinished) animation.reset()
        animation.loop = loop
        currentAnimation = animation
        animations.fastForEach { fadeTo(it, if (it === animation) 1f else 0f, fade) }
        return animation
    }

    /** Fades every clip out over [fade]. */
    fun stop(fade: Duration = defaultFade) {
        currentAnimation = null
        animations.fastForEach { fadeTo(it, 0f, fade) }
    }

    fun disableAllAnimations() {
        enableAnimation(-1)
    }

    /** Plays the clip at [animationIdx] at full weight at once, or none if it's out of range. */
    fun enableAnimation(animationIdx: Int) {
        currentAnimation = animations.getOrNull(animationIdx)
        animations.fastForEachWithIndex { i, anim ->
            anim.weight = if (i == animationIdx) 1f else 0f
            anim.fadeTarget = anim.weight
            anim.fadeRate = 0f
        }
    }

    /** Sets the blend weight of the clip at [idx] at once, cancelling any fade of it. */
    fun setAnimationWeight(idx: Int, weight: Float) {
        val anim = animations[idx]
        anim.weight = weight
        anim.fadeTarget = weight
        anim.fadeRate = 0f
    }

    override fun onUpdate(dt: Duration) {
        super.onUpdate(dt)
        updateAnimations(dt)
    }

    /**
     * Advances the fades and playing clips by [dt] and poses the model. Called by [onUpdate].
     */
    fun updateAnimations(dt: Duration) {
        if (sharedAnimations != animations.size) shareBlends()
        val seconds = dt.seconds
        var active = 0
        var totalWeight = 0f
        var single: Animation? = null
        animations.fastForEach { anim ->
            if (anim.fadeRate > 0f) {
                val step = anim.fadeRate * seconds
                if (abs(anim.fadeTarget - anim.weight) <= step) {
                    anim.weight = anim.fadeTarget
                    anim.fadeRate = 0f
                } else if (anim.fadeTarget > anim.weight) {
                    anim.weight += step
                } else {
                    anim.weight -= step
                }
            }
            if (anim.weight > 0f) {
                anim.advance(dt, notify = anim.fadeRate == 0f || anim.fadeTarget > 0f)
                active++
                totalWeight += anim.weight
                single = anim
            }
        }
        if (active == 0) return
        val only = single
        if (active == 1 && only != null && only.weight >= 1f) {
            only.applyPose()
            return
        }
        if (totalWeight < 1f) totalWeight = 1f
        animations.fastForEach { if (it.weight > 0f) it.resetBlend() }
        animations.fastForEach { if (it.weight > 0f) it.blend(it.weight) }
        animations.fastForEach { if (it.weight > 0f) it.applyBlend(totalWeight) }
    }

    private fun fadeTo(animation: Animation, target: Float, fade: Duration) {
        animation.fadeTarget = target
        if (fade <= Duration.ZERO || animation.weight == target) {
            animation.weight = target
            animation.fadeRate = 0f
        } else {
            animation.fadeRate = abs(target - animation.weight) / fade.seconds
        }
    }

    private fun shareBlends() {
        transformBlends.clear()
        morphBlends.clear()
        animations.fastForEach { it.shareBlends(transformBlends, morphBlends) }
        sharedAnimations = animations.size
    }
}
