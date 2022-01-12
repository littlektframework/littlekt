package com.lehaine.littlekt.file

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.util.*
import kotlin.concurrent.thread

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class JvmVfs(context: Context, logger: Logger, storageBaseDir: String, assetsBaseDir: String) :
    Vfs(context, logger, assetsBaseDir) {

    private val storageDir = File(storageBaseDir)
    private val keyValueStore = mutableMapOf<String, String>()

    init {
        HttpCache.initCache(File(".httpCache"))
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            logger.error { "Failed to create storage directory at $storageBaseDir" }
        }

        val persistentKvStorage = File(storageDir, KEY_VALUE_STORAGE_NAME)
        if (persistentKvStorage.canRead()) {
            try {
                val kvStore = Json.decodeFromString<KeyValueStore>(persistentKvStorage.readText())
                kvStore.keyValues.forEach { (k, v) -> keyValueStore[k] = v }
            } catch (e: Exception) {
                logger.error { "Failed loading key value store: $e" }
                e.printStackTrace()
            }
        }
        Runtime.getRuntime().addShutdownHook(thread(false) {
            val kvStore = KeyValueStore(keyValueStore.map { (k, v) -> KeyValueEntry(k, v) })
            File(storageDir, KEY_VALUE_STORAGE_NAME).writeText(Json.encodeToString(kvStore))
        })
    }

    override suspend fun loadRawAsset(rawRef: RawAssetRef): LoadedRawAsset {
        return if (rawRef.isLocal) {
            loadLocalRaw(rawRef)
        } else {
            loadHttpRaw(rawRef)
        }
    }

    override suspend fun loadSequenceStreamAsset(sequenceRef: SequenceAssetRef): SequenceStreamCreatedAsset {
        var sequence: SequenceStream? = null

        withContext(Dispatchers.IO) {
            try {
                openLocalStream(sequenceRef.url).let {
                    sequence = JvmSequenceStream(it)
                }
            } catch (e: Exception) {
                logger.error { "Failed loading creating buffered sequence of ${sequenceRef.url}: $e" }
            }
        }
        return SequenceStreamCreatedAsset(sequenceRef, sequence)
    }

    private suspend fun loadLocalRaw(localRawRef: RawAssetRef): LoadedRawAsset {
        var data: ByteBufferImpl? = null
        withContext(Dispatchers.IO) {
            try {
                openLocalStream(localRawRef.url).use { data = ByteBufferImpl(it.readBytes()) }
            } catch (e: Exception) {
                logger.error { "Failed loading asset ${localRawRef.url}: $e" }
            }
        }
        return LoadedRawAsset(localRawRef, data)
    }

    private suspend fun loadHttpRaw(httpRawRef: RawAssetRef): LoadedRawAsset {
        var data: ByteBufferImpl? = null

        if (httpRawRef.url.startsWith("data:", true)) {
            data = decodeDataUrl(httpRawRef.url)
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val f = HttpCache.loadHttpResource(httpRawRef.url)
                        ?: throw IOException("Failed downloading ${httpRawRef.url}")
                    runCatching {
                        FileInputStream(f).use { data = ByteBufferImpl(it.readBytes()) }
                    }
                } catch (e: Exception) {
                    logger.error { "Failed loading asset ${httpRawRef.url}: $e" }
                }
            }
        }
        return LoadedRawAsset(httpRawRef, data)
    }

    private fun decodeDataUrl(dataUrl: String): ByteBufferImpl {
        val dataIdx = dataUrl.indexOf(";base64,") + 8
        return ByteBufferImpl(Base64.getDecoder().decode(dataUrl.substring(dataIdx)))
    }

    private fun openLocalStream(assetPath: String): InputStream {
        var inStream = ClassLoader.getSystemResourceAsStream(assetPath)
        if (inStream == null) {
            // if asset wasn't found in resources try to load it from file system
            inStream = FileInputStream(assetPath)
        }
        return inStream
    }

    override fun store(key: String, data: ByteArray): Boolean {
        return try {
            val file = File(storageDir, key)
            FileOutputStream(file).use { it.write(data) }
            logger.debug { "Wrote to ${file.absolutePath}" }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    override fun store(key: String, data: String): Boolean {
        keyValueStore[key] = data
        return true
    }

    override fun load(key: String): ByteBuffer? {
        val file = File(storageDir, key)
        if (!file.canRead()) {
            return null
        }
        return try {
            ByteBufferImpl(file.readBytes())
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun loadString(key: String): String? {
        return keyValueStore[key]
    }

    companion object {
        private const val KEY_VALUE_STORAGE_NAME = ".keyValueStorage.json"
    }

    @Serializable
    data class KeyValueEntry(val k: String, val v: String)

    @Serializable
    data class KeyValueStore(val keyValues: List<KeyValueEntry>)
}