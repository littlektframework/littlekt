package com.lehaine.littlekt.file.vfs

import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.file.UnsupportedFileTypeException
import com.lehaine.littlekt.file.atlas.AtlasInfo
import com.lehaine.littlekt.file.atlas.AtlasPage
import com.lehaine.littlekt.file.ldtk.LDtkMapLoader
import com.lehaine.littlekt.file.ldtk.ProjectJson
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureAtlas
import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.font.CharacterSets
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkWorld
import com.lehaine.littlekt.util.internal.unquote
import kotlinx.serialization.decodeFromString

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
/**
 * Loads a [TextureAtlas] from the given path. Currently, supports only JSON atlas files.
 * @return the texture atlas
 */
suspend fun VfsFile.readAtlas(): TextureAtlas {
    val data = readString()
    val info = when {
        data.startsWith("{") -> {
            val page = vfs.json.decodeFromString<AtlasPage>(data)
            AtlasInfo(page.meta, listOf(page))
        }
        data.startsWith('\n') -> TODO("Implement text atlas format")
        data.startsWith("\r\n") -> TODO("Implement text atlas format")
        else -> throw UnsupportedFileTypeException("This atlas format is not supported! ($path)")
    }

    val textures = info.pages.associate {
        it.meta.image to VfsFile(vfs, it.meta.image).readTexture()
    }
    return TextureAtlas(textures, info)
}

suspend fun VfsFile.readTtfFont(chars: String = CharacterSets.LATIN_ALL): TtfFont {
    val data = read()
    return TtfFont(chars).also { it.load(data) }
}

/**
 * Reads a bitmap font.
 */
suspend fun VfsFile.readBitmapFont(filter: TexMagFilter = TexMagFilter.NEAREST, mipmaps: Boolean = true): BitmapFont {
    val data = readString()
    val textures = mutableMapOf<Int, Texture>()
    if (data.startsWith("info")) {
        return readBitmapFontTxt(data, this, textures, filter, mipmaps)
    } else {
        TODO("Unsupported font type.")
    }
}

private suspend fun readBitmapFontTxt(
    data: String,
    fontFile: VfsFile,
    textures: MutableMap<Int, Texture>,
    filter: TexMagFilter,
    mipmaps: Boolean
): BitmapFont {
    val kernings = mutableListOf<BitmapFont.Kerning>()
    val glyphs = mutableListOf<BitmapFont.Glyph>()
    var lineHeight = 16f
    var fontSize = 16f
    var base: Float? = null

    val lines = data.split("\n")
    lines.forEach { rline ->
        val line = rline.trim()
        val map = linkedMapOf<String, String>()

        line.split(' ').forEach {
            val (key, value) = it.split('=') + listOf("", "")
            map[key] = value
        }

        when {
            line.startsWith("info") -> {
                fontSize = map["size"]?.toFloat() ?: 16f
            }
            line.startsWith("page") -> {
                val id = map["id"]?.toInt() ?: 0
                val file = map["file"]?.unquote() ?: error("Page without file")
                textures[id] = fontFile.parent[file].readTexture(magFilter = filter, mipmaps = mipmaps)
            }
            line.startsWith("common ") -> {
                lineHeight = map["lineHeight"]?.toFloatOrNull() ?: 16f
                base = map["base"]?.toFloatOrNull()
            }
            line.startsWith("char ") -> {
                val page = map["page"]?.toIntOrNull() ?: 0
                val texture = textures[page] ?: textures.values.first()
                val id = map["id"]?.toIntOrNull() ?: 0

                glyphs += BitmapFont.Glyph(
                    fontSize = fontSize,
                    id = id,
                    slice = TextureSlice(
                        texture,
                        map["x"]?.toIntOrNull() ?: 0,
                        map["y"]?.toIntOrNull() ?: 0,
                        map["width"]?.toIntOrNull() ?: 0,
                        map["height"]?.toIntOrNull() ?: 0
                    ),
                    xoffset = map["xoffset"]?.toIntOrNull() ?: 0,
                    yoffset = map["yoffset"]?.toIntOrNull() ?: 0,
                    xadvance = map["xadvance"]?.toIntOrNull() ?: 0
                )
            }
            line.startsWith("kerning ") -> {
                kernings += BitmapFont.Kerning(
                    first = map["first"]?.toIntOrNull() ?: 0,
                    second = map["second"]?.toIntOrNull() ?: 0,
                    amount = map["amount"]?.toIntOrNull() ?: 0
                )
            }
        }
    }

    return BitmapFont(
        fontSize = fontSize,
        lineHeight = lineHeight,
        base = base ?: lineHeight,
        textures = textures.values.toList(),
        glyphs = glyphs.associateBy { it.id },
        kernings = kernings.associateBy { BitmapFont.Kerning.buildKey(it.first, it.second) })
}

private val mapCache = mutableMapOf<String, LDtkMapLoader>()

/**
 * Reads the [VfsFile] as a [LDtkWorld]. Any loaders and assets will be cached for reuse/reloading.
 * @param loadAllLevels if true this will load all the external levels and their dependencies. They then will all be available
 * in [LDtkWorld.levels]; if false it will load the specified [levelIdx] as the default and only level.
 * @param levelIdx the index of the level to load if [loadAllLevels] is false.
 * @param tilesetBorder the border thickness of each slice when loading the tileset to prevent bleeding
 * @return the loaded LDtk map
 * @see [VfsFile.readLDtkLevel]
 */
suspend fun VfsFile.readLDtkMap(loadAllLevels: Boolean = true, levelIdx: Int = 0, tilesetBorder: Int = 2): LDtkWorld {
    val loader = mapCache.getOrPut(path) {
        val project = decodeFromString<ProjectJson>()
        LDtkMapLoader(this, project).also { it.levelLoader.sliceBorder = tilesetBorder }
    }
    return loader.loadMap(loadAllLevels, levelIdx).also {
        it.onDispose = {
            loader.dispose()
            mapCache.remove(path)
        }
    }
}

/**
 * Reads the [VfsFile] as a [LDtkWorld] and loads the level specified by [levelIdx].
 * Any loaders and assets will be cached for reuse/reloading.
 * @param levelIdx the index of the level to load
 * @param tilesetBorder the border thickness of each slice when loading the tileset to prevent bleeding
 * @return the loaded LDtk level
 */
suspend fun VfsFile.readLDtkLevel(levelIdx: Int, tilesetBorder: Int = 2): LDtkLevel {
    val loader = mapCache.getOrPut(path) {
        val project = decodeFromString<ProjectJson>()
        LDtkMapLoader(this, project).also { it.levelLoader.sliceBorder = tilesetBorder }
    }
    return loader.loadLevel(levelIdx)
}

/**
 * Loads an image from the path as a [Texture]. This will call [Texture.prepare] before returning!
 * @return the loaded texture
 */
expect suspend fun VfsFile.readTexture(
    minFilter: TexMinFilter = TexMinFilter.NEAREST,
    magFilter: TexMagFilter = TexMagFilter.NEAREST,
    mipmaps: Boolean = true
): Texture

/**
 * Loads an image from the path as a [Pixmap].
 * @return the loaded texture
 */
expect suspend fun VfsFile.readPixmap(): Pixmap

/**
 * Loads audio from the path as an [AudioClip].
 * @return the loaded audio clip
 */
expect suspend fun VfsFile.readAudioClip(): AudioClip

/**
 * Write pixmap to disk.
 */
expect suspend fun VfsFile.writePixmap(pixmap: Pixmap)