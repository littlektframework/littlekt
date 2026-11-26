package com.littlekt.file.vfs

import com.littlekt.LwjglContext
import com.littlekt.audio.AudioClip
import com.littlekt.audio.AudioStream
import com.littlekt.audio.OpenALAudioClip
import com.littlekt.audio.OpenALAudioStream
import com.littlekt.file.ByteBufferImpl
import com.littlekt.file.JvmByteSequenceStream
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.PixmapTexture
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.Device
import com.littlekt.log.Logger
import fr.delthas.javamp3.Sound
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.IntBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

private val logger = Logger("VfsLoaders")

/**
 * Decodes PNG or JPEG bytes, or any other image format stb_image can decode, into a [Pixmap] of
 * RGBA pixels.
 *
 * @return the decoded pixmap
 */
actual suspend fun ByteArray.readPixmap(): Pixmap = readPixmapInternal(this)

/**
 * Decodes PNG or JPEG bytes, or any other image format stb_image can decode, into a [Texture].
 *
 * @param device the device to create the texture with
 * @param options additional [TextureOptions] such as generating mips or passing along a
 *   [com.littlekt.graphics.webgpu.SamplerDescriptor].
 * @return the decoded texture
 */
actual suspend fun ByteArray.readTexture(device: Device, options: TextureOptions): Texture =
    readPixmapInternal(this).toTexture(device, options)

private fun Pixmap.toTexture(device: Device, options: TextureOptions): Texture =
    PixmapTexture(
        device,
        options.format,
        this,
        if (options.generateMipMaps) Texture.calculateNumMips(width, height) else 1,
        options.samplerDescriptor,
    )

/**
 * Loads an image from the path as a [Pixmap].
 *
 * @return the loaded texture
 */
actual suspend fun VfsFile.readPixmap(): Pixmap {
    val bytes = readBytes()
    return readPixmapInternal(bytes)
}

private fun readPixmapInternal(bytes: ByteArray): Pixmap {
    return MemoryStack.stackPush().use { stack ->
        val w: IntBuffer = stack.mallocInt(1)
        val h: IntBuffer = stack.mallocInt(1)
        val comp: IntBuffer = stack.mallocInt(1)
        val channels: IntBuffer = stack.mallocInt(1)
        val desiredChannels = 4
        // unfortunately we need to do an array copy since we don't have an underlying direct
        // buffer so we can't use ByteBuffer.wrap(bytes) :(
        val raw = ByteBuffer.allocateDirect(bytes.size).put(bytes).flip()
        if (!stbi_info_from_memory(raw, w, h, comp)) {
            logger.trace { "Failed to read image: ${stbi_failure_reason()}" }
        } else {
            logger.trace { "OK with reason: ${stbi_failure_reason()}" }
        }
        val rawResult = stbi_load_from_memory(raw, w, h, channels, desiredChannels)
        checkNotNull(rawResult) { "Failed to load image: ${stbi_failure_reason()}" }

        val result = ByteBufferImpl(rawResult.capacity()).apply { putByte(rawResult) }
        MemoryUtil.memFree(rawResult)
        Pixmap(w[0], h[0], result)
    }
}

actual suspend fun VfsFile.readTexture(options: TextureOptions): Texture =
    readPixmap().toTexture(vfs.context.graphics.device, options)

/**
 * Loads audio from the path as an [AudioClip].
 *
 * @return the loaded audio clip
 */
actual suspend fun VfsFile.readAudioClip(): AudioClip {
    val asset = read()
    // TODO refactor the sound handling to check the actual file headers
    val (source, channels, sampleRate) =
        if (pathInfo.extension == "mp3") {
            val decoder = kotlin.run { Sound(ByteArrayInputStream(asset.toArray())) }
            val source = decoder.readBytes().also { run { decoder.close() } }
            val channels = if (decoder.isStereo) 2 else 1
            Triple(source, channels, decoder.samplingFrequency.toFloat())
        } else {
            val pcm = run { AudioSystem.getAudioInputStream(ByteArrayInputStream(asset.toArray())).toPcm16() }
            val source = pcm.use { it.readAllBytes() }
            Triple(source, pcm.format.channels, pcm.format.sampleRate)
        }

    vfs.context as LwjglContext
    return OpenALAudioClip(vfs.context.audioContext, source, channels, sampleRate.toInt())
}

/**
 * Streams audio from the path as an [AudioStream].
 *
 * @return a new [AudioStream]
 */
actual suspend fun VfsFile.readAudioStream(): AudioStream {
    if (
        pathInfo.extension == "mp3"
    ) { // TODO refactor the sound handling to check the actual file headers
        return createAudioStreamMp3()
    }

    return createAudioStreamWav()
}

private suspend fun VfsFile.createAudioStreamMp3(): OpenALAudioStream {
    vfs.context as LwjglContext
    var decoder = run { Sound((readStream() as JvmByteSequenceStream).stream) }
    val channels = if (decoder.isStereo) 2 else 1
    val read: (ByteArray) -> Int = { decoder.read(it) }
    val reset: suspend () -> Unit = {
        run {
            decoder.close()
            decoder = Sound((readStream() as JvmByteSequenceStream).stream)
        }
    }

    val close: () -> Unit = { decoder.close() }

    return OpenALAudioStream(
        vfs.context.audioContext,
        read,
        reset,
        close,
        channels,
        decoder.samplingFrequency,
    )
}

private suspend fun VfsFile.createAudioStreamWav(): OpenALAudioStream {
    vfs.context as LwjglContext
    var clip = run {
        AudioSystem.getAudioInputStream((readStream() as JvmByteSequenceStream).stream).toPcm16()
    }
    val read: (ByteArray) -> Int = {
        val result = clip.read(it)
        result
    }
    val reset: suspend () -> Unit = {
        run {
            clip.close()
            clip = AudioSystem.getAudioInputStream((readStream() as JvmByteSequenceStream).stream).toPcm16()
        }
    }

    val close: () -> Unit = { clip.close() }

    return OpenALAudioStream(
        vfs.context.audioContext,
        read,
        reset,
        close,
        clip.format.channels,
        clip.format.sampleRate.toInt(),
    )
}

private fun AudioInputStream.toPcm16(): AudioInputStream {
    val source = format
    if (
        source.encoding == AudioFormat.Encoding.PCM_SIGNED &&
            source.sampleSizeInBits == 16 &&
            !source.isBigEndian
    ) {
        return this
    }
    val target =
        AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            source.sampleRate,
            16,
            source.channels,
            source.channels * 2,
            source.sampleRate,
            false,
        )
    return AudioSystem.getAudioInputStream(target, this)
}
