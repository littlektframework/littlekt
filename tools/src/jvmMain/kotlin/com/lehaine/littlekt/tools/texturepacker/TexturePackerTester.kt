package com.lehaine.littlekt.tools.texturepacker

/**
 * @author Colton Daily
 * @date 1/31/2022
 */

fun main(args: Array<String>) {
    val packer = TexturePacker(TexturePackerConfig().apply {
        inputDir = "./art/export_tiles"
        outputDir = "./art/default_assets"
        outputName = "default_tiles"
        packingOptions = PackingOptions().apply {
            outputPagesAsPowerOfTwo = false
            allowRotation = true
        }
    })
    packer.process()
    packer.pack()
}