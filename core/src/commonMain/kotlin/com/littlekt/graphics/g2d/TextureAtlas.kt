package com.littlekt.graphics.g2d

import com.littlekt.Releasable
import com.littlekt.file.atlas.AtlasInfo
import com.littlekt.file.atlas.AtlasPage
import com.littlekt.graphics.Texture
import com.littlekt.math.Rect
import com.littlekt.util.internal.compareName

/**
 * Holds all the TextureSlice entries of the atlas that was read and loaded in.
 *
 * @author Colton Daily
 * @date 11/27/2021
 */
class TextureAtlas
internal constructor(private val textures: Map<String, Texture>, info: AtlasInfo) : Releasable {
    constructor(
        textures: Map<String, Texture>
    ) : this(textures, AtlasInfo(AtlasPage.Meta(), listOf()))

    /** All the entries in this [TextureAtlas]. */
    val entries =
        info.pages
            .flatMap { page -> page.frames.map { frame -> Entry(frame, page) } }
            .sortedWith { o1, o2 -> o1.name.compareName(o2.name) }

    val entriesMap = entries.associateBy { it.name }

    /**
     * @param prefix the name prefix of the [TextureSlice]
     * @returns the first [Entry] that matches the supplied prefix
     */
    fun getByPrefix(prefix: String): Entry =
        entries.firstOrNull { it.name.startsWith(prefix) }
            ?: throw NoSuchElementException("'$prefix' does not exist in this texture atlas!")

    operator fun get(name: String): Entry =
        entriesMap[name] ?: throw NoSuchElementException("Can't find $name in atlas.")

    /** Contains the name,[TextureSlice], and the [Texture] for this entry of the atlas. */
    inner class Entry internal constructor(info: AtlasPage.Frame, page: AtlasPage) {
        private val frame = info
        val texture =
            textures[page.meta.image] ?: error("Can't find ${page.meta.image} in ${textures.keys}")
        val slice =
            TextureSlice(texture, frame.frame.x, frame.frame.y, frame.frame.w, frame.frame.h)
                .apply {
                    rotated = frame.rotated
                    if (frame.trimmed) {
                        virtualFrame =
                            Rect(
                                frame.spriteSourceSize.x.toFloat(),
                                frame.spriteSourceSize.y.toFloat(),
                                frame.spriteSourceSize.w.toFloat(),
                                frame.spriteSourceSize.h.toFloat()
                            )
                        originalWidth = frame.sourceSize.w
                        originalHeight = frame.sourceSize.h
                    }
                }
        val name
            get() = frame.name

        override fun toString(): String {
            return "Entry(texture=$texture, slice=$slice, name='$name', frame='$frame')"
        }
    }

    override fun release() {
        textures.values.forEach { it.release() }
    }
}
