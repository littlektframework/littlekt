package com.littlekt.file.vfs

import com.littlekt.file.Base64.encodeToBase64

actual fun toJsArray(array: Array<JsAny?>): JsArray<JsAny?> = array.toJsArray()
actual fun encodeByteArrayToBase64(array: ByteArray): String = array.encodeToBase64()