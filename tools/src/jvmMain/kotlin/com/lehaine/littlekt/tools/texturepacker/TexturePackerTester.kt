package com.lehaine.littlekt.tools.texturepacker

/**
 * @author Colton Daily
 * @date 1/31/2022
 */

fun main(args: Array<String>) {
    val packer = TexturePacker(TexturePackerConfig().apply {
        inputDir = "./art/export_tiles/raw"
        outputDir = "./art/output_atlas"
        outputName = "tile.atlas"
    })
    packer.process()
    packer.pack()
}