package com.lehaine.littlekt.io

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.PlatformApplication
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureData
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.graphics.gl.TextureFormat
import com.lehaine.littlekt.log.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import javax.imageio.ImageIO

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class JvmFileHandler(application: Application, logger: Logger, assetsBaseDir: String) :
    FileHandler(application, logger, assetsBaseDir) {

    private val imageIoLock = Any()

    override suspend fun loadRaw(rawRef: RawAssetRef): LoadedRawAsset {
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
                    FileInputStream(f).use { data = Uint8BufferImpl(it.readBytes()) }
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
            application as PlatformApplication
            application.runOnMainThread {
                it.prepare(application)
                deferred.complete(it)
            }

        }
        return deferred.await()
    }

    override suspend fun loadTexture(textureRef: TextureAssetRef): LoadedTextureAsset {
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
        TODO("Not yet implemented")
    }

}