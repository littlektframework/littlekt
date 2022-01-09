package com.lehaine.littlekt.file.vfs

import com.lehaine.littlekt.LwjglContext
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.audio.OpenALAudioClip
import com.lehaine.littlekt.file.ImageUtils
import com.lehaine.littlekt.file.createByteBuffer
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.graphics.gl.TextureFormat
import fr.delthas.javamp3.Sound
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO
import javax.sound.sampled.AudioSystem

/**
 * @author Colton Daily
 * @date 12/20/2021
 */

private val imageIoLock = Any()

/**
 * Loads an image from the path as a [Texture]. This will call [Texture.prepare] before returning!
 * @return the loaded texture
 */
actual suspend fun VfsFile.readTexture(minFilter: TexMinFilter, magFilter: TexMagFilter, mipmaps: Boolean): Texture {
    val data = PixmapTextureData(readPixmap(), mipmaps)

    return Texture(data).also {
        it.minFilter = minFilter
        it.magFilter = magFilter
        vfs.context as LwjglContext
        it.prepare(vfs.context)
    }
}

actual suspend fun VfsFile.readPixmap(): Pixmap {
    val bytes = readBytes()
    // ImageIO.read is not thread safe!
    val img = synchronized(imageIoLock) {
        runCatching {
            ImageIO.read(ByteArrayInputStream(bytes))
        }.getOrThrow()
    }
    return Pixmap(
        img.width,
        img.height,
        ImageUtils.bufferedImageToBuffer(img, TextureFormat.RGBA, img.width, img.height)
    )
}

/**
 * Loads audio from the path as an [AudioClip].
 * @return the loaded audio clip
 */
actual suspend fun VfsFile.readAudioClip(): AudioClip {
    val asset = read()
    // TODO refactor the sound handling to check the actual file headers
    val (source, channels, sampleRate) = if (pathInfo.extension == "mp3") {
        runCatching {
            val decoder = Sound(ByteArrayInputStream(asset.toArray()))
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