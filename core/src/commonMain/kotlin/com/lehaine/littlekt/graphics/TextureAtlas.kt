package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.file.atlas.AtlasInfo
import com.lehaine.littlekt.file.atlas.AtlasPage

/**
 * Holds all the TextureSlice entries of the atlas that was read and loaded in.
 * @see [com.lehaine.littlekt.file.Vfs.loadAtlas]
 * @author Colton Daily
 * @date 11/27/2021
 */
class TextureAtlas internal constructor(private val textures: Map<String, Texture>, info: AtlasInfo) {
    constructor(textures: Map<String, Texture>) : this(textures, AtlasInfo(AtlasPage.Meta(), listOf()))

    /**
     * All the entries in this [TextureAtlas].
     */
    val entries = info.pages.flatMap { page ->
        page.frames.map { frame ->
            Entry(frame, page)
        }
    }

    val entriesMap = entries.associateBy { it.name }

    /**
     * @param prefix the name prefix of the [TextureSlice]
     * @returns the first [Entry] that matches the supplied prefix
     */
    fun getByPrefix(prefix: String): Entry = entries.first { it.name.startsWith(prefix) }

    operator fun get(name: String): Entry = entriesMap[name] ?: error("Can't find $name in atlas.")

    /**
     * Contains the name,[TextureSlice], and the [Texture] for this entry of the atlas.
     */
    inner class Entry internal constructor(info: AtlasPage.Frame, page: AtlasPage) {
        private val frame = info.applyRotation()
        val texture = textures[page.meta.image] ?: error("Can't find ${page.meta.image} in ${textures.keys}")
        val slice = TextureSlice(texture, frame.frame.x, frame.frame.y, frame.frame.w, frame.frame.h)
        val name get() = frame.name
    }
}