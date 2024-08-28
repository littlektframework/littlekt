package com.littlekt.file

/**
 * @author Colton Daily
 * @date 8/28/2024
 */
interface KeyValueStorage {
    fun store(key: String, data: ByteArray): Boolean

    fun store(key: String, data: String): Boolean

    fun load(key: String): ByteBuffer?

    fun loadString(key: String): String?
}
