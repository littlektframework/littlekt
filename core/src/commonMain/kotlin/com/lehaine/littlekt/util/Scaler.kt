package com.lehaine.littlekt.util

import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Vec2f

/**
 * @author Colton Daily
 * @date 12/21/2021
 */
sealed class Scaler {
    protected val temp = MutableVec2f()

    abstract fun apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vec2f
    fun apply(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): Vec2f =
        apply(sourceWidth.toFloat(), sourceHeight.toFloat(), targetWidth.toFloat(), targetHeight.toFloat())

    class Fit : Scaler() {
        override fun apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vec2f {
            val targetRatio = targetHeight / targetWidth
            val sourceRatio = sourceHeight / sourceWidth
            val scale = if (targetRatio > sourceRatio) targetWidth / sourceWidth else targetHeight / sourceHeight
            temp.set(sourceWidth * scale, sourceHeight * scale)
            return temp
        }
    }

    class Fill : Scaler() {
        override fun apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vec2f {
            val targetRatio = targetHeight / targetWidth
            val sourceRatio = sourceHeight / sourceWidth
            val scale = if (targetRatio < sourceRatio) targetWidth / sourceWidth else targetHeight / sourceHeight
            temp.set(sourceWidth * scale, sourceHeight * scale)
            return temp
        }

    }

    class FillX : Scaler() {
        override fun apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vec2f {
            val scale = targetWidth / sourceWidth
            temp.set(sourceWidth * scale, sourceHeight * scale)
            return temp
        }

    }

    class FillY : Scaler() {
        override fun apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vec2f {
            val scale = targetHeight / sourceHeight
            temp.set(sourceWidth * scale, sourceHeight * scale)
            return temp
        }

    }

    class Stretch : Scaler() {
        override fun apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vec2f {
            temp.set(targetWidth, targetHeight)
            return temp
        }

    }

    class StretchX : Scaler() {
        override fun apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vec2f {
            temp.set(targetWidth, sourceHeight)
            return temp
        }

    }

    class StretchY : Scaler() {
        override fun apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vec2f {
            temp.set(sourceWidth, targetHeight)
            return temp
        }

    }

    class None : Scaler() {
        override fun apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vec2f {
            temp.x = sourceWidth
            temp.y = sourceHeight
            return temp
        }
    }
}