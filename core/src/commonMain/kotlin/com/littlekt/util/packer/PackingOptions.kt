package com.littlekt.util.packer

/**
 * @author Colton Daily
 * @date 1/28/2022
 */
open class PackingOptions(
    /** If true, images will be rotated 90 degrees in an attempt to pack more efficiently. */
    var allowRotation: Boolean = false,

    /** Number of pixels between packed images horizontally */
    var paddingHorizontal: Int = 2,

    /** Number of pixels between packed images vertically */
    var paddingVertical: Int = 2,

    /** If true, pages will have power of two dimensions. */
    var outputPagesAsPowerOfTwo: Boolean = true,
    var maxWidth: Int = 4096,
    var maxHeight: Int = 4096,
    var edgeBorder: Int = 2,

    /**
     * If true, RGB values for transparent pixels are set based on the RGB values of the nearest
     * non-transparent pixels. This prevents filtering artifacts when RGB values are sampled for
     * transparent pixels.
     */
    var bleed: Boolean = true,

    /**
     * The amount of bleed iterations that should be performed. Use greater values such as 4 or 8 if
     * you’re having artifacts when downscaling your textures.
     */
    var bleedIterations: Int = 2,

    /** Repeats the packed image pixels at the border. Does not change the packed image size. */
    var extrude: Int = 1,
) {

    fun clone() =
        PackingOptions().apply {
            allowRotation = this@PackingOptions.allowRotation
            paddingHorizontal = this@PackingOptions.paddingHorizontal
            paddingVertical = this@PackingOptions.paddingVertical
            outputPagesAsPowerOfTwo = this@PackingOptions.outputPagesAsPowerOfTwo
            maxWidth = this@PackingOptions.maxWidth
            maxHeight = this@PackingOptions.maxHeight
            edgeBorder = this@PackingOptions.edgeBorder
            bleed = this@PackingOptions.bleed
            bleedIterations = this@PackingOptions.bleedIterations
            extrude = this@PackingOptions.extrude
        }

    override fun toString(): String {
        return "PackingOptions(allowRotation=$allowRotation, paddingHorizontal=$paddingHorizontal, paddingVertical=$paddingVertical, outputPagesAsPowerOfTwo=$outputPagesAsPowerOfTwo, maxWidth=$maxWidth, maxHeight=$maxHeight, edgeBorder=$edgeBorder)"
    }
}
