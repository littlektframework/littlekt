package com.lehaine.littlekt.file.vfs

import com.lehaine.littlekt.LwjglContext
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.audio.OpenALAudioClip
import com.lehaine.littlekt.file.ImageUtils
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.graphics.gl.TextureFormat
import fr.delthas.javamp3.Sound
import java.io.ByteArrayInputStream
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
actual suspend fun VfsFile.readTexture(): Texture {
    val bytes = readBytes()
    // ImageIO.read is not thread safe!
    val img = synchronized(imageIoLock) {
        runCatching {
            ImageIO.read(ByteArrayInputStream(bytes))
        }.getOrThrow()
    }
    val pixmap = Pixmap(
        img.width,
        img.height,
        ImageUtils.bufferedImageToBuffer(img, TextureFormat.RGBA, img.width, img.height)
    )
    val data = PixmapTextureData(pixmap, true)

    return Texture(data).also {
        vfs.context as LwjglContext
        vfs.context.runOnMainThread {
            it.prepare(vfs.context)
        }
    }
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