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
import com.lehaine.littlekt.graphics.font.CharacterSets
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
import kotlinx.serialization.decodeFromString

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
/**
 * Loads a [TextureAtlas] from the given path. Currently supports only JSON atlas files.
 * @param assetPath the path to the atlas file to load
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

private val mapCache = mutableMapOf<String, LDtkMapLoader>()

/**
 * Reads the [VfsFile] as a [LDtkTileMap]. Any loaders and assets will be cached for reuse/reloading.
 * @param loadAllLevels if true this will load all the external levels and their dependencies. They then will all be available
 * in [LDtkTileMap.levels]; if false it will load the specified [levelIdx] as the default and only level.
 * @param levelIdx the index of the level to load if [loadAllLevels] is false.
 * @return the loaded LDtk map
 * @see [VfsFile.readLDtkLevel]
 */
suspend fun VfsFile.readLDtkMap(loadAllLevels: Boolean = true, levelIdx: Int = 0): LDtkTileMap {
    val loader = mapCache.getOrPut(path) {
        val project = decodeFromString<ProjectJson>()
        LDtkMapLoader(this, project)
    }
    return loader.loadMap(loadAllLevels, levelIdx)
}

/**
 * Reads the [VfsFile] as a [LDtkTileMap] and loads the level specified by [levelIdx].
 * Any loaders and assets will be cached for reuse/reloading.
 * @param levelIdx the index of the level to load
 * @return the loaded LDtk level
 */
suspend fun VfsFile.readLDtkLevel(levelIdx: Int): LDtkLevel {
    val loader = mapCache.getOrPut(path) {
        val project = decodeFromString<ProjectJson>()
        LDtkMapLoader(this, project)
    }
    return loader.loadLevel(levelIdx)
}

/**
 * Loads an image from the path as a [Texture]. This will call [Texture.prepare] before returning!
 * @return the loaded texture
 */
expect suspend fun VfsFile.readTexture(): Texture

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