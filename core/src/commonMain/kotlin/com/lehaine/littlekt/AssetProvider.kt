package com.lehaine.littlekt

import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.audio.AudioStream
import com.lehaine.littlekt.file.UnsupportedFileTypeException
import com.lehaine.littlekt.file.ldtk.LDtkMapLoader
import com.lehaine.littlekt.file.vfs.*
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
import com.lehaine.littlekt.util.internal.lock
import kotlinx.atomicfu.atomic
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Provides helper functions to load and prepare assets without having to use `null` or `lateinit`.
 */
open class AssetProvider(val context: Context) {
    private val assetsToPrepare = mutableListOf<PreparableGameAsset<*>>()
    private var totalAssetsLoading = atomic(0)
    private var totalAssets = atomic(0)
    private var totalAssetsFinished = atomic(0)
    private val filesBeingChecked = mutableListOf<VfsFile>()
    private val _assets = mutableMapOf<KClass<*>, MutableMap<VfsFile, GameAsset<*>>>()
    private val lock = Any()

    val percentage: Float get() = if (totalAssets.value == 0) 1f else totalAssetsFinished.value / totalAssets.value.toFloat()
    val loaders = createDefaultLoaders().toMutableMap()
    val assets: Map<KClass<*>, Map<VfsFile, GameAsset<*>>> get() = _assets
    var onFullyLoaded: (() -> Unit)? = null


    /**
     * Holds the current state of assets being prepared.
     * @see prepare
     */
    var prepared = false
        protected set

    /**
     * Calls [update] to get the latest assets loaded to determine if is has been fully loaded.
     */
    val fullyLoaded: Boolean
        get() = totalAssetsLoading.value == 0 && prepared


    /**
     * Updates to check if all assets have been loaded, and if so, prepare them.
     */
   suspend fun update() {
        if (totalAssetsLoading.value > 0) return
        if (!prepared) {
            assetsToPrepare.forEach {
                it.prepare()
            }
            assetsToPrepare.clear()
            prepared = true
            onFullyLoaded?.invoke()
        }
    }

    /**
     * Loads an asset asynchronously.
     * @param T concrete class of [Any] instance that should be loaded.
     * @param file the file to load
     * @param parameters any parameters that need setting when loading the asset
     * @see LDtkGameAssetParameter
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> load(
        file: VfsFile,
        clazz: KClass<T>,
        parameters: GameAssetParameters = EmptyGameAssetParameter()
    ): GameAsset<T> {
        val sceneAsset = _assets[clazz]?.get(file)?.let {
            return it as GameAsset<T>
        } ?: GameAsset<T>(file)

        if (filesBeingChecked.contains(file)) {
            throw IllegalStateException("'${file.path}' has already been triggered to load but hasn't finished yet! Ensure `load()` hasn't been called twice for the same VfsFile")
        }
        prepared = false
        filesBeingChecked += file
        totalAssetsLoading.addAndGet(1)
        totalAssets.addAndGet(1)
        context.vfs.launch {
            val loader = loaders[clazz] ?: throw UnsupportedFileTypeException(file.path)
            val result = loader.invoke(file, parameters) as T
            sceneAsset.load(result)
            lock(lock) {
                _assets.getOrPut(clazz) { mutableMapOf() }.let {
                    it[file] = sceneAsset
                }
                filesBeingChecked -= file
            }
            totalAssetsFinished.addAndGet(1)
            totalAssetsLoading.addAndGet(-1)
        }
        return sceneAsset
    }

    /**
     * Loads an asset asynchronously.
     * @param T concrete class of [Any] instance that should be loaded.
     * @param file the file to load
     * @param parameters any parameters that need setting when loading the asset
     * @see BitmapFontAssetParameter
     * @see LDtkGameAssetParameter
     */
    inline fun <reified T : Any> load(
        file: VfsFile,
        parameters: GameAssetParameters = EmptyGameAssetParameter()
    ) = load(file, T::class, parameters)


