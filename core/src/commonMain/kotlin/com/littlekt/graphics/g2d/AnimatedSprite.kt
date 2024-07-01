package com.littlekt.graphics.g2d

/**
 * A simple [AnimationPlayer] that uses [TextureSlice] as the keyframe type.
 *
 * @param slice initialize [TextureSlice] of the sprite
 * @param anchorX normalized x-coordinate of sprite to be used in rendering. `0f - 1f`.
 * @param anchorY normalized y-coordinate of sprite to be used in rendering. `0f - 1f`.
 * @author Colton Daily
 * @date 12/29/2021
 */
class AnimatedSprite(var slice: TextureSlice, var anchorX: Float = 0f, var anchorY: Float = 0f) :
    AnimationPlayer<TextureSlice>() {

    /** X-position of this sprite. */
    var x: Float = 0f

    /** Y-position of this sprite. */
    var y: Float = 0f

    /** X-scale of this sprite. */
    var scaleX: Float = 0f

    /** Y-scale of this sprite. */
    var scaleY: Float = 0f

    init {
        onFrameChange = { slice = currentAnimation?.getFrame(it) ?: slice }
    }

    /**
     * Renders this sprite with the given [batch].
     *
     * @param batch [Batch] to render the sprite.
     */
    fun render(batch: Batch) {
        batch.draw(
            slice,
            x,
            y,
            slice.width * anchorX,
            slice.height * anchorY,
            scaleX = scaleX,
            scaleY = scaleY
        )
    }
}
