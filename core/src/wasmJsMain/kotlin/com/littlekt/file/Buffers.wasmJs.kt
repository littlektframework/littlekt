package com.littlekt.file

actual fun byteArrayOf(endOffset: Int, startOffset: Int): ByteArray = ByteArray(endOffset - startOffset)