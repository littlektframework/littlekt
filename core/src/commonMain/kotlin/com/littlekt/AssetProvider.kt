package com.littlekt

import com.littlekt.audio.AudioClip
import com.littlekt.audio.AudioStream
import com.littlekt.file.UnsupportedFileTypeException
import com.littlekt.file.ldtk.LDtkMapLoader
import com.littlekt.file.vfs.*
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.TextureAtlas
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.font.BitmapFont
import com.littlekt.graphics.g2d.font.CharacterSets
import com.littlekt.graphics.g2d.font.TtfFont
import com.littlekt.graphics.g2d.tilemap.tiled.TiledMap
import com.littlekt.graphics.webgpu.Device
import com.littlekt.util.internal.lock
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlinx.atomicfu.atomic

/**
 * Provides helper functions to load and prepare assets without having to use `null` or `lateinit`.
 */
open class AssetProvider(val context: Context) {
    private val assetsToPrepare = arrayListOf<GameAsset<*>>()
    private var totalAssetsLoading = atomic(0)
    private var totalAssets = atomic(0)
    private var totalAssetsFinished = atomic(0)
    private val filesBeingChecked = mutableListOf<VfsFile>()
    private val _assets = mutableMapOf<KClass<*>, MutableMap<VfsFile, GameAsset<*>>>()
    private val lock = Any()

    val percentage: Float
        get() =
            if (totalAssets.value == 0) 1f
            else totalAssetsFinished.value / totalAssets.value.toFloat()

    val loaders = createDefaultLoaders(context.graphics.device).toMutableMap()
    val assets: Map<KClass<*>, Map<VfsFile, GameAsset<*>>>
        get() = _assets

    var onFullyLoaded: (() -> Unit)? = null

    /** Determines if it has been fully loaded. */
    val fullyLoaded: Boolean
        get() = totalAssetsLoading.value == 0

    /**
     * Loads an asset asynchronously.
     *
     * @param T concrete class of [Any] instance that should be loaded.
     * @param file the file to load
     * @param parameters any parameters that need setting when loading the asset
     * @see BitmapFontAssetParameter
     * @see LDtkGameAssetParameter
     * @see TextureGameAssetParameter
     * @see TtfFileAssetParameter
     */
    fun <T : Any> load(
        file: VfsFile,
        clazz: KClass<T>,
        parameters: GameAssetParameters = EmptyGameAssetParameter(),
    ): GameAsset<T> {
        val sceneAsset = checkOrCreateNewSceneAsset(file, clazz)
        context.vfs.launch { loadVfsFile(sceneAsset, file, clazz, parameters) }
        return sceneAsset
    }

