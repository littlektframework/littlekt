package com.lehaine.littlekt.file.vfs

import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.audio.AudioStream
import com.lehaine.littlekt.file.UnsupportedFileTypeException
import com.lehaine.littlekt.file.atlas.AtlasInfo
import com.lehaine.littlekt.file.atlas.AtlasPage
import com.lehaine.littlekt.file.createByteBuffer
import com.lehaine.littlekt.file.gltf.GltfFile
import com.lehaine.littlekt.file.gltf.toModel
import com.lehaine.littlekt.file.ldtk.LDtkMapData
import com.lehaine.littlekt.file.ldtk.LDtkMapLoader
import com.lehaine.littlekt.file.tiled.TiledMapData
import com.lehaine.littlekt.file.tiled.TiledMapLoader
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.g2d.TextureAtlas
import com.lehaine.littlekt.graphics.g2d.TextureSlice
import com.lehaine.littlekt.graphics.g2d.font.*
import com.lehaine.littlekt.graphics.g2d.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.g2d.tilemap.ldtk.LDtkWorld
import com.lehaine.littlekt.graphics.g2d.tilemap.tiled.TiledMap
import com.lehaine.littlekt.graph.node.node3d.Model
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.math.MutableVec4i
import com.lehaine.littlekt.util.internal.unquote
import com.lehaine.littlekt.util.toString
import kotlinx.serialization.decodeFromString
import kotlin.math.max

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
            val pages = mutableListOf(page)
            page.meta.relatedMultiPacks.forEach {
                pages += vfs.json.decodeFromString<AtlasPage>(parent[it].readString())
            }
            AtlasInfo(page.meta, pages)
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
 * @param filter the filter to assign any [Texture] that gets loaded
 * @param mipmaps whether the loaded [Texture] should use mipmaps
 * @param preloadedTextures instead of loading a [Texture] when parsing the bitmap font, this will use an existing
 * [TextureSlice]. This is useful if the bitmap font texture already exists in an atlas. Each slice in the list
 * is considered a page in the bitmap font. Disposing a [BitmapFont] that uses preloaded textures will not dispose
 * of the textures.
 */
suspend fun VfsFile.readBitmapFont(
    filter: TexMagFilter = TexMagFilter.NEAREST,
    mipmaps: Boolean = true,
    preloadedTextures: List<TextureSlice> = listOf(),
): BitmapFont {
    val data = readString()
    val textures = mutableMapOf<Int, Texture>()
    var pages = 0
    preloadedTextures.forEach { slice ->
        if (!textures.containsValue(slice.texture)) {
            textures[pages++] = slice.texture
        }
    }
    if (data.startsWith("info")) {
        return readBitmapFontTxt(data, this, textures, preloadedTextures, preloadedTextures.isEmpty(), filter, mipmaps)
    } else {
        TODO("Unsupported font type.")
    }
}

