package com.lehaine.littlekt.util.packer

/**
 * @author Colton Daily
 * @date 1/28/2022
 */
open class PackingOptions(
    /**
     * If true, images will be rotated 90 degrees in an attempt to pack more efficiently.
     */
    var allowRotation: Boolean = false,

    /**
     * Number of pixels between packed images horizontally
     */
    var paddingHorizontal: Int = 2,

    /**
     * Number of pixels between packed images vertically
     */
    var paddingVertical: Int = 2,

    /**
     * If true, pages will have power of two dimensions.
     */
    var outputPagesAsPowerOfTwo: Boolean = true,

    var maxWidth: Int = 4096,
    var maxHeight: Int = 4096,

    var edgeBorder: Int = 2,
) {

    fun clone() = PackingOptions().apply {
        allowRotation = this@PackingOptions.allowRotation
        paddingHorizontal = this@PackingOptions.paddingHorizontal
        paddingVertical = this@PackingOptions.paddingVertical
        outputPagesAsPowerOfTwo = this@PackingOptions.outputPagesAsPowerOfTwo
        maxWidth = this@PackingOptions.maxWidth
        maxHeight = this@PackingOptions.maxHeight
        edgeBorder = this@PackingOptions.edgeBorder
    }

    override fun toString(): String {
        return "PackingOptions(allowRotation=$allowRotation, paddingHorizontal=$paddingHorizontal, paddingVertical=$paddingVertical, outputPagesAsPowerOfTwo=$outputPagesAsPowerOfTwo, maxWidth=$maxWidth, maxHeight=$maxHeight, edgeBorder=$edgeBorder)"
    }
}