    /**
     * Loads an asset in a suspending function.
     *
     * @param T concrete class of [Any] instance that should be loaded.
     * @param file the file to load
     * @param parameters any parameters that need setting when loading the asset
     * @see BitmapFontAssetParameter
     * @see LDtkGameAssetParameter
     * @see TextureGameAssetParameter
     * @see TtfFileAssetParameter
     */
    suspend fun <T : Any> loadSuspending(
        file: VfsFile,
        clazz: KClass<T>,
        parameters: GameAssetParameters = EmptyGameAssetParameter(),
    ): GameAsset<T> {
        val sceneAsset = checkOrCreateNewSceneAsset(file, clazz)
        loadVfsFile(sceneAsset, file, clazz, parameters)
        return sceneAsset
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> checkOrCreateNewSceneAsset(
        file: VfsFile,
        clazz: KClass<T>,
    ): GameAsset<T> {
        val sceneAsset =
            _assets[clazz]?.get(file)?.let {
                return it as GameAsset<T>
            } ?: GameAsset<T>(file)

        if (filesBeingChecked.contains(file)) {
            throw IllegalStateException(
                "'${file.path}' has already been triggered to load but hasn't finished yet! Ensure `load()` hasn't been called twice for the same VfsFile"
            )
        }
        filesBeingChecked += file
        totalAssetsLoading.addAndGet(1)
        totalAssets.addAndGet(1)
        return sceneAsset
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> loadVfsFile(
        sceneAsset: GameAsset<T>,
        file: VfsFile,
        clazz: KClass<T>,
        parameters: GameAssetParameters = EmptyGameAssetParameter(),
    ) {
        val loader = loaders[clazz] ?: throw UnsupportedFileTypeException(file.path)
        val result = loader.invoke(file, parameters) as T
        sceneAsset.load(result)
        lock(lock) {
            _assets.getOrPut(clazz) { mutableMapOf() }.let { it[file] = sceneAsset }
            filesBeingChecked -= file
        }
        totalAssetsFinished.addAndGet(1)
        totalAssetsLoading.addAndGet(-1)
    }

    /**
     * Loads an asset asynchronously.
     *
     * @param T concrete class of [Any] instance that should be loaded.
     * @param file the file to load
     * @param parameters any parameters that need setting when loading the asset
     * @see BitmapFontAssetParameter
     * @see LDtkGameAssetParameter
     * @see TextureGameAssetParameter
     * @see TtfFileAssetParameter
     */
    inline fun <reified T : Any> load(
        file: VfsFile,
        parameters: GameAssetParameters = EmptyGameAssetParameter(),
    ) = load(file, T::class, parameters)

    /**
     * Loads an asset in a suspending function.
     *
     * @param T concrete class of [Any] instance that should be loaded.
     * @param file the file to load
     * @param parameters any parameters that need setting when loading the asset
     * @see BitmapFontAssetParameter
     * @see LDtkGameAssetParameter
     * @see TextureGameAssetParameter
     * @see TtfFileAssetParameter
     */
    suspend inline fun <reified T : Any> loadSuspending(
        file: VfsFile,
        parameters: GameAssetParameters = EmptyGameAssetParameter(),
    ) = loadSuspending(file, T::class, parameters)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: KClass<T>, vfsFile: VfsFile) =
        assets[clazz]?.get(vfsFile)?.content as T

    inline fun <reified T : Any> get(
        file: VfsFile,
    ) = get(T::class, file)

    companion object {
        fun createDefaultLoaders(device: Device) =
            mapOf<KClass<*>, suspend (VfsFile, GameAssetParameters) -> Any>(
                Texture::class to
                    { file, param ->
                        if (param is TextureGameAssetParameter) {
                            file.readTexture()
                        } else {
                            file.readTexture()
                        }
                    },
                Pixmap::class to { file, _ -> file.readPixmap() },
                AudioClip::class to { file, _ -> file.readAudioClip() },
                AudioStream::class to { file, _ -> file.readAudioStream() },
                TextureAtlas::class to { file, _ -> file.readAtlas() },
                TtfFont::class to
                    { file, params ->
                        if (params is TtfFileAssetParameter) {
                            file.readTtfFont(params.chars)
                        } else {
                            file.readTtfFont()
                        }
                    },
                BitmapFont::class to
                    { file, params ->
                        if (params is BitmapFontAssetParameter) {
                            file.readBitmapFont(params.preloadedTextures)
                        } else {
                            file.readBitmapFont()
                        }
                    },
                LDtkMapLoader::class to
                    { file, params ->
                        if (params is LDtkGameAssetParameter) {
                            file.readLDtkMapLoader(params.atlas, params.tilesetBorderThickness)
                        } else {
                            file.readLDtkMapLoader()
                        }
                    },
                TiledMap::class to { file, _ -> file.readTiledMap() }
            )
    }
}

class GameAsset<T>(val vfsFile: VfsFile) {
    private var result: T? = null
    private var isLoaded = false
    val content
        get() =
            if (isLoaded) result!!
            else throw IllegalStateException("Asset not loaded yet! ${vfsFile.path}")

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = content

    fun load(content: T) {
        result = content
        isLoaded = true
    }
}

interface GameAssetParameters

class EmptyGameAssetParameter : GameAssetParameters

class TextureGameAssetParameter() : GameAssetParameters

class LDtkGameAssetParameter(
    val atlas: TextureAtlas? = null,
    val tilesetBorderThickness: Int = 2,
) : GameAssetParameters

class TtfFileAssetParameter(
    /**
     * The chars to load a glyph for.
     *
     * @see CharacterSets
     */
    val chars: String = CharacterSets.LATIN_ALL,
) : GameAssetParameters

class BitmapFontAssetParameter(
    val preloadedTextures: List<TextureSlice> = listOf(),
) : GameAssetParameters
