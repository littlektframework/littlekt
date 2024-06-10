package com.littlekt.util

import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f

/**
 * An abstract utility class to calculate scaling viewports.
 *
 * @author Colton Daily
 * @date 12/21/2021
 */
abstract class Scaler {
    protected val temp = MutableVec2f()

    /**
     * Apply this [Scaler] to calculate the new size
     *
     * @param sourceWidth the source width
     * @param sourceHeight the source height
     * @param targetWidth the target width
     * @param targetHeight the target height
     * @return the new calculated size
     */
    abstract fun apply(
        sourceWidth: Float,
        sourceHeight: Float,
        targetWidth: Float,
        targetHeight: Float
    ): Vec2f

    fun apply(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): Vec2f =
        apply(
            sourceWidth.toFloat(),
            sourceHeight.toFloat(),
            targetWidth.toFloat(),
            targetHeight.toFloat()
        )

    /**
     * The [Scaler] will maintain its aspect ratio while attempting to fit as much as possible onto
     * the screen
     */
    class Fit : Scaler() {
        override fun apply(
            sourceWidth: Float,
            sourceHeight: Float,
            targetWidth: Float,
            targetHeight: Float
        ): Vec2f {
            val targetRatio = targetHeight / targetWidth
            val sourceRatio = sourceHeight / sourceWidth
            val scale =
                if (targetRatio > sourceRatio) targetWidth / sourceWidth
                else targetHeight / sourceHeight
            temp.set(sourceWidth * scale, sourceHeight * scale)
            return temp
        }
    }

    class Fill : Scaler() {
        override fun apply(
            sourceWidth: Float,
            sourceHeight: Float,
            targetWidth: Float,
            targetHeight: Float
        ): Vec2f {
            val targetRatio = targetHeight / targetWidth
            val sourceRatio = sourceHeight / sourceWidth
            val scale =
                if (targetRatio < sourceRatio) targetWidth / sourceWidth
                else targetHeight / sourceHeight
            temp.set(sourceWidth * scale, sourceHeight * scale)
            return temp
        }
    }

    /** A [Scaler] that is stretched to fit the target size in the X-direaction. */
    class FillX : Scaler() {
        override fun apply(
            sourceWidth: Float,
            sourceHeight: Float,
            targetWidth: Float,
            targetHeight: Float
        ): Vec2f {
            val scale = targetWidth / sourceWidth
            temp.set(sourceWidth * scale, sourceHeight * scale)
            return temp
        }
    }

    /** A [Scaler] that is stretched to fit the target size in the Y-direaction. */
    class FillY : Scaler() {
        override fun apply(
            sourceWidth: Float,
            sourceHeight: Float,
            targetWidth: Float,
            targetHeight: Float
        ): Vec2f {
            val scale = targetHeight / sourceHeight
            temp.set(sourceWidth * scale, sourceHeight * scale)
            return temp
        }
    }

    /** A [Scaler] that is stretched to fit the target size. */
    class Stretch : Scaler() {
        override fun apply(
            sourceWidth: Float,
            sourceHeight: Float,
            targetWidth: Float,
            targetHeight: Float
        ): Vec2f {
            temp.set(targetWidth, targetHeight)
            return temp
        }
    }

    /** A [Scaler] that is stretched to fit the target size in the X-direction. */
    class StretchX : Scaler() {
        override fun apply(
            sourceWidth: Float,
            sourceHeight: Float,
            targetWidth: Float,
            targetHeight: Float
        ): Vec2f {
            temp.set(targetWidth, sourceHeight)
            return temp
        }
    }

    /** A [Scaler] that is stretched to fit the target size in the Y-direction. */
    class StretchY : Scaler() {
        override fun apply(
            sourceWidth: Float,
            sourceHeight: Float,
            targetWidth: Float,
            targetHeight: Float
        ): Vec2f {
            temp.set(sourceWidth, targetHeight)
            return temp
        }
    }

    /** A [Scaler] that does no scaling and returns the source size. */
    class None : Scaler() {
        override fun apply(
            sourceWidth: Float,
            sourceHeight: Float,
            targetWidth: Float,
            targetHeight: Float
        ): Vec2f {
            temp.x = sourceWidth
            temp.y = sourceHeight
            return temp
        }
    }
}
