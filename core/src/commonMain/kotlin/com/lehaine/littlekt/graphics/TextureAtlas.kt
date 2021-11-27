package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.io.atlas.AtlasInfo
import com.lehaine.littlekt.io.atlas.AtlasPage

/**
 * @author Colton Daily
 * @date 11/27/2021
 */
class TextureAtlas(val textures: Map<String, Texture>, val info: AtlasInfo) {
    val entries = info.pages.flatMap { page ->
        page.frames.map { frame ->
            Entry(frame, page)
        }
    }

    val entriesMap = entries.associateBy { it.name }

    operator fun get(name: String): Entry = entriesMap[name] ?: error("Can't find $name in atlas.")

    inner class Entry(info: AtlasPage.Frame, page: AtlasPage) {
        private val frame = info.applyRotation()
        val texture = textures[page.meta.image] ?: error("Can't find ${page.meta.image} in ${textures.keys}")
        val slice = TextureSlice(texture, frame.frame.x, frame.frame.y, frame.frame.w, frame.frame.h)
        val name get() = frame.name
    }
}