package com.lehaine.littlekt.tools.texturepacker.template

import com.lehaine.littlekt.tools.StringCompare
import com.lehaine.littlekt.tools.texturepacker.ImageRectData
import com.lehaine.littlekt.tools.texturepacker.TexturePackerConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.awt.image.BufferedImage

/**
 * @author Colton Daily
 * @date 1/31/2022
 */
@Serializable
internal data class AtlasPage(
    val meta: Meta,
    val frames: List<Frame>,
) {

    @Serializable
    internal data class Frame(
        @SerialName("filename")
        val name: String,
        val frame: Rect,
        val rotated: Boolean,
        val trimmed: Boolean,
        val spriteSourceSize: Rect,
        val sourceSize: Size,
    )

    @Serializable
    internal data class Rect(val x: Int, val y: Int, val w: Int, val h: Int)

    @Serializable
    internal data class Size(val w: Int, val h: Int)

    @Serializable
    internal data class Meta(
        val app: String = "https://littlekt.com",
        val version: String = "1.0",
        val image: String = "",
        val format: String = "",
        val scale: Float = 1f,
        val size: Size = Size(0, 0),
        @SerialName("related_multi_packs")
        val relatedMultiPacks: List<String> = listOf(),
    )
}

internal fun createAtlasPage(
    image: BufferedImage,
    imageName: String,
    data: List<ImageRectData>,
    relatedMultiPacks: List<String>,
    cropType: TexturePackerConfig.CropType,
) =
    AtlasPage(createMeta(image, imageName, relatedMultiPacks), data.toFrames(cropType))

internal fun createMeta(image: BufferedImage, imageName: String, relatedMultiPacks: List<String>) =
    AtlasPage.Meta(
        image = imageName,
        format = "RGBA8888",
        size = AtlasPage.Size(image.width, image.height),
        relatedMultiPacks = relatedMultiPacks
    )

internal fun List<ImageRectData>.toFrames(cropType: TexturePackerConfig.CropType): List<AtlasPage.Frame> {
    val output = mutableListOf<AtlasPage.Frame>()
    forEach { data ->
        output += AtlasPage.Frame(
            name = data.name,
            frame = AtlasPage.Rect(
                data.x + data.extrude,
                data.y + data.extrude,
                data.width - data.extrude * 2,
                data.height - data.extrude * 2
            ),
            rotated = data.isRotated,
            trimmed = (cropType != TexturePackerConfig.CropType.FLUSH_POSITION && (data.offsetX != 0 || data.offsetY != 0)) || data.regionWidth != data.width - data.extrude * 2 || data.regionHeight != data.height - data.extrude * 2,
            spriteSourceSize = AtlasPage.Rect(
                if (cropType == TexturePackerConfig.CropType.FLUSH_POSITION) 0 else data.offsetX,
                if (cropType == TexturePackerConfig.CropType.FLUSH_POSITION) 0 else data.offsetY,
                data.regionWidth,
                data.regionHeight),
            sourceSize = AtlasPage.Size(data.originalWidth, data.originalHeight)
        )

        data.aliases.forEach {
            output += AtlasPage.Frame(
                name = it.name,
                frame = AtlasPage.Rect(
                    data.x + data.extrude,
                    data.y + data.extrude,
                    data.width - data.extrude * 2,
                    data.height - data.extrude * 2
                ),
                rotated = data.isRotated,
                trimmed = (cropType != TexturePackerConfig.CropType.FLUSH_POSITION && (data.offsetX != 0 || data.offsetY != 0)) || data.regionWidth != data.width - data.extrude * 2 || data.regionHeight != data.height - data.extrude * 2,
                spriteSourceSize = AtlasPage.Rect(
                    if (cropType == TexturePackerConfig.CropType.FLUSH_POSITION) 0 else data.offsetX,
                    if (cropType == TexturePackerConfig.CropType.FLUSH_POSITION) 0 else data.offsetY,
                    data.regionWidth,
                    data.regionHeight),
                sourceSize = AtlasPage.Size(data.originalWidth, data.originalHeight)
            )
        }
    }
    output.sortWith { o1, o2 -> StringCompare.compareNames(o1.name, o2.name) }
    return output.toList()
}