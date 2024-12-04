package com.littlekt.file.vfs

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
private val MimeType_byExtensions by lazy { LinkedHashMap<String, MimeType>() }

class MimeType(val mime: String, val exts: List<String>) {
    companion object {
        val APPLICATION_OCTET_STREAM = MimeType("application/octet-stream", listOf("bin"))
        val APPLICATION_JSON = MimeType("application/json", listOf("json"))
        val IMAGE_PNG = MimeType("image/png", listOf("png"))
        val IMAGE_JPEG = MimeType("image/jpeg", listOf("jpg", "jpeg"))
        val IMAGE_GIF = MimeType("image/gif", listOf("gif"))
        val TEXT_HTML = MimeType("text/html", listOf("htm", "html"))
        val TEXT_PLAIN = MimeType("text/plain", listOf("txt", "text"))
        val TEXT_CSS = MimeType("text/css", listOf("css"))
        val TEXT_JS = MimeType("application/javascript", listOf("js"))

        fun register(mimeType: MimeType) {
            mimeType.exts.forEach { ext -> MimeType_byExtensions[ext] = mimeType }
        }

        fun register(vararg mimeTypes: MimeType) {
            mimeTypes.forEach { mt -> register(mt) }
        }

        fun register(mime: String, vararg exsts: String) {
            register(MimeType(mime, exsts.map(String::lowercase)))
        }

        init {
            register(
                APPLICATION_OCTET_STREAM,
                APPLICATION_JSON,
                IMAGE_PNG,
                IMAGE_JPEG,
                IMAGE_GIF,
                TEXT_HTML,
                TEXT_PLAIN,
                TEXT_CSS,
                TEXT_JS,
            )
        }

        fun getByExtension(ext: String, default: MimeType = APPLICATION_OCTET_STREAM): MimeType =
            MimeType_byExtensions[ext.lowercase()] ?: default
    }
}
