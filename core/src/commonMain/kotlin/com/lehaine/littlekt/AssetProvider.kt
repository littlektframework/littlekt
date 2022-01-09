package com.lehaine.littlekt

import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.file.UnsupportedFileTypeException
import com.lehaine.littlekt.file.vfs.*
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureAtlas
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.font.CharacterSets
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkWorld
import kotlinx.atomicfu.atomic
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Provides helper functions to load and prepare assets without having to use `null` or `lateinit`.
 */
open class AssetProvider(val context: Context) {
    private val loaders = createLoaders()
    private val assetsToPrepare = mutableListOf<PreparableGameAsset<*>>()
    private var totalAssetsLoading = atomic(0)
    var onLoaded: (() -> Unit)? = null

    /**
     * Hold the current state of assets being loaded.
     * @see loadAssets
     * @see load
     */
    var loading = true
        protected set

    /**
     * Holds the current state of assets being prepared.
     * @see create
     * @see prepare
     */
    var prepared = false
        protected set

    val fullyLoaded get() = !loading && totalAssetsLoading.value == 0 && prepared

    init {
        context.vfs.launch {
            loadAssets()
            loading = false
        }
    }

    /**
     * This is triggered before anything else runs. Runs in a separate thread. Load any assets here.
     * If an asset needs to be prepared, then prepare it in the [create] function.
     */
    open suspend fun loadAssets() {}

    /**
     * This is triggered after all assets have been loaded. Runs on the ui thread. Initialize any GL related
     * objects here to ensure everything has been loaded.
     */
    open fun create() {}

    /**
     * Handle any render / update logic here.
     */
    fun update() {
        if (loading || totalAssetsLoading.value > 0) return
        if (!prepared) {
            assetsToPrepare.forEach {
                it.prepare()
            }
            assetsToPrepare.clear()
            create()
            prepared = true
            onFullyLoaded()
            onLoaded?.invoke()
        }
    }

    /**
     * Invoked when every asset has been loaded, prepared, and [create] triggered.
     */
    protected open fun onFullyLoaded() = Unit

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
        val sceneAsset = GameAsset<T>(file)
        totalAssetsLoading.addAndGet(1)
        context.vfs.launch {
            val loader = loaders[clazz] ?: throw UnsupportedFileTypeException(file.path)
            val result = loader.invoke(file, parameters) as T
            sceneAsset.load(result)
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
    fun <T : Any> prepare(action: () -> T) = PreparableGameAsset(action).also { assetsToPrepare += it }


    companion object {
        private fun createLoaders() = mapOf<KClass<*>, suspend (VfsFile, GameAssetParameters) -> Any>(
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
                    file.readBitmapFont(params.magFilter, params.mipmaps)
                } else {
                    file.readBitmapFont()
                }
            },
            LDtkWorld::class to { file, params ->
                if (params is LDtkGameAssetParameter) {
                    file.readLDtkMap(params.loadAllLevels, params.levelIdx, params.tilesetBorderThickness)
                } else {
                    file.readLDtkMap()
                }
            },
            LDtkLevel::class to { file, params ->
                if (params is LDtkGameAssetParameter) {
                    file.readLDtkLevel(params.levelIdx)
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

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (isLoaded) {
            return result!!
        } else {
            throw IllegalStateException("Asset not loaded yet! ${vfsFile.path}")
        }
    }

    fun load(content: T) {
        result = content
        isLoaded = true
    }
}

class PreparableGameAsset<T>(val action: () -> T) {
    private var isPrepared = false
    private var result: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (isPrepared) {
            return result!!
        } else {
            throw IllegalStateException("Asset not prepared yet!")
        }
    }

    fun prepare() {
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
    val loadAllLevels: Boolean = true,
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
    val mipmaps: Boolean = true
) : GameAssetParameters