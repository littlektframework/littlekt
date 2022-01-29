package com.lehaine.littlekt.gradle.texturepacker

import com.lehaine.littlekt.tools.texturepacker.PackingOptions

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