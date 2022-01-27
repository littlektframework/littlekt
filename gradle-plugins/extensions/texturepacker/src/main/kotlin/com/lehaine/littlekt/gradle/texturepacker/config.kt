package com.lehaine.littlekt.gradle.texturepacker

/**
 * @author Colton Daily
 * @date 1/27/2022
 */
open class LittleKtConfig {

    var texturePackerConfig = TexturePackerConfig()
}

/**
 * @author Colton Daily
 * @date 1/27/2022
 */
open class TexturePackerConfig {

    /**
     * The input directory with the images to pack.
     */
    var inputDir: String = "src/commonMain/resources/"
        set(value) {
            field = if (value.endsWith("/")) {
                value
            } else {
                "$value/"
            }
        }

    /**
     * The name of the output atlas file.
     */
    var outputName: String = "textures.atlas.json"

    /**
     * The output directory to save the atlas files to.
     */
    var outputDir: String = "src/commonMain/resources/atlases/"
        set(value) {
            field = if (value.endsWith("/")) {
                value
            } else {
                "$value/"
            }
        }

    var packingOptions = PackingOptions()
}

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