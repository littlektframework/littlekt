package com.lehaine.littlekt.file.vfs

import android.content.res.AssetFileDescriptor
import android.graphics.BitmapFactory
import android.media.SoundPool
import android.util.Log
import com.lehaine.littlekt.AndroidContext
import com.lehaine.littlekt.async.onRenderingThread
import com.lehaine.littlekt.audio.*
import com.lehaine.littlekt.file.AndroidVfs
import com.lehaine.littlekt.file.ByteBufferImpl
import com.lehaine.littlekt.file.createByteBuffer
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.util.internal.lock
import com.lehaine.littlekt.util.toString
import java.io.File
import java.io.FileOutputStream

/**
 * Loads an image from the path as a [Texture]. This will call [Texture.prepare] before returning!
 * @return the loaded texture
 */
actual suspend fun VfsFile.readTexture(minFilter: TexMinFilter, magFilter: TexMagFilter, mipmaps: Boolean): Texture {
    val data = PixmapTextureData(readPixmap(), mipmaps)

    return Texture(data).also {
        it.minFilter = minFilter
        it.magFilter = magFilter
        onRenderingThread {
            it.prepare(vfs.context)
        }
    }
}

/**
 * Reads Base64 encoded ByteArray for embedded images.
 */
internal actual suspend fun ByteArray.readPixmap(): Pixmap {
    return readPixmap(this)
}

actual suspend fun VfsFile.readPixmap(): Pixmap {
    val bytes = readBytes()
    return readPixmap(bytes)
}

private fun readPixmap(bytes: ByteArray): Pixmap {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val buffer = createByteBuffer(bitmap.width * bitmap.height * 4) as ByteBufferImpl
    bitmap.copyPixelsToBuffer(buffer.buffer)
    return Pixmap(bitmap.width, bitmap.height, buffer)
}

private val VfsFile.audioContext: AndroidAudioContext get() = (vfs.context as AndroidContext).audioContext
private val VfsFile.soundPool: SoundPool get() = audioContext.soundPool

/**
 * Loads audio from the path as an [AudioClip].
 * @return the loaded audio clip
 */
actual suspend fun VfsFile.readAudioClip(): AudioClip {
    val descriptor = createAssetFileDescriptor()

    vfs.logger.info { "Loaded $baseName (${(descriptor.length / 1024.0 / 1024.0).toString(2)} mb)" }
    return AndroidAudioClip(
        soundPool,
        soundPool.load(descriptor, 1),
    ).also {
        descriptor.close()
    }
}

/**
 * Streams audio from the path as an [AudioStream].
 * @return a new [AudioStream]
 */
actual suspend fun VfsFile.readAudioStream(): AudioStream {
    val descriptor = createAssetFileDescriptor()
    val player = audioContext.createMediaPlayer().apply {
        setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
    }
    vfs.logger.info { "Loaded $baseName (${(descriptor.length / 1024.0 / 1024.0).toString(2)} mb)" }
    descriptor.close()
    player.prepare()
    return AndroidAudioStream(audioContext, player).also { lock(audioContext.streams) { audioContext.streams += it } }
}

actual suspend fun VfsFile.writePixmap(pixmap: Pixmap) {
    // TODO write to an actual png vs bmp - currently bmps write bottom to top while pixmaps pixels are top to bottom
    val buffer = pixmap.pixels
    val width = pixmap.width
    val height = pixmap.height
    val bmpBuffer = createByteBuffer(54 + buffer.capacity)
    bmpBuffer.run {
        putByte('B'.code.toByte())
        putByte('M'.code.toByte())
        putUInt(54 + width * height * 4) // size
        putUShort(0) // res1
        putUShort(0) // res2
        putUInt(54) // offset
        putUInt(40) // biSize
        putUInt(width)
        putUInt(height)
        putUShort(1.toShort()) // planes
        putUShort((8 * 4).toShort()) // bitCount
        putUInt(0) // compression
        putUInt(width * height * 4) // image size bytes
        putUInt(0) // x pixels per meter
        putUInt(0) // y pixels per meter
        putUInt(0) // colors used
        putUInt(0) // important colors
        putByte(buffer.toArray(), 0, buffer.capacity)
    }
    runCatching {
        FileOutputStream(File(path)).use { it.write(bmpBuffer.toArray()) }
    }.getOrThrow()
}

private fun VfsFile.createAssetFileDescriptor(): AssetFileDescriptor =
    (vfs as AndroidVfs).assets
        .openFd(
            path.removePrefix(
                "./"
            )
        )