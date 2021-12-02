package com.lehaine.littlekt.file

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.file.atlas.AtlasInfo
import com.lehaine.littlekt.file.atlas.AtlasPage
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureAtlas
import com.lehaine.littlekt.graphics.TextureData
import com.lehaine.littlekt.graphics.font.CharacterSets
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.graphics.gl.TextureFormat
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.internal.toString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

/**
 * @author Colton Daily
 * @date 11/6/2021
 */

abstract class FileHandler(
    val application: Application,
    val logger: Logger,
    var assetsBaseDir: String
) : CoroutineScope {

    protected val json = Json { ignoreUnknownKeys = true }

    protected val job = Job()

    override val coroutineContext: CoroutineContext = job

    private val awaitedAssetsChannel = Channel<AwaitedAsset>()
    private val assetRefChannel = Channel<AssetRef>(Channel.UNLIMITED)
    private val loadedAssetChannel = Channel<LoadedAsset>()

    private val workers = List(NUM_LOAD_WORKERS) { loadWorker(assetRefChannel, loadedAssetChannel) }

    private val loader = launch {
        val requested = mutableMapOf<AssetRef, MutableList<AwaitedAsset>>()
        while (true) {
            select<Unit> {
                awaitedAssetsChannel.onReceive { awaited ->
                    val awaiting = requested[awaited.ref]
                    if (awaiting == null) {
                        requested[awaited.ref] = mutableListOf(awaited)
                        assetRefChannel.send(awaited.ref)
                    } else {
                        awaiting.add(awaited)
                    }
                }
                loadedAssetChannel.onReceive { loaded ->
                    val awaiting = requested.remove(loaded.ref)!!
                    for (awaited in awaiting) {
                        awaited.awaiting.complete(loaded)
                    }
                }
            }
        }
    }

    private fun loadWorker(assetRefs: ReceiveChannel<AssetRef>, loadedAssets: SendChannel<LoadedAsset>) = launch {
        for (ref in assetRefs) {
            loadedAssets.send(loadAsset(ref))
        }
    }

    /**
     * Cancels this file handlers job.
     */
    open fun close() {
        job.cancel()
    }

    private suspend fun loadAsset(ref: AssetRef): LoadedAsset {
        return when (ref) {
            is RawAssetRef -> loadRawAsset(ref)
            is TextureAssetRef -> loadTextureAsset(ref)
        }
    }


    protected abstract suspend fun loadRawAsset(rawRef: RawAssetRef): LoadedRawAsset

    protected abstract suspend fun loadTextureAsset(textureRef: TextureAssetRef): LoadedTextureAsset

    abstract suspend fun loadAudioClip(assetPath: String): AudioClip

    protected open fun isHttpAsset(assetPath: String): Boolean =
        // maybe use something less naive here?
        assetPath.startsWith("http://", true) ||
                assetPath.startsWith("https://", true) ||
                assetPath.startsWith("data:", true)

    /**
     * Launches a new coroutine using the this file handlers coroutine context. Use this to load assets asynchronously.
     */
    fun launch(block: suspend FileHandler.() -> Unit) {
        (this as CoroutineScope).launch {
            block.invoke(this@FileHandler)
        }
    }

    fun launchBlocking(block: FileHandler.() -> Unit) {
        (this as CoroutineScope).launch {
            block.invoke(this@FileHandler)
        }
    }

    /**
     * Loads a raw file into a [Uint8Buffer]
     * @param assetPath the path to the file
     * @return the raw byte buffer
     */
    suspend fun loadAsset(assetPath: String): Uint8Buffer {
        val ref = if (isHttpAsset(assetPath)) {
            RawAssetRef(assetPath, false)
        } else {
            RawAssetRef("$assetsBaseDir/$assetPath", true)
        }
        val awaitedAsset = AwaitedAsset(ref)
        awaitedAssetsChannel.send(awaitedAsset)
        val loaded = awaitedAsset.awaiting.await() as LoadedRawAsset
        loaded.data?.let {
            logger.debug { "Loaded ${assetPathToName(assetPath)} (${(it.capacity / 1024.0 / 1024.0).toString(2)} mb)" }
        }
        return loaded.data ?: throw RuntimeException("Failed loading $assetPath")
    }

    /**
     * Loads a [TextureAtlas] from the given path. Currently supports only JSON atlas files.
     * @param assetPath the path to the atlas file to load
     * @return the texture atlas
     */
    suspend fun loadAtlas(assetPath: String): TextureAtlas {
        val data = loadAsset(assetPath).toArray().decodeToString()
        val info = when {
            data.startsWith("{") -> {
                val page = json.decodeFromString<AtlasPage>(data)
                AtlasInfo(page.meta, listOf(page))
            }
            data.startsWith('\n') -> TODO("Implement text atlas format")
            data.startsWith("\r\n") -> TODO("Implement text atlas format")
            else -> throw RuntimeException("Unsupported atlas format.")
        }

        val textures = info.pages.associate {
            it.meta.image to loadTexture(it.meta.image)
        }
        return TextureAtlas(textures, info)
    }

    suspend fun loadTtfFont(assetPath: String, chars: String = CharacterSets.LATIN_ALL): TtfFont {
        val data = loadAsset(assetPath)
        return TtfFont(chars).also { it.load(data) }
    }

    /**
     * Loads an image from the path as [TextureData] to be used in creating a [Texture].
     * @param assetPath the path to the image
     * @param format the texture format. Defaults to [TextureFormat.RGBA]
     * @return the loaded texture data
     */
    suspend fun loadTextureData(assetPath: String, format: TextureFormat? = TextureFormat.RGBA): TextureData {
        val ref = if (isHttpAsset(assetPath)) {
            TextureAssetRef(assetPath, false, format)
        } else {
            TextureAssetRef("$assetsBaseDir/$assetPath", true, format)
        }
        val awaitedAsset = AwaitedAsset(ref)
        awaitedAssetsChannel.send(awaitedAsset)
        val loaded = awaitedAsset.awaiting.await() as LoadedTextureAsset
        loaded.data?.let {
            logger.debug { "Loaded ${assetPathToName(assetPath)} (${it.format}, ${it.width}x${it.height})" }
        }
        return loaded.data ?: throw RuntimeException("Failed loading texture")
    }

    /**
     * Loads an image from the path as a [Texture]. This will call [Texture.prepare] before returning!
     * @return the loaded texture
     */
    abstract suspend fun loadTexture(assetPath: String): Texture

    fun assetPathToName(assetPath: String): String {
        return if (assetPath.startsWith("data:", true)) {
            val idx = assetPath.indexOf(';')
            assetPath.substring(0 until idx)
        } else {
            assetPath
        }
    }

    protected inner class AwaitedAsset(
        val ref: AssetRef,
        val awaiting: CompletableDeferred<LoadedAsset> = CompletableDeferred(job)
    )

    companion object {
        const val NUM_LOAD_WORKERS = 8
    }
}

sealed class AssetRef
data class RawAssetRef(val url: String, val isLocal: Boolean) : AssetRef()
data class TextureAssetRef(
    val url: String,
    val isLocal: Boolean,
    val fmt: TextureFormat?,
    val tilesX: Int = 1,
    val tilesY: Int = 1
) : AssetRef()

sealed class LoadedAsset(val ref: AssetRef, val successful: Boolean)
class LoadedRawAsset(ref: AssetRef, val data: Uint8Buffer?) : LoadedAsset(ref, data != null)
class LoadedTextureAsset(ref: AssetRef, val data: TextureData?) : LoadedAsset(ref, data != null)