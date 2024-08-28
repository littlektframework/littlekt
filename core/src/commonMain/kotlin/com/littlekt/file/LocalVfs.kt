package com.littlekt.file

import com.littlekt.Context
import com.littlekt.file.vfs.normalize
import com.littlekt.file.vfs.pathInfo
import com.littlekt.log.Logger
import com.littlekt.util.toString
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.Json

/**
 * A [Vfs] that handles reading from files on the local file system.
 *
 * @author Colton Daily
 * @date 8/26/2024
 */
abstract class LocalVfs(context: Context, logger: Logger, baseDir: String) :
    Vfs(context, logger, baseDir) {

    override val json = Json {
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

    /**
     * Loads a raw file into a [ByteBuffer]
     *
     * @param assetPath the path to the file
     * @return the raw byte buffer
     */
    override suspend fun readBytes(assetPath: String): ByteBuffer {
        check(!isHttpAsset(assetPath)) {
            "A http url is not a valid asset path. Vfs only loads local files!"
        }
        val ref =
            if (baseDir.endsWith("/")) {
                RawAssetRef("$baseDir$assetPath".pathInfo.normalize(), true)
            } else if (baseDir.isNotBlank()) {
                RawAssetRef("$baseDir/$assetPath".pathInfo.normalize(), true)
            } else {
                RawAssetRef(assetPath.pathInfo.normalize(), true)
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
    override suspend fun readStream(assetPath: String): ByteSequenceStream {
        check(!isHttpAsset(assetPath)) {
            "A http url is not a valid asset path. Vfs only loads local files!"
        }
        val ref =
            if (baseDir.endsWith("/")) {
                SequenceAssetRef("$baseDir$assetPath")
            } else if (baseDir.isNotBlank()) {
                SequenceAssetRef("$baseDir/$assetPath")
            } else {
                SequenceAssetRef(assetPath)
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

    protected inner class AwaitedAsset(
        val ref: AssetRef,
        val awaiting: CompletableDeferred<LoadedAsset> = CompletableDeferred(job)
    )

    companion object {
        const val NUM_LOAD_WORKERS = 8
    }
}
