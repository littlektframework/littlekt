package com.lehaine.littlekt.tools.texturepacker.template

import com.lehaine.littlekt.tools.StringCompare
import com.lehaine.littlekt.tools.texturepacker.ImageRectData
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
    val frames: List<Frame>
) {

    @Serializable
    internal data class Frame(
        @SerialName("filename")
        val name: String,
        val frame: Rect,
        val rotated: Boolean,
        val trimmed: Boolean,
        val spriteSourceSize: Rect,
        val sourceSize: Size
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
        val size: Size = Size(0, 0)
    )
}

internal fun createAtlasPage(image: BufferedImage, imageName: String, data: List<ImageRectData>) =
    AtlasPage(createMeta(image, imageName), data.toFrames())

internal fun createMeta(image: BufferedImage, imageName: String) =
    AtlasPage.Meta(image = imageName, format = "RGBA8888", size = AtlasPage.Size(image.width, image.height))

internal fun List<ImageRectData>.toFrames(): List<AtlasPage.Frame> {
    val output = mutableListOf<AtlasPage.Frame>()
    forEach { data ->
        output += AtlasPage.Frame(
            data.name,
            AtlasPage.Rect(data.x, data.y, data.width, data.height),
            data.isRotated,
            data.offsetX != 0 || data.offsetY != 0 || data.regionWidth != data.width || data.regionHeight != data.height,
            AtlasPage.Rect(data.offsetX, data.offsetY, data.regionWidth, data.regionHeight),
            AtlasPage.Size(data.originalWidth, data.originalHeight)
        )

        data.aliases.forEach {
            output += AtlasPage.Frame(
                it.name,
                AtlasPage.Rect(data.x, data.y, data.width, data.height),
                data.isRotated,
                data.offsetX != 0 || data.offsetY != 0 || data.regionWidth != data.width || data.regionHeight != data.height,
                AtlasPage.Rect(data.offsetX, data.offsetY, data.regionWidth, data.regionHeight),
                AtlasPage.Size(data.originalWidth, data.originalHeight)
            )
        }
    }
    output.sortWith { o1, o2 -> StringCompare.compareNames(o1.name, o2.name) }
    return output.toList()
}