package com.littlekt.graphics.g3d.skin

import com.littlekt.math.*
import com.littlekt.util.toString
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author Colton Daily
 * @date 2/3/2025
 */
abstract class AnimationKey<T : AnimationKey<T>>(val time: Float) {
    var interpolation = Interpolation.LINEAR

    abstract fun apply(time: Float, next: T?, node: AnimationNode)

    protected fun interpolationPos(pos: Float, nextTime: Float): Float {
        return if (time != nextTime) {
            val t =
                if (time < nextTime) {
                    time
                } else {
                    0f
                }
            interpolation.getInterpolationPos((pos - t) / (nextTime - t))
        } else {
            0f
        }
    }

    override fun toString() = "${time.toString(2)} -> [$interpolation]"

    enum class Interpolation {
        LINEAR {
            override fun getInterpolationPos(linearPos: Float) = linearPos
        },
        STEP {
            override fun getInterpolationPos(linearPos: Float) = 0f
        },
        CUBICSPLINE {
            // actual cubic interpolation depends on more variables, but takes linear value as an
            // input
            override fun getInterpolationPos(linearPos: Float) = linearPos
        };

        abstract fun getInterpolationPos(linearPos: Float): Float
    }
}

open class RotationKey(time: Float, val rotation: Quaternion) : AnimationKey<RotationKey>(time) {
    private val tmpRotation = MutableQuaternion()

    private val qa = MutableQuaternion()
    private val qb = MutableQuaternion()
    private val qc = MutableQuaternion()

    override fun apply(time: Float, next: RotationKey?, node: AnimationNode) {
        if (next == null) {
            tmpRotation.set(rotation)
        } else {
            slerp(rotation, next.rotation, interpolationPos(time, next.time), tmpRotation)
        }
        node.setRotation(tmpRotation)
    }

    override fun toString() = "${time.toString(2)} -> rotation: $rotation [$interpolation]"

    private fun slerp(
        quatA: Quaternion,
        quatB: Quaternion,
        f: Float,
        result: MutableQuaternion,
    ): MutableQuaternion {
        quatA.norm(qa)
        quatB.norm(qb)

        val t = f.clamp(0f, 1f)

        var dot = qa.dot(qb).clamp(-1f, 1f)
        if (dot < 0) {
            qa.scale(-1f)
            dot = -dot
        }

        if (dot > 0.9999995f) {
            qb.subtract(qa, result).scale(t).add(qa).norm()
        } else {
            val theta0 = acos(dot)
            val theta = theta0 * t

            qa.scale(-dot, qc).add(qb).norm()

            qa.scale(cos(theta))
            qc.scale(sin(theta))
            result.set(qa).add(qc)
        }
        return result
    }
}

class CubicRotationKey(
    time: Float,
    rotation: Quaternion,
    val startTan: Quaternion,
    val endTan: Quaternion,
) : RotationKey(time, rotation) {
    private val p0 = MutableQuaternion()
    private val p1 = MutableQuaternion()
    private val m0 = MutableQuaternion()
    private val m1 = MutableQuaternion()

    override fun apply(time: Float, next: RotationKey?, node: AnimationNode) {
        if (next == null) {
            node.setRotation(rotation)
        } else {
            val t = interpolationPos(time, next.time)
            val t2 = t * t
            val t3 = t * t * t

            val f1 = 2 * t3 - 3 * t2 + 1
            val f2 = t3 - 2 * t2 + t
            val f3 = -2 * t3 + 3 * t2
            val f4 = t3 - t2
            p0.set(rotation).scale(f1)
            m0.set(startTan).scale(f2)
            p1.set(next.rotation).scale(f3)
            m1.set(endTan).scale(f4)
            node.setRotation(p0.add(m0).add(p1).add(m1).norm())
        }
    }
}

open class TranslationKey(time: Float, val translation: Vec3f) :
    AnimationKey<TranslationKey>(time) {
    private val tmpTranslation = MutableVec3f()

    override fun apply(time: Float, next: TranslationKey?, node: AnimationNode) {
        if (next == null) {
            node.setTranslation(translation)
        } else {
            next.translation
                .subtract(translation, tmpTranslation)
                .scale(interpolationPos(time, next.time))
                .add(translation)
            node.setTranslation(tmpTranslation)
        }
    }

    override fun toString() = "${time.toString(2)} -> translation: $translation [$interpolation]"
}

