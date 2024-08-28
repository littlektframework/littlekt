package com.littlekt.file

/**
 * A simple key-value storage system.
 *
 * @author Colton Daily
 * @date 8/28/2024
 */
interface KeyValueStorage {
    /**
     * Stores the following [data] by the specified [key].
     *
     * @return `true` if stored successfully.
     */
    fun store(key: String, data: ByteArray): Boolean

    /**
     * Stores the following [data] by the specified [key].
     *
     * @return `true` if stored successfully.
     */
    fun store(key: String, data: String): Boolean

    /**
     * Loads data, if it exists, by the specified [key].
     *
     * @return stored data, if it exists.
     */
    fun load(key: String): ByteBuffer?

    /**
     * Loads data, if it exists, by the specified [key].
     *
     * @return stored data, if it exists.
     */
    fun loadString(key: String): String?
}
