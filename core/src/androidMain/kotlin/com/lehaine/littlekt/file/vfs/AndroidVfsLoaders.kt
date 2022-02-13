package com.lehaine.littlekt.file.vfs

import android.graphics.BitmapFactory
import com.lehaine.littlekt.async.onRenderingThread
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.audio.AudioStream
import com.lehaine.littlekt.file.ByteBufferImpl
import com.lehaine.littlekt.file.createByteBuffer
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
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

/**
 * Loads audio from the path as an [AudioClip].
 * @return the loaded audio clip
 */
actual suspend fun VfsFile.readAudioClip(): AudioClip {
    TODO("Implement Me!")
}

/**
 * Streams audio from the path as an [AudioStream].
 * @return a new [AudioStream]
 */
actual suspend fun VfsFile.readAudioStream(): AudioStream {
    TODO("Implement Me!")
}

private suspend fun VfsFile.createAudioStreamMp3() {
    TODO("Implement Me!")
}

private suspend fun VfsFile.createAudioStreamWav() {
    TODO("Implement Me!")
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