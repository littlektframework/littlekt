package com.lehaine.littlekt

import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.file.UnsupportedFileTypeException
import com.lehaine.littlekt.file.vfs.*
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureAtlas
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.time.Duration

/**
 * A [ContextListener] that handles commonly loading assets and preparing them before a scene begins.
 * @author Colt Daily
 * @date 12/23/21
 */
open class ContextScene(context: Context) : ContextListener(context) {
    private val loaders = createLoaders()
    private val assetsToPrepare = mutableListOf<PreparableSceneAsset<*>>()
    var loading = true
    var prepared = false
    private var totalAssetsLoading = 0

    init {
        context.vfs.launch {
            loadAssets()
            loading = false
        }
    }

    /**
     * This is triggered before anything else runs. Runs in a separate thread. Load any assets here.
     * If an asset needs to be prepared, then prepare it in the [prepare] function.
     */
    open suspend fun loadAssets() {}

    /**
     * This is triggered after all assets have been loaded. Runs on the ui thread. Prepare any asset here.
     */
    open fun prepare() {}

    /**
     * Override [update] instead!! Render will call update when the scene has finished loading and preparing.
     */
    final override fun render(dt: Duration) {
        if (loading || totalAssetsLoading > 0) return
        if (!prepared) {
            assetsToPrepare.forEach {
                it.prepare()
            }
            assetsToPrepare.clear()
            prepare()
            prepared = true
        }

        update(dt)
    }

    open fun update(dt: Duration) {}

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> load(
        file: VfsFile,
        clazz: KClass<T>,
        parameters: SceneAssetParameters = SceneAssetParameters()
    ): SceneAsset<T> {
        val sceneAsset = SceneAsset<T>(file)
        totalAssetsLoading++
        context.vfs.launch {
            val loader = loaders[clazz] ?: throw UnsupportedFileTypeException(file.path)
            val result = loader.invoke(file, parameters) as T
            sceneAsset.load(result)
            totalAssetsLoading--
        }
        return sceneAsset
    }

    /**
     * Loads an asset.
     * @param file the file to load
     * @param parameters any parameters that need setting when loading the asset
     * @see LDtkSceneAssetParameter
     */
    inline fun <reified T : Any> load(
        file: VfsFile,
        parameters: SceneAssetParameters = SceneAssetParameters()
    ) = load(file, T::class, parameters)


    /**
     * Prepares a value once assets have finished loading. This acts the same as [lazy] except this will
     * invoke the [action] once loading is finished to ensure everything is initialized before the first frame.
     * @param action the action to initialize this value
     */
    fun <T : Any> prepare(action: () -> T) = PreparableSceneAsset(action).also { assetsToPrepare += it }

    companion object {
        private fun createLoaders() = mapOf<KClass<*>, suspend (VfsFile, SceneAssetParameters) -> Any>(
            Texture::class to { file, params ->
                file.readTexture()
            },
            AudioClip::class to { file, params ->
                file.readAudioClip()
            },
            TextureAtlas::class to { file, params ->
                file.readAtlas()
            },
            LDtkTileMap::class to { file, params ->
                if (params is LDtkSceneAssetParameter) {
                    file.readLDtkMap(params.loadAllLevels, params.levelIdx)
                } else {
                    file.readLDtkMap()
                }
            }
        )
    }
}

class SceneAsset<T>(val vfsFile: VfsFile) {
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

class PreparableSceneAsset<T>(val action: () -> T) {
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

open class SceneAssetParameters

class LDtkSceneAssetParameter(val loadAllLevels: Boolean = true, val levelIdx: Int = 0) : SceneAssetParameters()