package com.littlekt.tools.texturepacker

import com.littlekt.util.packer.PackingOptions

/**
 * @author Colton Daily
 * @date 1/31/2022
 */
fun main(args: Array<String>) {
    val packer =
        TexturePacker(
            TexturePackerConfig().apply {
                inputDir = "./art/export_tiles"
                outputDir = "./art/default_assets"
                outputName = "default_tiles"
                crop = TexturePackerConfig.CropType.FLUSH_POSITION
                packingOptions =
                    PackingOptions(
                        outputPagesAsPowerOfTwo = false,
                        allowRotation = false,
                        extrude = 2
                    )
            }
        )
    packer.process()
    packer.pack()
}