    /**
     * Prepares a value once assets have finished loading. This acts the same as [lazy] except this will
     * invoke the [action] once loading is finished to ensure everything is initialized before the first frame.
     * @param action the action to initialize this value
     * @see load
     */
    fun <T : Any> prepare(action: suspend () -> T) = PreparableGameAsset(action).also { assetsToPrepare += it }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: KClass<T>, vfsFile: VfsFile) = assets[clazz]?.get(vfsFile)?.content as T

    inline fun <reified T : Any> get(
        file: VfsFile,
    ) = get(T::class, file) as T

    companion object {
        fun createDefaultLoaders() = mapOf<KClass<*>, suspend (VfsFile, GameAssetParameters) -> Any>(
            Texture::class to { file, param ->
                if (param is TextureGameAssetParameter) {
                    file.readTexture(param.minFilter, param.magFilter, param.useMipmaps)
                } else {
                    file.readTexture()
                }
            },
            Pixmap::class to { file, _ ->
                file.readPixmap()
            },
            AudioClip::class to { file, _ ->
                file.readAudioClip()
            },
            AudioStream::class to { file, _ ->
                file.readAudioStream()
            },
            TextureAtlas::class to { file, _ ->
                file.readAtlas()
            },
            TtfFont::class to { file, params ->
                if (params is TtfFileAssetParameter) {
                    file.readTtfFont(params.chars)
                } else {
                    file.readTtfFont()
                }
            },
            BitmapFont::class to { file, params ->
                if (params is BitmapFontAssetParameter) {
                    file.readBitmapFont(params.magFilter, params.mipmaps, params.preloadedTextures)
                } else {
                    file.readBitmapFont()
                }
            },
            LDtkMapLoader::class to { file, params ->
                if (params is LDtkGameAssetParameter) {
                    file.readLDtkMapLoader(params.tilesetBorderThickness)
                } else {
                    file.readLDtkMapLoader()
                }
            },
            LDtkLevel::class to { file, params ->
                if (params is LDtkGameAssetParameter) {
                    file.readLDtkLevel(params.levelIdx, params.tilesetBorderThickness)
                } else {
                    file.readLDtkLevel(0)
                }
            }
        )
    }
}

class GameAsset<T>(val vfsFile: VfsFile) {
    private var result: T? = null
    private var isLoaded = false
    val content get() = if (isLoaded) result!! else throw IllegalStateException("Asset not loaded yet! ${vfsFile.path}")

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = content

    fun load(content: T) {
        result = content
        isLoaded = true
    }
}

class PreparableGameAsset<T>(val action: suspend () -> T) {
    private var isPrepared = false
    private var result: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (isPrepared) {
            return result!!
        } else {
            throw IllegalStateException("Asset not prepared yet!")
        }
    }

    suspend fun prepare() {
        result = action.invoke()
        isPrepared = true
    }
}

interface GameAssetParameters

class EmptyGameAssetParameter : GameAssetParameters

class TextureGameAssetParameter(
    val minFilter: TexMinFilter = TexMinFilter.NEAREST,
    val magFilter: TexMagFilter = TexMagFilter.NEAREST,
    val useMipmaps: Boolean = true
) : GameAssetParameters

class LDtkGameAssetParameter(
    val levelIdx: Int = 0,
    val tilesetBorderThickness: Int = 2
) : GameAssetParameters

class TtfFileAssetParameter(
    /**
     * The chars to load a glyph for.
     * @see CharacterSets
     */
    val chars: String = CharacterSets.LATIN_ALL
) : GameAssetParameters

class BitmapFontAssetParameter(
    val magFilter: TexMagFilter = TexMagFilter.NEAREST,
    /**
     * Use mipmaps on the bitmap textures or not.
     */
    val mipmaps: Boolean = true,
    val preloadedTextures: List<TextureSlice> = listOf(),
) : GameAssetParameters