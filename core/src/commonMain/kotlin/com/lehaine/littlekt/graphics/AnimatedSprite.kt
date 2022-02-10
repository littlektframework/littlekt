package com.lehaine.littlekt.graphics

/**
 * @author Colton Daily
 * @date 12/29/2021
 */
class AnimatedSprite(
    var slice: TextureSlice = Textures.white,
    var anchorX: Float = 0f,
    var anchorY: Float = 0f
) : AnimationPlayer<TextureSlice>() {

    var x: Float = 0f
    var y: Float = 0f
    var scaleX: Float = 0f
    var scaleY: Float = 0f

    init {
        onFrameChange = {
            slice = currentAnimation?.getFrame(it) ?: slice
        }
    }

    fun render(batch: Batch) {
        batch.draw(slice, x, y, slice.width * anchorX, slice.height * anchorY, scaleX = scaleX, scaleY = scaleY)
    }
}