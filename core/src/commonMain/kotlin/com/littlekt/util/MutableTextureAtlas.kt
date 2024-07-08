package com.littlekt.util

import com.littlekt.Context
import com.littlekt.file.atlas.AtlasInfo
import com.littlekt.file.atlas.AtlasPage
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.PixmapTexture
import com.littlekt.graphics.Texture
import com.littlekt.graphics.drawSlice
import com.littlekt.graphics.g2d.TextureAtlas
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.webgpu.TextureFormat
import com.littlekt.util.packer.BinRect
import com.littlekt.util.packer.MaxRectsPacker
import com.littlekt.util.packer.PackingOptions

/**
 * Allows building of a [TextureAtlas] by combining existing textures, texture slices, and texture
 * atlases.
 *
 * @author Colton Daily
 * @date 2/8/2022
 */
class MutableTextureAtlas(val context: Context, options: PackingOptions = PackingOptions()) {
    constructor(
        context: Context,
        width: Int = 4096,
        height: Int = 4096,
        padding: Int = 2
    ) : this(
        context,
        PackingOptions(
            maxWidth = width,
            maxHeight = height,
            paddingVertical = padding,
            paddingHorizontal = padding
        )
    )

    private val packer = MaxRectsPacker(options)
    private val entries = mutableListOf<Entry>()

    private data class Entry(val slice: TextureSlice, val name: String) :
        BinRect(0, 0, slice.width, slice.height)

    val size: Int
        get() = entries.size

    val width: Int
        get() = packer.width

    val height: Int
        get() = packer.height

    fun add(slice: TextureSlice, name: String = "slice$size"): MutableTextureAtlas {
        entries += Entry(slice, name)
        return this
    }

    fun add(slices: List<TextureSlice>) = slices.forEach { add(it) }

    fun add(atlas: TextureAtlas): MutableTextureAtlas {
        atlas.entries.forEach { entries += Entry(it.slice, it.name) }
        return this
    }

    fun toImmutable(
        format: TextureFormat =
            if (context.graphics.preferredFormat.srgb) TextureFormat.RGBA8_UNORM_SRGB
            else TextureFormat.RGBA8_UNORM,
        useMiMaps: Boolean = true
    ): TextureAtlas {
        packer.add(entries)
        val pages = mutableListOf<AtlasPage>()
        val textures = mutableMapOf<String, Texture>()
        packer.bins.forEachIndexed { index, bin ->
            val textureName = "texture_$index"
            val meta = AtlasPage.Meta(image = textureName)
            val pixmap = Pixmap(bin.width, bin.height)
            val frames = mutableListOf<AtlasPage.Frame>()
            bin.rects.forEach {
                it as Entry
                val slice = it.slice
                pixmap.drawSlice(it.x, it.y, slice)
                frames +=
                    AtlasPage.Frame(
                        it.name,
                        AtlasPage.Rect(it.x, it.y, it.width, it.height),
                        it.isRotated,
                        slice.offsetX != 0 ||
                            slice.offsetY != 0 ||
                            slice.packedWidth != slice.width ||
                            slice.packedHeight != slice.height,
                        AtlasPage.Rect(
                            slice.offsetX,
                            slice.offsetY,
                            slice.packedWidth,
                            slice.packedHeight
                        ),
                        AtlasPage.Size(slice.originalWidth, slice.originalHeight)
                    )
            }
            pages += AtlasPage(meta, frames = frames)
            textures["texture_$index"] = PixmapTexture(context.graphics.device, format, pixmap)
        }
        entries.clear()
        packer.reset()
        return TextureAtlas(textures, AtlasInfo(AtlasPage.Meta(), pages))
    }
}
