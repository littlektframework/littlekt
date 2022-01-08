package com.lehaine.littlekt.file

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.file.vfs.lightCombine
import com.lehaine.littlekt.file.vfs.pathInfo
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.toString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.Json
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext

/**
 * @author Colton Daily
 * @date 11/6/2021
 */

abstract class Vfs(
    val context: Context,
    val logger: Logger,
    var baseDir: String
) : CoroutineScope {
    val root get() = VfsFile(this, baseDir)

    protected open val absolutePath: String get() = ""

    @PublishedApi
    internal val json = Json { ignoreUnknownKeys = true }

    protected val job = Job()

    override val coroutineContext: CoroutineContext = job

    private val awaitedAssetsChannel = Channel<AwaitedAsset>()
    private val assetRefChannel = Channel<AssetRef>(Channel.UNLIMITED)
    private val loadedAssetChannel = Channel<LoadedAsset>()

    init {
        repeat(NUM_LOAD_WORKERS) { loadWorker(assetRefChannel, loadedAssetChannel) }
        launch {
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
    }

    private fun loadWorker(assetRefs: ReceiveChannel<AssetRef>, loadedAssets: SendChannel<LoadedAsset>) = launch {
        for (ref in assetRefs) {
            loadedAssets.send(readBytes(ref))
        }
    }

    /**
     * Cancels this file handlers job.
     */
    open fun close() {
        job.cancel()
    }

    open fun getAbsolutePath(path: String) = absolutePath.pathInfo.lightCombine(path.pathInfo).fullPath

    private suspend fun readBytes(ref: AssetRef): LoadedAsset {
        return when (ref) {
            is RawAssetRef -> loadRawAsset(ref)
        }
    }

    protected abstract suspend fun loadRawAsset(rawRef: RawAssetRef): LoadedRawAsset

    protected open fun isHttpAsset(assetPath: String): Boolean =
        // maybe use something less naive here?
        assetPath.startsWith("http://", true) ||
                assetPath.startsWith("https://", true) ||
                assetPath.startsWith("data:", true)

    /**
     * Launches a new coroutine using the this vfs coroutine context. Use this to load assets asynchronously.
     */
    fun launch(block: suspend Vfs.() -> Unit) {
        (this as CoroutineScope).launch {
            block.invoke(this@Vfs)
        }
    }

    abstract fun store(key: String, data: ByteArray): Boolean
    abstract fun store(key: String, data: String): Boolean
    abstract fun load(key: String): ByteBuffer?
    abstract fun loadString(key: String): String?

    /**
     * Loads a raw file into a [ByteBuffer]
     * @param assetPath the path to the file
     * @return the raw byte buffer
     */
    suspend fun readBytes(assetPath: String): ByteBuffer {
        val ref = if (isHttpAsset(assetPath)) {
            RawAssetRef(assetPath, false)
        } else {
            RawAssetRef("$baseDir/$assetPath", true)
        }
        val awaitedAsset = AwaitedAsset(ref)
        awaitedAssetsChannel.send(awaitedAsset)
        val loaded = awaitedAsset.awaiting.await() as LoadedRawAsset
        loaded.data?.let {
            logger.debug { "Loaded ${assetPathToName(assetPath)} (${(it.capacity / 1024.0 / 1024.0).toString(2)} mb)" }
        }
        return loaded.data
            ?: throw FileNotFoundException(assetPath)
    }

    fun assetPathToName(assetPath: String): String {
        return if (assetPath.startsWith("data:", true)) {
            val idx = assetPath.indexOf(';')
            assetPath.substring(0 until idx)
        } else {
            assetPath
        }
    }

    operator fun get(path: String) = root[path]

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

sealed class LoadedAsset(val ref: AssetRef, val successful: Boolean)
class LoadedRawAsset(ref: AssetRef, val data: ByteBuffer?) : LoadedAsset(ref, data != null)

class FileNotFoundException(path: String) :
    Exception("File ($path) could not be found! Check to make sure it exists and is not corrupt.")

class UnsupportedFileTypeException(message: String) : Exception("Unsupported file: $message")