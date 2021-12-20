package com.lehaine.littlekt.file

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.LwjglContext
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.audio.OpenALAudioClip
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureData
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.graphics.gl.TextureFormat
import com.lehaine.littlekt.log.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.util.*
import javax.imageio.ImageIO
import javax.sound.sampled.AudioSystem
import kotlin.concurrent.thread
import fr.delthas.javamp3.Sound as MP3Decoder

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class JvmFileHandler(context: Context, logger: Logger, storageBaseDir: String, assetsBaseDir: String) :
    FileHandler(context, logger, assetsBaseDir) {

    private val storageDir = File(storageBaseDir)
    private val imageIoLock = Any()

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

    private suspend fun loadLocalRaw(localRawRef: RawAssetRef): LoadedRawAsset {
        var data: Uint8BufferImpl? = null
        withContext(Dispatchers.IO) {
            try {
                openLocalStream(localRawRef.url).use { data = Uint8BufferImpl(it.readBytes()) }
            } catch (e: Exception) {
                logger.error { "Failed loading asset ${localRawRef.url}: $e" }
            }
        }
        return LoadedRawAsset(localRawRef, data)
    }

    private suspend fun loadHttpRaw(httpRawRef: RawAssetRef): LoadedRawAsset {
        var data: Uint8BufferImpl? = null

        if (httpRawRef.url.startsWith("data:", true)) {
            data = decodeDataUrl(httpRawRef.url)
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val f = HttpCache.loadHttpResource(httpRawRef.url)
                        ?: throw IOException("Failed downloading ${httpRawRef.url}")
                    runCatching {
                        FileInputStream(f).use { data = Uint8BufferImpl(it.readBytes()) }
                    }
                } catch (e: Exception) {
                    logger.error { "Failed loading asset ${httpRawRef.url}: $e" }
                }
            }
        }
        return LoadedRawAsset(httpRawRef, data)
    }

    private fun decodeDataUrl(dataUrl: String): Uint8BufferImpl {
        val dataIdx = dataUrl.indexOf(";base64,") + 8
        return Uint8BufferImpl(Base64.getDecoder().decode(dataUrl.substring(dataIdx)))
    }

    internal fun openLocalStream(assetPath: String): InputStream {
        var inStream = ClassLoader.getSystemResourceAsStream(assetPath)
        if (inStream == null) {
            // if asset wasn't found in resources try to load it from file system
            inStream = FileInputStream(assetPath)
        }
        return inStream
    }

    override suspend fun loadTexture(assetPath: String): Texture {
        val data = loadTextureData(assetPath)
        val deferred = CompletableDeferred<Texture>(job)
        Texture(data).also {
            context as LwjglContext
            context.runOnMainThread {
                it.prepare(context)
                deferred.complete(it)
            }
        }
        return deferred.await()
    }

    override suspend fun loadTextureAsset(textureRef: TextureAssetRef): LoadedTextureAsset {
        var data: TextureData? = null
        withContext(Dispatchers.IO) {
            try {
                data = if (textureRef.isLocal) {
                    loadLocalTexture(textureRef)
                } else {
                    loadHttpTexture(textureRef)
                }
            } catch (e: Exception) {
                logger.error { "Failed loading texture ${textureRef.url}: $e" }
            }
        }
        return LoadedTextureAsset(textureRef, data)

    }


    private fun loadLocalTexture(localTextureRef: TextureAssetRef): TextureData {
        return openLocalStream(localTextureRef.url).use {
            // ImageIO.read is not thread safe!
            val img = synchronized(imageIoLock) {
                ImageIO.read(it)
            }
            val pixmap = Pixmap(
                img.width,
                img.height,
                ImageUtils.bufferedImageToBuffer(img, TextureFormat.RGBA, img.width, img.height)
            )
            PixmapTextureData(pixmap, true)
        }
    }

    private fun loadHttpTexture(httpTextureRef: TextureAssetRef): TextureData {
        val f = HttpCache.loadHttpResource(httpTextureRef.url)!!
        return FileInputStream(f).use {
            // ImageIO.read is not thread safe!
            val img = synchronized(imageIoLock) {
                ImageIO.read(it)
            }
            val pixmap = Pixmap(
                img.width,
                img.height,
                ImageUtils.bufferedImageToBuffer(img, TextureFormat.RGBA, img.width, img.height)
            )
            PixmapTextureData(pixmap, true)
        }
    }

    override suspend fun loadAudioClip(assetPath: String): AudioClip {
        val asset = loadAsset(assetPath)
        // TODO refactor the sound handling to check the actual file headers
        val (source, channels, sampleRate) = if (assetPath.endsWith(".mp3")) {
            runCatching {
                val decoder = MP3Decoder(ByteArrayInputStream(asset.toArray()))
                val source = decoder.readBytes().also { decoder.close() }
                val channels = if (decoder.isStereo) 2 else 1
                Triple(source, channels, decoder.samplingFrequency.toFloat())
            }.getOrThrow()
        } else {
            runCatching {
                val source = asset.toArray()
                val clip = AudioSystem.getAudioFileFormat(ByteArrayInputStream(asset.toArray()))
                Triple(source, clip.format.channels, clip.format.sampleRate)
            }.getOrThrow()
        }

        return OpenALAudioClip(source, channels, sampleRate.toInt())
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

    override fun load(key: String): Uint8Buffer? {
        val file = File(storageDir, key)
        if (!file.canRead()) {
            return null
        }
        return try {
            Uint8BufferImpl(file.readBytes())
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