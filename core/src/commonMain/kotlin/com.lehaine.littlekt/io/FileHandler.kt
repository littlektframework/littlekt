package com.lehaine.littlekt.io

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureData
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

    open fun close() {
        job.cancel()
    }

    private suspend fun loadAsset(ref: AssetRef): LoadedAsset {
        return when (ref) {
            is RawAssetRef -> loadRaw(ref)
            is TextureAssetRef -> loadTexture(ref)
        }
    }


    protected abstract suspend fun loadRaw(rawRef: RawAssetRef): LoadedRawAsset

    protected abstract suspend fun loadTexture(textureRef: TextureAssetRef): LoadedTextureAsset

    abstract suspend fun loadAudioClip(assetPath: String): AudioClip

    protected open fun isHttpAsset(assetPath: String): Boolean =
        // maybe use something less naive here?
        assetPath.startsWith("http://", true) ||
                assetPath.startsWith("https://", true) ||
                assetPath.startsWith("data:", true)

    fun launch(block: suspend FileHandler.() -> Unit) {
        (this as CoroutineScope).launch {
            block.invoke(this@FileHandler)
        }
    }

    suspend fun loadAsset(assetPath: String): Uint8Buffer? {
        val ref = if (isHttpAsset(assetPath)) {
            RawAssetRef(assetPath, false)
        } else {
            RawAssetRef("$assetsBaseDir/$assetPath", true)
        }
        val awaitedAsset = AwaitedAsset(ref)
        awaitedAssetsChannel.send(awaitedAsset)
        val loaded = awaitedAsset.awaiting.await() as LoadedRawAsset
        loaded.data?.let {
            logger.debug { "Loaded ${assetPathToName(assetPath)} (${(it.capacity / 1024.0 / 1024.0).toString(1)} mb)" }
        }
        return loaded.data
    }

    suspend fun loadTextureData(assetPath: String, format: TextureFormat? = TextureFormat.RGBA): TextureData {
        val ref = if (isHttpAsset(assetPath)) {
            TextureAssetRef(assetPath, false, format, false)
        } else {
            TextureAssetRef("$assetsBaseDir/$assetPath", true, format, false)
        }
        val awaitedAsset = AwaitedAsset(ref)
        awaitedAssetsChannel.send(awaitedAsset)
        val loaded = awaitedAsset.awaiting.await() as LoadedTextureAsset
        loaded.data?.let {
            logger.debug { "Loaded ${assetPathToName(assetPath)} (${it.format}, ${it.width}x${it.height})" }
        }
        return loaded.data ?: throw RuntimeException("Failed loading texture")
    }

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
    val isAtlas: Boolean,
    val tilesX: Int = 1,
    val tilesY: Int = 1
) : AssetRef()

sealed class LoadedAsset(val ref: AssetRef, val successfull: Boolean)
class LoadedRawAsset(ref: AssetRef, val data: Uint8Buffer?) : LoadedAsset(ref, data != null)
class LoadedTextureAsset(ref: AssetRef, val data: TextureData?) : LoadedAsset(ref, data != null)