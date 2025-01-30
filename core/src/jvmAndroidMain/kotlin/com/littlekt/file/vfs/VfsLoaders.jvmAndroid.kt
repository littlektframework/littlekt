package com.littlekt.file.vfs

import com.littlekt.graphics.LazyTexture

actual suspend fun VfsFile.readImageData(): LazyTexture.ImageData<*> =
    LazyTexture.ImageData(readPixmap())

internal actual suspend fun ByteArray.readImageData(mimeType: String?): LazyTexture.ImageData<*> =
    LazyTexture.ImageData(readPixmap())
