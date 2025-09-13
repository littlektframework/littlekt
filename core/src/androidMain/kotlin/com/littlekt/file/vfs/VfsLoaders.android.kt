package com.littlekt.file.vfs

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.littlekt.AndroidContext
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.littlekt.audio.AudioClip
import com.littlekt.audio.AudioStream
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.PixmapTexture
import com.littlekt.graphics.Texture
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal actual suspend fun ByteArray.readPixmap(): Pixmap {
    return readPixmapInternal(this)
}

private fun readPixmapInternal(bytes: ByteArray): Pixmap {
    val opts = android.graphics.BitmapFactory.Options().apply {
        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
    }
    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        ?: error("Failed to decode image bytes")

    val w = bmp.width
    val h = bmp.height

    val argb = IntArray(w * h)
    bmp.getPixels(argb, 0, w, 0, 0, w, h)

    val buf = com.littlekt.file.ByteBuffer(w * h * 4)

    for (i in 0 until argb.size) {
        val c = argb[i]
        val a = (c ushr 24) and 0xFF
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        val rgba = (r shl 24) or (g shl 16) or (b shl 8) or a
        buf.putInt(i * 4, rgba)
    }

    bmp.recycle()
    return Pixmap(w, h, buf)
}

actual suspend fun VfsFile.readPixmap(): Pixmap {
    val bytes = readBytes()
    return readPixmapInternal(bytes)
}

actual suspend fun VfsFile.readTexture(options: TextureOptions): Texture {
    val pixmap = readPixmap()
    return PixmapTexture(
        vfs.context.graphics.device,
        options.format,
        pixmap,
        if (options.generateMipMaps) Texture.calculateNumMips(pixmap.width, pixmap.height) else 1,
        options.samplerDescriptor,
    )
}

actual suspend fun VfsFile.readAudioClip(): AudioClip {
    val dataSource = resolveMediaDataSource()
    val context = (vfs.context as AndroidContext).androidContext
    return AndroidMediaAudioClip(dataSource, context)
}

actual suspend fun VfsFile.readAudioStream(): AudioStream {
    val dataSource = resolveMediaDataSource()
    val context = (vfs.context as AndroidContext).androidContext
    return AndroidMediaAudioStream(dataSource, context)
}

private class MediaDataSource(
    val uri: Uri? = null,
    val localFile: File? = null,
)

private suspend fun VfsFile.resolveMediaDataSource(): MediaDataSource {
    if (isHttpUrl() && !path.startsWith("data:", true)) {
        return MediaDataSource(uri = Uri.parse(path))
    }

    val bytes = readBytes()
    val ext = pathInfo.extension.takeIf { it.isNotBlank() } ?: "bin"
    val temp = withContext(Dispatchers.IO) {
        val f = File.createTempFile("littlekt_audio_", ".${ext}", (vfs.context as AndroidContext).androidContext.cacheDir)
        FileOutputStream(f).use { it.write(bytes) }
        f
    }
    return MediaDataSource(localFile = temp)
}


private class AndroidMediaAudioClip(
    private val dataSource: MediaDataSource,
    context: Context,
) : AudioClip {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private var durationMs: Long = 0L

    override var volume: Float
        get() = player.volume
        set(value) {
            player.volume = value.coerceIn(0f, 1f)
        }

    override val duration: Duration
        get() {
            val d = if (player.duration != C.TIME_UNSET) player.duration else durationMs
            return if (d > 0) d.milliseconds else Duration.ZERO
        }

    init {
        val uri = dataSource.uri ?: Uri.fromFile(requireNotNull(dataSource.localFile))
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                val d = player.duration
                if (d != C.TIME_UNSET && d > 0) {
                    durationMs = d
                }
            }
        })
        player.prepare()
    }

    override fun play(volume: Float, loop: Boolean) {
        this.volume = volume
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.seekTo(0)
        player.playWhenReady = true
    }

    override fun stop() {
        player.stop()
        player.seekTo(0)
    }

    override fun resume() {
        player.playWhenReady = true
    }

    override fun pause() {
        player.playWhenReady = false
    }

    override fun release() {
        player.release()
        dataSource.localFile?.delete()
    }
}

private class AndroidMediaAudioStream(
    private val dataSource: MediaDataSource,
    context: Context,
) : AudioStream {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    override var volume: Float
        get() = player.volume
        set(value) {
            player.volume = value.coerceIn(0f, 1f)
        }

    override var looping: Boolean
        get() = player.repeatMode == Player.REPEAT_MODE_ONE
        set(value) {
            player.repeatMode = if (value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }

    override val playing: Boolean
        get() = player.isPlaying

    init {
        val uri = dataSource.uri ?: Uri.fromFile(requireNotNull(dataSource.localFile))
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    override suspend fun play(volume: Float, loop: Boolean) {
        this.volume = volume
        this.looping = loop
        player.playWhenReady = true
    }

    override fun stop() {
        player.stop()
        player.seekTo(0)
    }

    override fun resume() {
        player.playWhenReady = true
    }

    override fun pause() {
        player.playWhenReady = false
    }

    override fun release() {
        player.release()
        dataSource.localFile?.delete()
    }
}