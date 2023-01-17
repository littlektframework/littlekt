package com.lehaine.littlekt.graphics.g3d.model

import com.lehaine.littlekt.graph.node.node3d.MeshNode
import com.lehaine.littlekt.graph.node.node3d.Node3D
import com.lehaine.littlekt.math.*
import com.lehaine.littlekt.util.datastructure.TreeMap
import com.lehaine.littlekt.util.seconds
import kotlin.math.min
import kotlin.time.Duration

class Animation(val name: String?) {
    val channels = mutableListOf<AnimationChannel<*>>()

    var weight = 1f
    var speed = 1f
    var duration = 1f
        private set

    var progress = 0f

    private val animationNodes = mutableListOf<AnimationNode>()

    fun prepareAnimation() {
        duration = channels.map { it.lastKeyTime }.maxOrNull() ?: 0f
        channels.forEach { it.duration = duration }
        animationNodes += channels.map { it.animationNode }.distinct()
    }

    fun apply(dt: Duration, firstWeightedTransform: Boolean = true) {
        progress = (progress + duration + dt.seconds * speed) % duration

        for (i in animationNodes.indices) {
            animationNodes[i].initTransform()
        }
        for (i in channels.indices) {
            channels[i].apply(progress)
        }
        if (weight == 1f) {
            for (i in animationNodes.indices) {
                animationNodes[i].applyTransform()
            }
        } else {
            for (i in animationNodes.indices) {
                animationNodes[i].applyTransformWeighted(weight, firstWeightedTransform)
            }
        }
    }

    fun printChannels() {
        println("$name channels:")
        channels.forEach { ch ->
            println("  ${ch.name} [node: ${ch.animationNode.name}]")
            ch.printKeys("    ")
        }
    }
}

abstract class AnimationChannel<T : AnimationKey<T>>(val name: String?, val animationNode: AnimationNode) {
    val keys = TreeMap<Float, T>()
    val lastKeyTime: Float
        get() = keys.lastKey()
    var duration = 0f

    fun apply(time: Float) {
        var key = keys.floorValue(time)
        if (key == null) {
            key = if (isFuzzyEqual(lastKeyTime, duration)) keys.lastValue() else keys.firstValue()
        }
        key.apply(time, keys.higherValue(time), animationNode)
    }

    fun printKeys(indent: String = "") {
        val animKeys = keys.values.toList()
        for (i in 0 until min(5, animKeys.size)) {
            println("$indent${animKeys[i]}")
        }
        if (animKeys.size > 5) {
            println("$indent  ...${animKeys.size - 5} more")
        }
    }
}

class TranslationAnimationChannel(name: String?, animationNode: AnimationNode) :
    AnimationChannel<TranslationKey>(name, animationNode)

class RotationAnimationChannel(name: String?, animationNode: AnimationNode) :
    AnimationChannel<RotationKey>(name, animationNode)

class ScaleAnimationChannel(name: String?, animationNode: AnimationNode) :
    AnimationChannel<ScaleKey>(name, animationNode)

class WeightAnimationChannel(name: String?, animationNode: AnimationNode) :
    AnimationChannel<WeightKey>(name, animationNode)

interface AnimationNode {
    val name: String?

    fun initTransform() {}
    fun applyTransform()
    fun applyTransformWeighted(weight: Float, firstWeightedTransform: Boolean)

    fun setTranslation(translation: Vec3f) {}
    fun setRotation(rotation: Vec4f) {}
    fun setScale(scale: Vec3f) {}

    fun setWeights(weights: FloatArray) {}
}

class AnimatedTransformGroup(val target: Node3D) : AnimationNode {
    override val name: String
        get() = target.name

    private val initTranslation = MutableVec3f()
    private val initRotation = MutableVec4f()
    private val initScale = MutableVec3f(1f, 1f, 1f)

    private val animTranslation = MutableVec3f()
    private val animRotation = MutableVec4f()
    private val animScale = MutableVec3f()

    private val tempVec3f = MutableVec3f()
    private val tempVec4f = MutableVec4f()

    private val quatRotMat = Mat4()
    private val weightedTransformMat = Mat4()

    init {
        target.transform.getTranslation(initTranslation)
        target.transform.getRotation(initRotation)
        target.transform.getScale(initScale)
    }

    override fun initTransform() {
        animTranslation.set(initTranslation)
        animRotation.set(initRotation)
        animScale.set(initScale)
    }

    override fun applyTransform() {
        target.setIdentity()
        target.translate(animTranslation)
        quatRotMat.setToRotation(animRotation)
        target.rotation(animRotation)
        target.scaling(animScale.x, animScale.y, animScale.z)
    }

    override fun applyTransformWeighted(weight: Float, firstWeightedTransform: Boolean) {
        weightedTransformMat.setToIdentity()
        weightedTransformMat.translate(animTranslation)
        weightedTransformMat.mul(quatRotMat.setToRotation(animRotation))
        weightedTransformMat.scale(animScale.x, animScale.y, animScale.z)

        weightedTransformMat.getTranslation(tempVec3f)
        tempVec3f.scale(weight)
        if (!firstWeightedTransform) {
            tempVec3f.add(target.position)
        }
        target.position(tempVec3f)

        weightedTransformMat.getRotation(tempVec4f)
        tempVec4f.scale(weight)
        if (!firstWeightedTransform) {
            tempVec4f.add(target.rotation)
        }
        target.rotation(tempVec4f)

        weightedTransformMat.getScale(tempVec3f)
        tempVec3f.scale(weight)
        if (!firstWeightedTransform) {
            tempVec3f.add(target.scale)
        }
        target.scaling(tempVec3f.x, tempVec3f.y, tempVec3f.z)

        target.setDirty()
    }

    override fun setTranslation(translation: Vec3f) {
        animTranslation.set(translation)
    }

    override fun setRotation(rotation: Vec4f) {
        animRotation.set(rotation)
    }

    override fun setScale(scale: Vec3f) {
        animScale.set(scale)
    }
}

class MorphAnimatedMesh(val target: MeshNode) : AnimationNode {
    override val name: String
        get() = target.name

    private var weights = FloatArray(1)

    override fun applyTransform() {
        target.morphWeights = weights
    }

    override fun applyTransformWeighted(weight: Float, firstWeightedTransform: Boolean) {
        var targetW = target.morphWeights
        if (targetW == null || targetW.size != weights.size) {
            targetW = FloatArray(weights.size)
            target.morphWeights = targetW
        }
        for (i in weights.indices) {
            if (firstWeightedTransform) {
                targetW[i] = weights[i] * weight
            } else {
                targetW[i] += weights[i] * weight
            }
        }
    }

    override fun setWeights(weights: FloatArray) {
        if (this.weights.size != weights.size) {
            this.weights = FloatArray(weights.size)
        }
        for (i in weights.indices) {
            this.weights[i] = weights[i]
        }
    }
}