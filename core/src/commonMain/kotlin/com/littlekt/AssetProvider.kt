package com.littlekt

import com.littlekt.async.KtScope
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
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.internal.lock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Provides helper functions to load and prepare assets without having to use `null` or `lateinit`.
 */
open class AssetProvider(val context: Context) {
    private val assetsToPrepare = arrayListOf<PreparableGameAsset<*>>()
    private var totalAssetsLoading = atomic(0)
    private var totalAssets = atomic(0)
    private var totalAssetsFinished = atomic(0)
    private val filesBeingChecked = mutableListOf<VfsFile>()
    private val _assets = mutableMapOf<KClass<*>, MutableMap<VfsFile, GameAsset<*>>>()
    private val lock = Any()

    /** The percentage of the total assets finished loading, using a value between `0f` and `1f`. */
    val percentage: Float
        get() =
            if (totalAssets.value == 0) 1f
            else totalAssetsFinished.value / totalAssets.value.toFloat()

    /** A map of loaders. Custom loaders may be added directly to this. */
    val loaders: MutableMap<KClass<*>, suspend (VfsFile, GameAssetParameters) -> Any> =
        createDefaultLoaders().toMutableMap()

    /** A map of loaded assets. */
    val assets: Map<KClass<*>, Map<VfsFile, GameAsset<*>>>
        get() = _assets

    /** A lambda that is invoked once all assets have been loaded and prepared. */
    var onFullyLoaded: (() -> Unit)? = null

    /** Determines if it has been fully loaded. */
    val fullyLoaded: Boolean
        get() = totalAssetsLoading.value == 0 && assetsToPrepare.isEmpty()

    private var job: Job? = null

    /** Updates to check if all assets have been loaded, and if so, prepare them. */
    fun update() {
        if (totalAssetsLoading.value > 0) return
        if (job?.isActive != true && assetsToPrepare.isNotEmpty()) {
            val prepare = assetsToPrepare.toList()
            assetsToPrepare.clear()
            job =
                KtScope.launch {
                    prepare.fastForEach { it.prepare() }
                    onFullyLoaded?.invoke()
                }
        }
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
    fun <T : Any> load(
        file: VfsFile,
        clazz: KClass<T>,
        parameters: GameAssetParameters = EmptyGameAssetParameter(),
    ): GameAsset<T> {
        val sceneAsset = checkOrCreateNewSceneAsset(file, clazz)
        file.vfs.launch { loadVfsFile(sceneAsset, file, clazz, parameters) }
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

    /**
     * Prepares a value once assets have finished loading. This acts the same as [lazy] except this
     * will invoke the [action] once loading is finished to ensure everything is initialized before
     * the first frame.
     *
     * @param action the action to initialize this value
     * @see load
     */
    @OptIn(ExperimentalContracts::class)
    fun <T : Any> prepare(action: suspend () -> T): PreparableGameAsset<T> {
        contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
        return PreparableGameAsset(action).also { assetsToPrepare += it }
    }

    /** @return the cached content of the specified [vfsFile]. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: KClass<T>, vfsFile: VfsFile) =
        assets[clazz]?.get(vfsFile)?.content as T

    /** @return the cached content of the specified [file]. */
    inline fun <reified T : Any> get(
        file: VfsFile,
    ) = get(T::class, file)

    companion object {
        /** Creates a map of the default asset loaders. */
        fun createDefaultLoaders() =
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

/** An asset that wraps a [VfsFile] that caches the loaded content. */
class GameAsset<T>(val vfsFile: VfsFile) {
    private var result: T? = null
    private var isLoaded = false

    /**
     * The cached content.
     *
     * @throws IllegalStateException if asset hasn't been loaded yet.
     */
    val content: T & Any
        get() =
            if (isLoaded) result!!
            else throw IllegalStateException("Asset not loaded yet! ${vfsFile.path}")

    /** @return [content] */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = content

    /**
     * Load this asset manually by setting the [content] directly. This marks the asset as loaded.
     */
    fun load(content: T) {
        result = content
        isLoaded = true
    }
}

/**
 * An asset that may be "prepared" by invoking the specified [action] which in turn returns the
 * content required. This is mainly used when calling [AssetProvider.load] on a [VfsFile] and
 * needing a specific piece of content from the result.
 */
class PreparableGameAsset<T>(val action: suspend () -> T) {
    private var isPrepared = false
    private var result: T? = null

    /**
     * @return the [action] result, if [prepare] has been called, otherwise throws an error.
     * @throws IllegalStateException if asset hasn't been prepared.
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (isPrepared) {
            return result!!
        } else {
            throw IllegalStateException("Asset not prepared yet!")
        }
    }

    /** Invokes [action] and is marked as prepared. */
    suspend fun prepare() {
        result = action.invoke()
        isPrepared = true
    }
}

/** Specific parameters for loading game assets. */
interface GameAssetParameters

/** An empty game asset parameter. */
class EmptyGameAssetParameter : GameAssetParameters

/** Parameters related to loading a [Texture]. */
class TextureGameAssetParameter : GameAssetParameters

/** Parameters related to loading a [LDtkMapLoader]. */
class LDtkGameAssetParameter(
    val atlas: TextureAtlas? = null,
    val tilesetBorderThickness: Int = 2,
) : GameAssetParameters

/**
 * Parameters related to loading a [TtfFont].
 *
 * @param chars chars to load a glyph for.
 * @see CharacterSets
 */
class TtfFileAssetParameter(
    val chars: String = CharacterSets.LATIN_ALL,
) : GameAssetParameters

/**
 * Parameters related to loading a [BitmapFont].
 *
 * @param preloadedTextures
 */
class BitmapFontAssetParameter(
    val preloadedTextures: List<TextureSlice> = listOf(),
) : GameAssetParameters