private suspend fun readBitmapFontTxt(
    data: String,
    fontFile: VfsFile,
    textures: MutableMap<Int, Texture>,
    preloadedTextures: List<TextureSlice>,
    loadTextures: Boolean,
    filter: TexMagFilter,
    mipmaps: Boolean,
): BitmapFont {
    val kernings = mutableListOf<Kerning>()
    val glyphs = mutableListOf<BitmapFont.Glyph>()
    var lineHeight = 16f
    var fontSize = 16f
    var base: Float? = null
    var pages = 1
    val lines = data.split("\n")
    val padding = MutableVec4i(0)

    val capChars = charArrayOf(
        'M', 'N', 'B', 'D', 'C', 'E', 'F', 'K', 'A', 'G', 'H', 'I', 'J', 'L', 'O', 'P', 'Q', 'R', 'S',
        'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    )
    var capHeightFound = false
    var capHeight = 1

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
                map["padding"]?.let {
                    val nums = it.split(',')
                    padding.set(
                        nums[0].toIntOrNull() ?: 0,
                        nums[1].toIntOrNull() ?: 0,
                        nums[2].toIntOrNull() ?: 0,
                        nums[3].toIntOrNull() ?: 0,
                    )
                }
            }

            line.startsWith("page") -> {
                val id = map["id"]?.toInt() ?: 0
                val file = map["file"]?.unquote() ?: error("Page without file")
                if (loadTextures) {
                    textures[id] = fontFile.parent[file].readTexture(magFilter = filter, mipmaps = mipmaps)
                }
            }

            line.startsWith("common ") -> {
                lineHeight = map["lineHeight"]?.toFloatOrNull() ?: 16f
                base = map["base"]?.toFloatOrNull()
                pages = map["pages"]?.toIntOrNull() ?: 1
            }

            line.startsWith("char ") -> {
                val page = map["page"]?.toIntOrNull() ?: 0
                val id = map["id"]?.toIntOrNull() ?: 0
                val width = map["width"]?.toIntOrNull() ?: 0
                val height = map["height"]?.toIntOrNull() ?: 0

                if (!capHeightFound) {
                    if (capChars.contains(id.toChar())) {
                        capHeight = height
                        capHeightFound = true
                    } else if (width != 0 && height != 0) {
                        capHeight = max(capHeight, height)
                    }
                }
                val slice = when {
                    loadTextures -> {
                        TextureSlice(
                            textures[page] ?: textures.values.first(),
                            map["x"]?.toIntOrNull() ?: 0,
                            map["y"]?.toIntOrNull() ?: 0,
                            width,
                            height
                        )
                    }

                    preloadedTextures.isNotEmpty() -> {
                        TextureSlice(
                            preloadedTextures[page],
                            map["x"]?.toIntOrNull() ?: 0,
                            map["y"]?.toIntOrNull() ?: 0,
                            width,
                            height
                        )
                    }

                    else -> {
                        throw IllegalStateException("Unable to load any textures for ${fontFile.baseName}. If they are preloaded, make sure to pass that in 'readBitmapFont()'.")
                    }
                }
                glyphs += BitmapFont.Glyph(
                    fontSize = fontSize,
                    id = id,
                    slice = slice,
                    xoffset = map["xoffset"]?.toIntOrNull() ?: 0,
                    yoffset = map["yoffset"]?.toIntOrNull() ?: 0,
                    xadvance = map["xadvance"]?.toIntOrNull() ?: 0,
                    width = width,
                    height = height,
                    page = page
                )
            }

            line.startsWith("kerning ") -> {
                kernings += Kerning(
                    first = map["first"]?.toIntOrNull() ?: 0,
                    second = map["second"]?.toIntOrNull() ?: 0,
                    amount = map["amount"]?.toIntOrNull() ?: 0
                )
            }
        }
    }

    capHeight -= padding.x + padding.z

    return BitmapFont(
        fontSize = fontSize,
        lineHeight = lineHeight,
        base = base ?: lineHeight,
        capHeight = capHeight.toFloat(),
        padding = FontMetrics.Padding(padding.x, padding.y, padding.z, padding.w),
        textures = textures.values.toList(),
        glyphs = glyphs.associateBy { it.id },
        kernings = kernings.associateBy { Kerning.buildKey(it.first, it.second) },
        pages = pages
    )
}

/**
 * Reads the [VfsFile] as a [LDtkMapLoader]. This will read the LDtk file and create a loader to allow flexible loading
 * of [LDtkWorld] or [LDtkLevel]. This loader should be cached and reused when loading separate levels.
 * @param atlas an atlas the has the preloaded textures for both tilesets and image layers. **Note**: that if the
 * [Texture] for a tileset has a border thickness set, that value must be used for [tilesetBorder]. If no border is set,
 * then [tilesetBorder] must be marked as `0`.
 * @param tilesetBorder the border thickness of each slice when loading the tileset to prevent bleeding. This is used when
 * slicing tileset textures from an atlas or when loading externally.
 * @return the loaded LDtk map
 */
suspend fun VfsFile.readLDtkMapLoader(atlas: TextureAtlas? = null, tilesetBorder: Int = 2): LDtkMapLoader {
    val mapData = decodeFromString<LDtkMapData>()
    return LDtkMapLoader(this, mapData, atlas, tilesetBorder)
}

/**
 * Reads the [VfsFile] as a [TiledMap]. Any loaders and assets will be cached for reuse/reloading.
 * @param atlas an atlas the has the preloaded textures for both tilesets and image layers. **Note**: that if the
 * [Texture] for a tileset has a border thickness set, that value must be used for [tilesetBorder]. If no border is set,
 * then [tilesetBorder] must be marked as `0`.
 * @param tilesetBorder the border thickness of each slice when loading the tileset to prevent bleeding. This is used when
 * slicing tileset textures from an atlas or when loading externally.
 * @return the loaded Tiled map
 */
