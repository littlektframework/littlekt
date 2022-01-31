package com.lehaine.littlekt.tools.texturepacker

/**
 * @author Colton Daily
 * @date 1/28/2022
 */
open class PackingOptions {
    /**
     * If true, images will be rotated 90 degrees in an attempt to pack more efficiently.
     */
    var allowRotation: Boolean = false

    /**
     * Number of pixels between packed images horizontally
     */
    var paddingHorizontal = 2

    /**
     * Number of pixels between packed images vertically
     */
    var paddingVertical = 2

    /**
     * If true, pages will have power of two dimensions.
     */
    var outputPagesAsPowerOfTwo = true

    var maxWidth = 4096
    var maxHeight = 4096

    var edgeBorder = 2

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