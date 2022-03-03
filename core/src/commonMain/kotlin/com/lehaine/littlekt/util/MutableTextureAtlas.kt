package com.lehaine.littlekt.util

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.file.atlas.AtlasInfo
import com.lehaine.littlekt.file.atlas.AtlasPage
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.util.packer.BinRect
import com.lehaine.littlekt.util.packer.MaxRectsPacker
import com.lehaine.littlekt.util.packer.PackingOptions

/**
 * Allows building of a [TextureAtlas] by combining existing textures, texture slices, and texture atlases.
 * @author Colton Daily
 * @date 2/8/2022
 */
class MutableTextureAtlas(val context: Context, options: PackingOptions = PackingOptions()) {
    constructor(context: Context, width: Int = 4096, height: Int = 4096, padding: Int = 2) : this(
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

    private data class Entry(val slice: TextureSlice, val name: String) : BinRect(0, 0, slice.width, slice.height)

    val size get() = entries.size
    val width get() = packer.width
    val height get() = packer.height


    fun add(slice: TextureSlice, name: String = "slice$size"): MutableTextureAtlas {
        entries += Entry(slice, name)
        return this
    }

    fun add(slices: List<TextureSlice>) = slices.forEach { add(it) }

    fun add(atlas: TextureAtlas): MutableTextureAtlas {
        atlas.entries.forEach {
            entries += Entry(it.slice, it.name)
        }
        return this
    }

    fun toImmutable(useMiMaps: Boolean = true): TextureAtlas {
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
                frames += AtlasPage.Frame(
                    it.name,
                    AtlasPage.Rect(it.x, it.y, it.width, it.height),
                    it.isRotated,
                    slice.offsetX != 0 || slice.offsetY != 0 || slice.packedWidth != slice.width || slice.packedHeight != slice.height,
                    AtlasPage.Rect(slice.offsetX, slice.offsetY, slice.packedWidth, slice.packedHeight),
                    AtlasPage.Size(slice.originalWidth, slice.originalHeight)
                )
            }
            pages += AtlasPage(meta, frames = frames)
            textures["texture_$index"] = Texture(PixmapTextureData(pixmap, useMiMaps)).also { it.prepare(context) }
        }
        return TextureAtlas(textures, AtlasInfo(AtlasPage.Meta(), pages))
    }
}

/**
 * Combine a [TextureSlice] with a [TextureAtlas] into a new [TextureAtlas].
 *
 * **This does not dispose of the [TextureAtlas] or the [Texture]!!**
 *
 * @param slice the texture slice to combine
 * @param name the name to give the texture slice in the [TextureAtlas]
 * @param context the current [Context]
 */
fun TextureAtlas.combine(slice: TextureSlice, name: String, context: Context) =
    MutableTextureAtlas(context).apply {
        add(slice, name)
        add(this@combine)
    }.toImmutable()

/**
 * Combine a [Texture] with a [TextureAtlas] into a new [TextureAtlas].
 *
 * **This does not dispose of the [TextureAtlas] or the [Texture]!!**
 *
 * @param texture the texture to combine
 * @param name the name to give the texture in the [TextureAtlas]
 * @param context the current [Context]
 */
fun TextureAtlas.combine(texture: Texture, name: String, context: Context) = combine(texture.slice(), name, context)

/**
 * Combine another [TextureAtlas] with the current atlas to create a new [TextureAtlas].
 *
 * **This does not dispose of either [TextureAtlas]!!**
 *
 * @param atlas the texture atlas to combine
 * @param context the current [Context]
 */
fun TextureAtlas.combine(atlas: TextureAtlas, context: Context) =
    MutableTextureAtlas(context).apply {
        add(atlas)
        add(this@combine)
    }.toImmutable()