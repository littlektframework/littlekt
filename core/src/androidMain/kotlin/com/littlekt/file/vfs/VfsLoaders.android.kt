package com.littlekt.file.vfs

import com.littlekt.audio.AudioClip
import com.littlekt.audio.AudioStream
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.TextureFormat

/** Reads Base64 encoded ByteArray for embedded images. */
internal actual suspend fun ByteArray.readPixmap(): Pixmap {
    TODO("Not yet implemented")
}

/**
 * Loads an image from the path as a [Pixmap].
 *
 * @return the loaded texture
 */
actual suspend fun VfsFile.readPixmap(): Pixmap {
    TODO("Not yet implemented")
}

actual suspend fun VfsFile.readTexture(preferredFormat: TextureFormat): Texture {
    TODO("Not yet implemented")
}

/**
 * Loads audio from the path as an [AudioClip].
 *
 * @return the loaded audio clip
 */
actual suspend fun VfsFile.readAudioClip(): AudioClip {
    TODO("Not yet implemented")
}

/**
 * Streams audio from the path as an [AudioStream].
 *
 * @return a new [AudioStream]
 */
actual suspend fun VfsFile.readAudioStream(): AudioStream {
    TODO("Not yet implemented")
}
