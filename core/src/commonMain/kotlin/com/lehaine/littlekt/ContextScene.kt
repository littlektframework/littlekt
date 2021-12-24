package com.lehaine.littlekt

import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.file.UnsupportedFileTypeException
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readAudioClip
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureAtlas
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.time.Duration

/**
 * Created by Colt Daily 12/23/21
 */
open class ContextScene(context: Context) : ContextListener(context) {
    private val loaders = createLoaders()
    var loading = true
    var prepared = false
    private var totalAssetsLoading = 0

    init {
        context.vfs.launch {
//            loadAssets()
            loading = false
        }
    }

    /**
     * Runs in a separate thread. Load any assets here.
     * If an asset needs to be prepared, then prepare it in the [prepare] function.
     */
    open suspend fun loadAssets() {}

    /**
     * Runs on the ui thread. Prepare any asset here.
     */
    open fun prepare() {}

    /**
     * Override [update] instead!! Render will call update when the scene has finished loading and preparing.
     */
    final override fun render(dt: Duration) {
        if (loading || totalAssetsLoading > 0) return
        if (!prepared) {
            prepare()
            prepared = true
        }

        update(dt)
    }

    open fun update(dt: Duration) {}

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> load(file: VfsFile, clazz: KClass<T>): SceneAsset<T> {
        val sceneAsset = SceneAsset<T>(file)
        totalAssetsLoading++
        context.vfs.launch {
            val loader = loaders[clazz] ?: throw UnsupportedFileTypeException(file.path)
            val result = loader.invoke(file) as T
            sceneAsset.load(result)
            totalAssetsLoading--
        }
        return sceneAsset
    }

    inline fun <reified T : Any> load(file: VfsFile) = load(file, T::class)

    companion object {
        private fun createLoaders() = mapOf<KClass<*>, suspend (VfsFile) -> Any>(
            Texture::class to { it.readTexture() },
            AudioClip::class to { it.readAudioClip() },
            TextureAtlas::class to { it.readAtlas() },
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