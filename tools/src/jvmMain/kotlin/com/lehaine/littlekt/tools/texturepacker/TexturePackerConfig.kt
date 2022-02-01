package com.lehaine.littlekt.tools.texturepacker

/**
 * @author Colton Daily
 * @date 1/31/2022
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
    var outputName: String = "atlas"

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

    var ignoreDirs: List<String> = listOf()
    var ignoreFiles: List<String> = listOf()

    var trim = true
    var trimMargin = 1

    var packingOptions = PackingOptions()
}