suspend fun VfsFile.readTiledMap(
    atlas: TextureAtlas? = null,
    tilesetBorder: Int = 2,
    mipmaps: Boolean = true,
): TiledMap {
    val mapData = decodeFromString<TiledMapData>()
    val loader = TiledMapLoader(parent, mapData, atlas, tilesetBorder, mipmaps)
    return loader.loadMap()
}

/**
 * Loads an image from the path as a [Texture]. This will call [Texture.prepare] before returning!
 * @return the loaded texture
 */
expect suspend fun VfsFile.readTexture(
    minFilter: TexMinFilter = TexMinFilter.NEAREST,
    magFilter: TexMagFilter = TexMagFilter.NEAREST,
    mipmaps: Boolean = true,
): Texture

/**
 * Reads Base64 encoded ByteArray for embedded images.
 */
internal expect suspend fun ByteArray.readPixmap(): Pixmap

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
 * Streams audio from the path as an [AudioStream].
 * @return a new [AudioStream]
 */
expect suspend fun VfsFile.readAudioStream(): AudioStream

/**
 * Write pixmap to disk.
 */
expect suspend fun VfsFile.writePixmap(pixmap: Pixmap)

/**
 * Loads a glTF / glb model from the path and converts it to a [Model].
 * @return a new [Model]
 */
suspend fun VfsFile.readGltfModel(): Model {
    val file: GltfFile = when {
        isGltf() -> loadGltf()
        isBinaryGltf() -> loadBinaryGltf()
        else -> throw IllegalArgumentException("Unknown glTF type: $path")
    }
    file.buffers.filter { it.uri != null }.forEach {
        val uri = it.uri!!
        val bufferPath = if (uri.startsWith("data:", true)) VfsFile(vfs, uri) else VfsFile(vfs, "$path/$uri")
        it.data = bufferPath.read()
    }
    //  file.images.filter { it.uri != null }.forEach { it.uri = "" }
    file.updateReferences()

    return file.toModel(vfs.context, vfs.context.gl)
}

private fun VfsFile.isGltf() = path.endsWith(".gltf", true) || path.endsWith(".gltf.gz", true)
private fun VfsFile.isBinaryGltf() = path.endsWith(".glb", true) || path.endsWith(".glb.gz", true)

private suspend fun VfsFile.loadGltf(): GltfFile {
    if (path.endsWith(".gz", true)) {
        TODO("Implement gzip inflation")
    }
    return decodeFromString()
}

private suspend fun VfsFile.loadBinaryGltf(): GltfFile {
    if (path.endsWith(".gz", true)) {
        TODO("Implement gzip inflation")
    }
    val stream = readStream()

    val magic = stream.readUInt()
    val version = stream.readUInt()
    val length = stream.readUInt()
    if (magic != GltfFile.GLB_FILE_MAGIC) {
        throw IllegalStateException("Unexpected glTF magic number: $magic. Expected: ${GltfFile.GLB_FILE_MAGIC} / 'glTF'.")
    }

    if (version != 2) {
        vfs.logger.warn { "Unexpected glTF version: $version. Expected: version 2." }
    }

    var chunkLength = stream.readUInt()
    var chunkType = stream.readUInt()
    if (chunkType != GltfFile.GLB_CHUNK_MAGIC_JSON) {
        throw IllegalStateException("Unexpected chunk type for chunk 0: $chunkType. Expected: ${GltfFile.GLB_CHUNK_MAGIC_JSON} / 'JSON'.")
    }

    val jsonData = stream.readChunk(chunkLength)

    val gltf = vfs.json.decodeFromString<GltfFile>(jsonData.decodeToString())

    var chunk = 1
    while (stream.hasRemaining()) {
        chunkLength = stream.readUInt()
        chunkType = stream.readUInt()
        if (chunkType != GltfFile.GLB_CHUNK_MAGIC_BIN) {
            vfs.logger.warn { "Unexpected chunk type for chunk $chunk: $chunkType. Expected: ${GltfFile.GLB_CHUNK_MAGIC_BIN} / 'BIN'." }
            stream.readChunk(chunkLength)
        } else {
            gltf.buffers[chunk - 1].data = createByteBuffer(stream.readChunk(chunkLength))
        }
        chunk++
    }

    vfs.logger.info { "Fully loaded glTF $path (${(length / 1024.0 / 1024.0).toString(2)} mb)" }
    return gltf
}