class CubicTranslationKey(time: Float, translation: Vec3f, val startTan: Vec3f, val endTan: Vec3f) :
    TranslationKey(time, translation) {
    private val p0 = MutableVec3f()
    private val p1 = MutableVec3f()
    private val m0 = MutableVec3f()
    private val m1 = MutableVec3f()

    override fun apply(time: Float, next: TranslationKey?, node: AnimationNode) {
        if (next == null) {
            node.setTranslation(translation)
        } else {
            val t = interpolationPos(time, next.time)
            val t2 = t * t
            val t3 = t * t * t

            val f1 = 2 * t3 - 3 * t2 + 1
            val f2 = t3 - 2 * t2 + t
            val f3 = -2 * t3 + 3 * t2
            val f4 = t3 - t2
            p0.set(translation).scale(f1)
            m0.set(startTan).scale(f2)
            p1.set(next.translation).scale(f3)
            m1.set(endTan).scale(f4)
            node.setTranslation(p0.add(m0).add(p1).add(m1))
        }
    }
}

open class ScaleKey(time: Float, val scale: Vec3f) : AnimationKey<ScaleKey>(time) {
    private val tmpScale = MutableVec3f()

    override fun apply(time: Float, next: ScaleKey?, node: AnimationNode) {
        if (next == null) {
            node.setScale(scale)
        } else {
            next.scale.subtract(scale, tmpScale).scale(interpolationPos(time, next.time)).add(scale)
            node.setScale(tmpScale)
        }
    }

    override fun toString() = "${time.toString(2)} -> scale: $scale [$interpolation]"
}

class CubicScaleKey(time: Float, scale: Vec3f, val startTan: Vec3f, val endTan: Vec3f) :
    ScaleKey(time, scale) {
    private val p0 = MutableVec3f()
    private val p1 = MutableVec3f()
    private val m0 = MutableVec3f()
    private val m1 = MutableVec3f()

    override fun apply(time: Float, next: ScaleKey?, node: AnimationNode) {
        if (next == null) {
            node.setScale(scale)
        } else {
            val t = interpolationPos(time, next.time)
            val t2 = t * t
            val t3 = t * t * t

            val f1 = 2 * t3 - 3 * t2 + 1
            val f2 = t3 - 2 * t2 + t
            val f3 = -2 * t3 + 3 * t2
            val f4 = t3 - t2
            p0.set(scale).scale(f1)
            m0.set(startTan).scale(f2)
            p1.set(next.scale).scale(f3)
            m1.set(endTan).scale(f4)
            node.setScale(p0.add(m0).add(p1).add(m1))
        }
    }
}

open class WeightKey(time: Float, val weights: FloatArray) : AnimationKey<WeightKey>(time) {
    protected var tmpW = FloatArray(1)

    override fun apply(time: Float, next: WeightKey?, node: AnimationNode) {
        if (tmpW.size != weights.size) {
            tmpW = FloatArray(weights.size)
        }

        if (next == null) {
            for (i in weights.indices) {
                tmpW[i] = weights[i]
            }
        } else {
            for (i in weights.indices) {
                tmpW[i] =
                    (next.weights[i] - weights[i]) * interpolationPos(time, next.time) + weights[i]
            }
        }
        node.setWeights(tmpW)
    }

    override fun toString() =
        "${time.toString(2)} -> weight: (${weights.joinToString(", ")}) [$interpolation]"
}

class CubicWeightKey(
    time: Float,
    weights: FloatArray,
    val startTan: FloatArray,
    val endTan: FloatArray,
) : WeightKey(time, weights) {
    override fun apply(time: Float, next: WeightKey?, node: AnimationNode) {
        if (tmpW.size != weights.size) {
            tmpW = FloatArray(weights.size)
        }

        if (next == null) {
            for (i in weights.indices) {
                tmpW[i] = weights[i]
            }
        } else {
            val t = interpolationPos(time, next.time)
            val t2 = t * t
            val t3 = t * t * t

            val f1 = 2 * t3 - 3 * t2 + 1
            val f2 = t3 - 2 * t2 + t
            val f3 = -2 * t3 + 3 * t2
            val f4 = t3 - t2

            for (i in weights.indices) {
                val p0 = weights[i] * f1
                val m0 = startTan[i] * f2
                val p1 = next.weights[i] * f3
                val m1 = endTan[i] * f4

                tmpW[i] = p0 + m0 + p1 + m1
            }
        }
        node.setWeights(tmpW)
    }
}
