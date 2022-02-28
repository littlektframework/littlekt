package com.lehaine.littlekt.tools.texturepacker

import com.lehaine.littlekt.util.packer.PackingOptions

/**
 * @author Colton Daily
 * @date 1/31/2022
 */

fun main(args: Array<String>) {
    val packer = TexturePacker(TexturePackerConfig().apply {
        inputDir = "./art/export_tiles"
        outputDir = "./art/default_assets"
        outputName = "default_tiles"
        packingOptions = PackingOptions(
            outputPagesAsPowerOfTwo = false,
            allowRotation = true
        )
    })
    packer.process()
    packer.pack()
}