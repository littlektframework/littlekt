package com.littlekt.file

import com.littlekt.Context
import com.littlekt.file.vfs.VfsFile
import com.littlekt.file.vfs.lightCombine
import com.littlekt.file.vfs.normalize
import com.littlekt.file.vfs.pathInfo
import com.littlekt.log.Logger
import com.littlekt.util.toString
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.Json

/**
 * A virtual file system that handles loading and streaming data and files.
 *
 * @author Colton Daily
 * @date 11/6/2021
 */
abstract class Vfs(val context: Context, val logger: Logger, var baseDir: String) : CoroutineScope {
    /** The root [VfsFile] that this [Vfs] starts from. */
    val root
        get() = VfsFile(this, baseDir)

    protected open val absolutePath: String
        get() = ""

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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

    private fun loadWorker(
        assetRefs: ReceiveChannel<AssetRef>,
        loadedAssets: SendChannel<LoadedAsset>
    ) = launch {
        for (ref in assetRefs) {
            loadedAssets.send(readBytes(ref))
        }
    }

    /** Cancels this vfs job. */
    open fun close() {
        job.cancel()
    }

    /**
     * Get the absolute path of the queried [path] based off the Vfs path.
     *
     * @param path the path to retrieve the absolute path for
     */
    open fun getAbsolutePath(path: String) =
        absolutePath.pathInfo.lightCombine(path.pathInfo).fullPath

    private suspend fun readBytes(ref: AssetRef): LoadedAsset {
        return when (ref) {
            is RawAssetRef -> loadRawAsset(ref)
            is SequenceAssetRef -> loadSequenceStreamAsset(ref)
        }
    }

    protected abstract suspend fun loadRawAsset(rawRef: RawAssetRef): LoadedRawAsset

    protected abstract suspend fun loadSequenceStreamAsset(
        sequenceRef: SequenceAssetRef
    ): SequenceStreamCreatedAsset

    protected open fun isHttpAsset(assetPath: String): Boolean =
        // maybe use something less naive here?
        assetPath.startsWith("http://", true) ||
            assetPath.startsWith("https://", true) ||
            assetPath.startsWith("data:", true)

    /**
     * Launches a new coroutine using the this vfs coroutine context. Use this to load assets
     * asynchronously.
     */
    fun launch(block: suspend Vfs.() -> Unit) {
        (this as CoroutineScope).launch { block.invoke(this@Vfs) }
    }

    /**
     * Store array of bytes in the storage directory based on the [key].
     *
     * @param key the key of the data
     * @param data the data to store
     */
    abstract fun store(key: String, data: ByteArray): Boolean

    /**
     * Store a string in the storage directory based on the [key].
     *
     * @param key the key of the data
     * @param data the string to store
     */
    abstract fun store(key: String, data: String): Boolean

    /**
     * Load an array of bytes from the storage directory based on the [key].
     *
     * @param key the key of the data to load
     */
    abstract fun load(key: String): ByteBuffer?

    /**
     * Load a string from the storage directory based on the [key].
     *
     * @param key the key of the string to load
     */
    abstract fun loadString(key: String): String?

    /**
     * Loads a raw file into a [ByteBuffer]
     *
     * @param assetPath the path to the file
     * @return the raw byte buffer
     */
    suspend fun readBytes(assetPath: String): ByteBuffer {
        val ref =
            if (isHttpAsset(assetPath)) {
                RawAssetRef(assetPath, false)
            } else {
                RawAssetRef("$baseDir/$assetPath".pathInfo.normalize(), true)
            }
        val awaitedAsset = AwaitedAsset(ref)
        awaitedAssetsChannel.send(awaitedAsset)
        val loaded = awaitedAsset.awaiting.await() as LoadedRawAsset
        loaded.data?.let {
            logger.info {
                "Loaded ${assetPathToName(assetPath)} (${(it.capacity / 1024.0 / 1024.0).toString(2)} mb)"
            }
        }
        return loaded.data ?: throw FileNotFoundException(assetPath)
    }

    /**
     * Opens a stream to a file into a [ByteSequenceStream].
     *
     * @param assetPath the path to file
     * @return the byte input stream
     */
    suspend fun readStream(assetPath: String): ByteSequenceStream {
        val ref =
            if (isHttpAsset(assetPath)) {
                SequenceAssetRef(assetPath)
            } else {
                SequenceAssetRef("$baseDir/$assetPath")
            }
        val awaitedAsset = AwaitedAsset(ref)
        awaitedAssetsChannel.send(awaitedAsset)
        val loaded = awaitedAsset.awaiting.await() as SequenceStreamCreatedAsset
        loaded.sequence?.let { logger.info { "Opened stream to ${assetPathToName(assetPath)}." } }
        return loaded.sequence ?: throw FileNotFoundException(assetPath)
    }

    private fun assetPathToName(assetPath: String): String {
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

data class SequenceAssetRef(val url: String) : AssetRef()

sealed class LoadedAsset(val ref: AssetRef, val successful: Boolean)

class LoadedRawAsset(ref: AssetRef, val data: ByteBuffer?) : LoadedAsset(ref, data != null)

class SequenceStreamCreatedAsset(ref: AssetRef, val sequence: ByteSequenceStream?) :
    LoadedAsset(ref, sequence != null)

class FileNotFoundException(path: String) :
    Exception("File ($path) could not be found! Check to make sure it exists and is not corrupt.")

class UnsupportedFileTypeException(message: String) : Exception("Unsupported file: $message")
