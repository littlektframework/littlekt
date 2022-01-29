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
}