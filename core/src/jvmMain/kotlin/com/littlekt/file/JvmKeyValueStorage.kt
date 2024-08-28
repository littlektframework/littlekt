package com.littlekt.file

import com.littlekt.log.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @author Colton Daily
 * @date 8/23/2024
 */
class JvmKeyValueStorage(private val logger: Logger, storageBaseDir: String) : KeyValueStorage {
    private val storageDir = File(storageBaseDir)
    private val keyValueStore = mutableMapOf<String, String>()

    init {
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
        Runtime.getRuntime()
            .addShutdownHook(
                thread(false) {
                    val kvStore = KeyValueStore(keyValueStore.map { (k, v) -> KeyValueEntry(k, v) })
                    File(storageDir, KEY_VALUE_STORAGE_NAME).writeText(Json.encodeToString(kvStore))
                }
            )
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

    @Serializable private data class KeyValueEntry(val k: String, val v: String)

    @Serializable private data class KeyValueStore(val keyValues: List<KeyValueEntry>)
}
